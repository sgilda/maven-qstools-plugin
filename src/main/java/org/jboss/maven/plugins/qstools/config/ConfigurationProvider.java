/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.maven.plugins.qstools.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.jboss.maven.plugins.qstools.Constants;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = ConfigurationProvider.class)
public class ConfigurationProvider {

    @Requirement
    private Context context;

    private Log log;

    private MavenSession mavenSession;

    private Map<String, Rules> configRules = new HashMap<String, Rules>();

    private URL configFileURL;

    private void configure() throws ContextException {
        configFileURL = (URL) context.get(Constants.CONFIG_FILE_CONTEXT);
        log = (Log) context.get(Constants.LOG_CONTEXT);
        mavenSession = (MavenSession) context.get(Constants.MAVEN_SESSION_CONTEXT);
    }

    /**
     * Return the {@link Rules} which should be used for a Quickstarts/Project
     * 
     * @param groupId - the groupId of the Quickstart/Project
     * 
     * @return
     */
    public Rules getQuickstartsRules(String groupId) {
        if (configFileURL == null) {
            try {
                configure();
            } catch (ContextException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
        Rules rules = configRules.get(groupId);
        if (rules == null) {
            rules = initializeConfig(groupId);
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private Rules initializeConfig(String groupId) {
        InputStream inputStream = null;
        try {
            // Retrieve inputStream (local cache or remote)
            inputStream = getFileInputStream(configFileURL);
            Yaml yaml = new Yaml();
            Map<String, Object> configFile = (Map<String, Object>) yaml.load(inputStream);
            Map<String, Object> quickstartsGroupIds = (Map<String, Object>) configFile.get("quickstarts");
            List<Object> configs = (List<Object>) quickstartsGroupIds.get(groupId);
            if (configs == null) {
                Map<String, Object> defaultRulesSection = (Map<String, Object>) ((List<Object>) configFile.get("rules")).get(0);
                List<Object> defaultRules = new LinkedList<Object>();
                defaultRules.add(defaultRulesSection);
                configs = defaultRules;
            }
            Rules rules = new Rules(configs);
            configRules.put(groupId, rules);
            return configRules.get(groupId);
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Something bad happened when closing the inputstream", e);
                }
            }
        }
    }

    /**
     * Return a FileInputStream from a local file.
     * 
     * The local file is cached based on {@link Constants#CACHE_EXPIRES_SECONDS}
     * 
     * If the caches expires, them the file is downloaded again
     * 
     * @return
     * @throws FileNotFoundException
     * 
     */
    public InputStream getFileInputStream(URL url) throws FileNotFoundException {
        InputStream repoStream = getCachedRepoStream(false, url);
        // if cache expired
        if (repoStream == null) {
            log.debug("Local cache file " + getLocalCacheFile(url) + " doesn't exist or cache has been expired");
            try {
                log.debug("Retrieving File from Remote repository " + url);
                repoStream = retrieveFileFromRemoteRepository(url);
                setCachedRepoStream(repoStream, url);
                log.debug("Forcing the use of local cache after download file without error from " + url);
                repoStream = getCachedRepoStream(true, url);
            } catch (Exception e) {
                log.warn("It was not possible to contact the repository at " + url + " . Cause " + e.getMessage());
                log.warn("Falling back to cache!");
                repoStream = getCachedRepoStream(true, url);
            }
        }
        return repoStream;
    }

    private InputStream getCachedRepoStream(final boolean force, URL url) throws FileNotFoundException {
        final String logmessage = "Local file %1s %2s used! Reason: Force:[%3b] - LastModification: %4d/%5d";
        File localCacheFile = getLocalCacheFile(url);
        if (localCacheFile.exists()) {
            long cachedvalidity = 1000 * Constants.CACHE_EXPIRES_SECONDS;
            long lastModified = localCacheFile.lastModified();
            long timeSinceLastModification = System.currentTimeMillis() - lastModified;
            // if online, consider the cache valid until it expires
            if (force || timeSinceLastModification <= cachedvalidity) {
                log.debug(String.format(logmessage, localCacheFile, "was", force, timeSinceLastModification,
                    cachedvalidity));
                return new FileInputStream(localCacheFile);
            }
            log.debug(String.format(logmessage, localCacheFile, "was not", force, timeSinceLastModification,
                cachedvalidity));
        }
        return null;
    }

    private void setCachedRepoStream(final InputStream stream, URL url) throws IOException {
        File localCacheFile = getLocalCacheFile(url);
        log.debug("Content stored at " + localCacheFile);
        if (!localCacheFile.exists()) {
            localCacheFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(localCacheFile);

        int i = 0;
        while ((i = stream.read()) != -1) {
            fos.write(i);
        }
        fos.close();
    }

    private File getLocalCacheFile(URL url) {
        // Remove no word character from the repo url
        String repo = url.toString().replaceAll("[^a-zA-Z_0-9]", "");
        return new File(System.getProperty("java.io.tmpdir"), repo);
    }

    private InputStream retrieveFileFromRemoteRepository(URL url) throws Exception {
        if (url.getProtocol().startsWith("http")) {
            HttpGet httpGet = new HttpGet(url.toURI());
            DefaultHttpClient client = new DefaultHttpClient();
            configureProxy(client);
            HttpResponse httpResponse = client.execute(httpGet);
            switch (httpResponse.getStatusLine().getStatusCode()) {
                case 200:
                    log.debug("Connected to repository! Getting " + url);
                    break;

                case 404:
                    log.error("Failed! (File not found: " + url + ")");
                    return null;

                default:
                    log.error("Failed! (server returned status code: "
                        + httpResponse.getStatusLine().getStatusCode());
                    return null;
            }
            log.info("Downloading " + url);
            return httpResponse.getEntity().getContent();
        } else if (url.getProtocol().startsWith("file")) {
            return new FileInputStream(new File(url.toURI()));
        }
        return null;
    }

    private void configureProxy(DefaultHttpClient client) {
        Proxy proxyConfig = null;
        if (mavenSession.getSettings().getProxies().size() > 0) {
            proxyConfig = mavenSession.getSettings().getProxies().get(0);
        }
        if (proxyConfig != null) {
            HttpHost proxy = new HttpHost(proxyConfig.getHost(), proxyConfig.getPort());
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            String proxyUsername = proxyConfig.getUsername();
            if (proxyUsername != null && !proxyUsername.isEmpty()) {
                String proxyPassword = proxyConfig.getPassword();
                AuthScope authScope = new AuthScope(proxyConfig.getHost(), proxyConfig.getPort());
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
                client.getCredentialsProvider().setCredentials(authScope, credentials);
            }
        }
    }
}
