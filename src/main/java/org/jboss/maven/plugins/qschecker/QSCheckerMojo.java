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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * Goal which touches a timestamp file.
 * 
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class QSCheckerMojo extends AbstractMojo {

    @Component
    private MavenProject project;
    
    @Component
    private QSChecker checker;

    public void execute() throws MojoExecutionException {
        System.out.println(project);
        checker.check();
    }
}
