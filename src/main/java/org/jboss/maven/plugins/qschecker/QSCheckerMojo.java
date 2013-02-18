package org.jboss.maven.plugins.qschecker;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;

/**
 * Execute all quickstart checks
 * 
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, requiresProject=true)
public class QSCheckerMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private PlexusContainer container;

    public void execute() throws MojoExecutionException {
       
        List<QSChecker> checkers;
        try {
            checkers = container.lookupList(QSChecker.class);            
            for (QSChecker qc : checkers) {
                qc.setup(project, getLog());
                qc.check();
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
