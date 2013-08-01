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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.jdf.stacks.client.StacksClient;
import org.jboss.jdf.stacks.model.Bom;
import org.jboss.jdf.stacks.model.Stacks;
import org.jboss.maven.plugins.qstools.maven.MavenDependency;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Update all BOMs to use the recommended versions. Note that the update only will be made from previous version to newer
 * recommended versions. It doesn't downgrade the versions;
 * 
 * @author Rafael Benevides
 * 
 */
@Mojo(name = "updateBoms", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true, aggregator = true)
public class BomUpdaterMojo extends AbstractMojo {

    @Parameter(property = "reactorProjects", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    /**
     * Overwrite the stacks file
     */
    @Parameter(property = "qstools.stacks.url")
    private URL stacksUrl;

    @Component
    private DependencyProvider dependencyProvider;

    private XPath xPath = XPathFactory.newInstance().newXPath();

    private StacksClient stacksClient;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            stacksClient = new StacksClient();
            if (stacksUrl != null) {
                stacksClient.getActualConfiguration().setUrl(stacksUrl);
            }
            getLog().info("Using the following Stacks YML file: " + stacksClient.getActualConfiguration().getUrl());
            getLog().warn("Running this plugin CAN MODIFY your pom.xml files. Make sure to have your changes commited before running this plugin");
            getLog().info("Do you want to continue[yes/no]");
            String answer = new Scanner(System.in).nextLine();
            if (answer.equalsIgnoreCase("yes")) {
                for (MavenProject project : reactorProjects) {
                    processProject(project);
                }
            } else {
                getLog().info("Aborted");
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * @param project
     * @throws Exception
     */
    private void processProject(MavenProject project) throws Exception {
        Stacks stacks = stacksClient.getStacks();
        getLog().debug("Processing " + project.getArtifactId());
        Document doc = PositionalXMLReader.readXML(new FileInputStream(project.getFile()));
        NodeList dependencies = (NodeList) xPath.evaluate("/project/dependencyManagement/dependencies/dependency", doc, XPathConstants.NODESET);
        // Iterate over all Declared Managed Dependencies
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = dependencyProvider.getDependencyFromNode(project, dependency);
            // use stacks to find if the project is using a jdf bom
            Bom bomUsed = null;
            for (Bom bom : stacks.getAvailableBoms()) {
                if (bom.getGroupId().equals(mavenDependency.getGroupId()) && bom.getArtifactId().equals(mavenDependency.getArtifactId())) {
                    bomUsed = bom;
                }
            }
            if (bomUsed != null && // It used a Managed JDF Bom
                !(mavenDependency.getInterpoledVersion().equals(bomUsed.getRecommendedVersion()))) {

                getLog().debug(String.format("Project [%s] - Dependency [%s:%s:%s] isnt'using the recommended version [%s]",
                    project.getArtifactId(),
                    mavenDependency.getGroupId(),
                    mavenDependency.getArtifactId(),
                    mavenDependency.getInterpoledVersion(),
                    bomUsed.getRecommendedVersion()));
                DefaultArtifactVersion recommendedVersion = new DefaultArtifactVersion(bomUsed.getRecommendedVersion());
                DefaultArtifactVersion usedVersion = new DefaultArtifactVersion(mavenDependency.getInterpoledVersion());
                if (recommendedVersion.compareTo(usedVersion) > 0) {
                    updateVersion(project.getFile(), mavenDependency.getInterpoledVersion(), bomUsed.getRecommendedVersion());
                } else {
                    getLog().warn(
                        String.format("Project [%s] will not be updated because it uses a newer BOM [%s:%s:%s] version than the recommended [%s]",
                            project.getArtifactId(),
                            mavenDependency.getGroupId(),
                            mavenDependency.getArtifactId(),
                            mavenDependency.getInterpoledVersion(),
                            bomUsed.getRecommendedVersion()));
                }
            }
        }
    }

    /**
     * @param file
     * @param interpoledVersion
     * @param recommendedVersion
     * @throws IOException
     */
    private void updateVersion(File file, String usedVersion, String recommendedVersion) throws Exception {
        getLog().info(String.format("Updating from %s to %s in file %s", usedVersion, recommendedVersion, file));
        String content = IOUtils.toString(new FileInputStream(file));
        content = content.replaceAll(usedVersion, recommendedVersion);
        FileOutputStream fos = new FileOutputStream(file);
        try {
            IOUtils.write(content, new FileOutputStream(file));
        } finally {
            fos.close();
        }
    }

}
