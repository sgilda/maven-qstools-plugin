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
package org.jboss.maven.plugins.qstools;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;

/**
 * This Mojo is used to check if all Dependencies declared in a </dependencyManagement> section of a BOM is resolvable.
 * 
 * @author Rafael Benevides
 * 
 */
@Mojo(name = "fix", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true, aggregator = true)
public class FixViolationsMojo extends AbstractMojo {

    @Component
    private PlexusContainer container;

    @Component
    private MavenSession mavenSession;

    @Component
    private MavenProject mavenProject;

    @Component
    private BuildPluginManager pluginManager;

    /**
     * Overwrite the config file
     */
    @Parameter(property = "qstools.configFileURL",
        defaultValue = "https://raw.github.com/jboss-developer/maven-qstools-plugin/master/config/qstools_config.yaml")
    private URL configFileURL;

    @Parameter(property = "reactorProjects", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            configurePlugin();
            getLog().warn("Running this plugin CAN MODIFY your files. Make sure to have your changes commited before running this plugin");
            getLog().warn("Do you want to continue[yes/NO]");
            String answer = new Scanner(System.in).nextLine();
            if (answer.equalsIgnoreCase("yes")) {
                List<QSFixer> fixersFound = container.lookupList(QSFixer.class);
                // sort the checkers
                List<QSFixer> fixers = new ArrayList<QSFixer>(fixersFound);
                Collections.sort(fixers, new Comparator<QSFixer>() {

                    @Override
                    public int compare(QSFixer o1, QSFixer o2) {
                        int value = Integer.valueOf(o1.order()).compareTo(o2.order());
                        if (value == 0){
                            return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
                        }else{
                            return value;
                        }
                    }
                });
                for (QSFixer fixer : fixers) {
                    getLog().info("Running Fixer: " + fixer.getClass().getSimpleName());
                    fixer.fix(mavenProject, mavenSession, reactorProjects, getLog());
                }
                getLog().info(" ***** All projects were processed! Total Processed: " + reactorProjects.size() +
                    "\nRun [mvn clean compile] to get sure that everything is working" +
                    "\nRun [git diff] to see the changes made." +
                    "\n");
            } else {
                getLog().info("Aborted");
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Check if a Custom Stacks URL was informed and configure Stacks client
     * 
     */
    private void configurePlugin() {
        getLog().info("Using the following QSTools config file: " + configFileURL);
        container.getContext().put(Constants.CONFIG_FILE_CONTEXT, configFileURL);

        container.getContext().put(Constants.LOG_CONTEXT, getLog());
        container.getContext().put(Constants.MAVEN_SESSION_CONTEXT, mavenSession);
        container.getContext().put(Constants.IGNORED_QUICKSTARTS_CONTEXT, Utils.readIgnoredFile());
        container.getContext().put(Constants.PLUGIN_MANAGER, pluginManager);
    }

   
}
