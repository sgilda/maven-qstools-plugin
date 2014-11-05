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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.mozilla.universalchardet.UniversalDetector;

/**
 * 
 * @author rafaelbenevides
 */
@Component(role = QSChecker.class, hint = "JavaEncondingChecker")
public class JavaEncondingChecker implements QSChecker {

    private int violationsQtd;

    private UniversalDetector encodingDetector = new UniversalDetector(null);

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Override
    public String getCheckerDescription() {
        return "Verifies if java source character encoding is UTF-8";
    }

    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession,
        List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        try {
            if (configurationProvider.getQuickstartsRules(project.getGroupId()).isCheckerIgnored(this.getClass())) {
                String msg = "Skiping %s for %s:%s";
                log.warn(String.format(msg, this.getClass().getSimpleName(), project.getGroupId(), project.getArtifactId()));
            } else {
                // get all files to process
                List<File> sourceFiles = FileUtils.getFiles(project.getBasedir(), "**/*.java", "");
                for (File source : sourceFiles) {
                    FileInputStream fis = null;
                    try {
                        // Read file content as byte array (no encoding)
                        fis = new FileInputStream(source);
                        byte[] buf = new byte[4096];
                        int nread;
                        while ((nread = fis.read(buf)) > 0 && !encodingDetector.isDone()) {
                            encodingDetector.handleData(buf, 0, nread);
                        }
                        encodingDetector.dataEnd();

                        // report if not utf-8
                        String encoding = encodingDetector.getDetectedCharset();
                        if (encoding != null && !encoding.equalsIgnoreCase("UTF-8")) {
                            // Get relative path based on maven work dir
                            String rootDirectory = (mavenSession.getExecutionRootDirectory() + File.separator).replace("\\",
                                "\\\\");
                            String fileAsString = source.getAbsolutePath().replace(rootDirectory, "");
                            if (results.get(fileAsString) == null) {
                                results.put(fileAsString, new ArrayList<Violation>());
                            }
                            results.get(fileAsString).add(new Violation(getClass(),
                                0,
                                "This file contains a non UTF-8 characters. It was detected as " + encoding));
                            violationsQtd++;
                        }
                    } finally {
                        encodingDetector.reset();
                        if (fis != null) {
                            fis.close();
                        }
                    }
                }
                if (violationsQtd > 0) {
                    log.info("There are " + violationsQtd + " checkers violations");
                }
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
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

}