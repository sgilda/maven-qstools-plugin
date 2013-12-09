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
package org.jboss.maven.plugins.qstools.fixers;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.checkers.TabSpaceChecker;
import org.w3c.dom.Document;

import com.google.common.io.Files;

/**
 * Fixer for {@link TabSpaceChecker} on XML Files
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "XMLTabFixer")
public class XMLTabFixer extends AbstractBaseFixerAdapter {

    @Override
    public String getFixerDescription() {
        return "Replace [TABS] by [spaces] on XMLs files";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        List<File> xmlFiles = FileUtils.getFiles(project.getBasedir(), "**/*.xml", "");
        for (File xmlSource : xmlFiles) {
            getLog().debug("Fixing tab on " + xmlSource);
            String source = Files.toString(xmlSource, Charset.forName("UTF-8"));
            String replaced = source.replace("\t", "    ");
            Files.write(replaced, xmlSource, Charset.forName("UTF-8"));
        }

    }

}
