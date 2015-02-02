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
package org.jboss.maven.plugins.qstools.checkers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSToolsException;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Resources;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * @author rafaelbenevides
 */
@Component(role = QSChecker.class, hint = "ValidXMLSchemaChecker")
public class ValidXMLSchemaChecker implements QSChecker {

    private int violationsQtd;

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Requirement
    private Resources resources;

    private String checkerMessage;

    private Log log;

    @Override
    public String getCheckerDescription() {
        return "Verifies if XML files are using a valid according to XML Schema or DTD";
    }

    private void addViolation(String fileAsString, int line, String message, Map<String, List<Violation>> results) {
        violationsQtd++;
        if (results.get(fileAsString) == null) {
            results.put(fileAsString, new ArrayList<Violation>());
        }
        results.get(fileAsString).add(new Violation(ValidXMLSchemaChecker.class, line, message));
    }

    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession,
        List<MavenProject> reactorProjects, Log log) throws QSToolsException {
        log.info("--> This Checker can take several minutes to run");
        this.log = log;
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
        try {
            if (rules.isCheckerIgnored(this.getClass())) {
                checkerMessage = "This checker is ignored for this groupId in config file.";
            } else {
                // get all xml to process but excludes hidden files and /target and /bin folders
                List<File> xmlFiles = FileUtils.getFiles(project.getBasedir(), "**/*.xml", rules.getExcludes());
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema();
                for (File xml : xmlFiles) {
                    // Get relative path based on maven work dir
                    String rootDirectory = (mavenSession.getExecutionRootDirectory() + File.separator).replace("\\", "\\\\");
                    String fileAsString = xml.getAbsolutePath().replace(rootDirectory, "");
                    Validator validator = schema.newValidator();
                    validator.setResourceResolver(new URLBasedResourceResolver(xml));
                    validator.setErrorHandler(new XMLErrorHandler(fileAsString, results));

                    log.info("Validating " + fileAsString);
                    try {
                        validator.validate(new StreamSource(new BufferedInputStream(new FileInputStream(xml))));
                    } catch (SAXException e) {
                        // validator.validate can throw a SAXException coming from the ErrorHandler
                        addViolation(fileAsString, 0, e.getMessage(), results);
                    }
                }
                if (getCheckerMessage() != null) {
                    log.info("--> Checker Message: " + getCheckerMessage());
                }
                if (violationsQtd > 0) {
                    log.info("There are " + violationsQtd + " checkers violations");
                }
            }
        } catch (Exception e) {
            throw new QSToolsException(e);
        }
        return results;

    }

    @Override
    public int getViolatonsQtd() {
        return violationsQtd;
    }

    @Override
    public void resetViolationsQtd() {
        violationsQtd = 0;
    }

    @Override
    public String getCheckerMessage() {
        return checkerMessage;
    }

    private class XMLErrorHandler implements ErrorHandler {

        private Map<String, List<Violation>> results;
        private String fileAsString;

        public XMLErrorHandler(String fileAsString, Map<String, List<Violation>> results) {
            this.results = results;
            this.fileAsString = fileAsString;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            addViolation(fileAsString, exception.getLineNumber(), exception.getMessage(), results);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            addViolation(fileAsString, exception.getLineNumber(), exception.getMessage(), results);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            addViolation(fileAsString, exception.getLineNumber(), exception.getMessage(), results);
        }

    }

    private class URLBasedResourceResolver implements LSResourceResolver {

        private File xml;

        public URLBasedResourceResolver(File xml) {
            this.xml = xml;
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI,
            String publicId, String systemId, String baseURI) {
            String msg = String.format("Resolve: type=%s, ns=%s, publicId=%s, systemId=%s, baseUri=%s.",
                type, namespaceURI, publicId, systemId, baseURI);
            log.debug(msg);
            MyLSInput input = new MyLSInput();
            input.setPublicId(publicId);
            input.setSystemId(systemId);
            try {
                if (type.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                    if (namespaceURI == null && systemId != null) {
                        FileInputStream fis = new FileInputStream(new File(xml.getParent(), systemId));
                        input.setByteStream(fis);
                        input.setSystemId(systemId);
                    } else if (namespaceURI != null) {
                        URI uri = new URI(baseURI == null ? "" : baseURI);
                        URL url = uri.resolve(systemId == null ? "" : systemId).toURL();
                        if (url.getProtocol().equals("http")) {
                            InputStream is = resources.getFileInputStream(url);
                            input.setBaseURI(baseURI);
                            input.setByteStream(is);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // It's ok for XMLs without systemId and BaseURI
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return input;
        }

        private class MyLSInput implements LSInput {

            private Reader characterStream;

            private InputStream inputStream;

            private String stringData;

            private String systemId;

            private String publicId;

            private String baseURI;

            private String encoding;

            private boolean certified;

            @Override
            public Reader getCharacterStream() {
                return characterStream;
            }

            @Override
            public void setCharacterStream(Reader characterStream) {
                this.characterStream = characterStream;
            }

            @Override
            public InputStream getByteStream() {
                return inputStream;
            }

            @Override
            public void setByteStream(InputStream byteStream) {
                this.inputStream = byteStream;
            }

            @Override
            public String getStringData() {
                return stringData;
            }

            @Override
            public void setStringData(String stringData) {
                this.stringData = stringData;
            }

            @Override
            public String getSystemId() {
                return systemId;
            }

            @Override
            public void setSystemId(String systemId) {
                this.systemId = systemId;
            }

            @Override
            public String getPublicId() {
                return publicId;
            }

            @Override
            public void setPublicId(String publicId) {
                this.publicId = publicId;

            }

            @Override
            public String getBaseURI() {
                return baseURI;
            }

            @Override
            public void setBaseURI(String baseURI) {
                this.baseURI = baseURI;

            }

            @Override
            public String getEncoding() {
                return encoding;
            }

            @Override
            public void setEncoding(String encoding) {
                this.encoding = encoding;
            }

            @Override
            public boolean getCertifiedText() {
                return certified;
            }

            @Override
            public void setCertifiedText(boolean certifiedText) {
                this.certified = certifiedText;
            }

        }
    }

}