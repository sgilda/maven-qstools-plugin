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
package org.jboss.maven.plugins.qschecker.checkers;

import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.jboss.maven.plugins.qschecker.Violation;
import org.w3c.dom.Document;

/**
 * @author Rafael Benevides
 *
 */
public class PomElementOrderChecker extends AbstractPomChecker {
    
    private static enum POM_ELEMENTS {
        parent, modules, properties, dependencyManagement, dependencies, build, profiles
    }


    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject, org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        // TODO Auto-generated method stub

    }

}
