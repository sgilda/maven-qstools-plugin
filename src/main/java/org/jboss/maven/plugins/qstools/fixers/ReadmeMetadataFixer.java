/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.maven.plugins.qstools.fixers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.common.ReadmeUtil;
import org.w3c.dom.Document;

import com.google.common.io.Files;

/**
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "ReadmeMetadataFixer")
public class ReadmeMetadataFixer extends AbstractBaseFixerAdapter {

    @Requirement
    private ReadmeUtil readmeUtil;

    private String regexPattern;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSFixer#getFixerDescription()
     */
    @Override
    public String getFixerDescription() {
        return "Adds two empty spaces at the end of Readme Metadatas";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.AbstractProjectWalker#fixProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document)
     */
    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        File readme = new File(project.getBasedir(), "README.md");
        regexPattern = readmeUtil.setupRegexPattern(project.getGroupId(), null);
        if (readme.exists()) {
            fixReadmeFile(project.getGroupId(), readme);
        }

    }

    /**
     * @param groupId
     * @param readme
     * @throws IOException
     */
    private void fixReadmeFile(String groupId, File readme) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(readme));
        try {
            Pattern p = Pattern.compile(regexPattern);
            StringBuilder sb = new StringBuilder();
            boolean readmeModified = false;
            while (br.ready()) {
                String line = br.readLine();
                Matcher m = p.matcher(line);
                if (m.find()) { // Only get metadata lines
                    if (!line.matches("\\w.*\\s\\s")) { // if line doesn't have two spaces
                        line = line.replace(line.trim(), line + "  "); // add two spaces only on a trimmed line
                        readmeModified = true;
                    }
                }
                sb.append(line + "\n");
            }
            if (readmeModified) {
                Files.write(sb.toString(), readme, Charset.forName("UTF-8"));
                getLog().info("Saving changes to " + readme);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

}
