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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.maven.MavenDependency;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    @Component
    private DependencyProvider dependencyProvider;

    @Component
    private ConfigurationProvider configurationProvider;

    @Component
    private MavenSession mavenSession;

    @Component
    private PlexusContainer container;

    /**
     * Overwrite the config file
     */
    @Parameter(property = "qstools.configFileURL",
        defaultValue = "https://raw.github.com/jboss-developer/maven-qstools-plugin/master/config/qstools_config.yaml")
    private URL configFileURL;

    private XPath xPath = XPathFactory.newInstance().newXPath();

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            configure();
            getLog().warn("Running this plugin CAN MODIFY your pom.xml files. Make sure to have your changes commited before running this plugin");
            getLog().warn("Do you want to continue[yes/no]");
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

    private void configure() {
        getLog().info("Using the following QSTools config file: " + configFileURL);
        container.getContext().put(Constants.CONFIG_FILE_CONTEXT, configFileURL);

        container.getContext().put(Constants.LOG_CONTEXT, getLog());
        container.getContext().put(Constants.MAVEN_SESSION_CONTEXT, mavenSession);
    }

    /**
     * @param project
     * @throws Exception
     */
    private void processProject(MavenProject project) throws Exception {
        Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
        getLog().debug("Processing " + project.getArtifactId());
        Document doc = PositionalXMLReader.readXML(new FileInputStream(project.getFile()));
        NodeList dependencies = (NodeList) xPath.evaluate("/project/dependencyManagement/dependencies/dependency", doc, XPathConstants.NODESET);
        replaceBOMsIfNeeded(project, dependencies, rules);
        updateBomsVersionIfNeeded(project, dependencies, rules, doc);
        write(doc, new FileOutputStream(project.getFile()));
    }

    private void updateBomsVersionIfNeeded(MavenProject project, NodeList dependencies, Rules rules, Document doc) throws InterpolationException, XPathExpressionException {
        // Iterate over all Declared Managed Dependencies - Needs to update BOM version?
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = dependencyProvider.getDependencyFromNode(project, dependency);
            String version = mavenDependency.getInterpoledVersion();
            String expectedBomVersion = (String) rules.getExpectedBomVersion().get(mavenDependency.getGroupId());
            // If not using expected Bom Version
            if (expectedBomVersion != null && !expectedBomVersion.equals(version)) {
                String declaredVersion = mavenDependency.getDeclaredVersion().replace("${", "").replace("}", "");
                // There's a declared property ?
                if (project.getProperties().get(declaredVersion) != null) { // Properties.contains() didn't work
                    // Alter ir
                    Node propertyNode = (Node) xPath.evaluate("/project/properties/" + declaredVersion, doc, XPathConstants.NODE);
                    if (propertyNode != null) { // It can be null for inherited property
                        propertyNode.setTextContent(expectedBomVersion);
                    }
                } else {
                    // Create the property if it doesn't exist
                    Node propertiesNode = (Node) xPath.evaluate("/project/properties", doc, XPathConstants.NODE);
                    Comment comment = doc.createComment("Automatically declared BOM property");
                    Element propertyNode = doc.createElement(declaredVersion);
                    propertyNode.setTextContent(expectedBomVersion);
                    propertiesNode.appendChild(doc.createTextNode("\n        ")); // LF + 8 spaces
                    propertiesNode.appendChild(comment);
                    propertiesNode.appendChild(doc.createTextNode("\n        ")); // LF + 8 spaces
                    propertiesNode.appendChild(propertyNode);
                }
            }
        }
    }

    private void replaceBOMsIfNeeded(MavenProject project, NodeList dependencies, Rules rules) throws InterpolationException {
        Properties bomsMigration = rules.getProjectBomsMigration();
        // Iterate over all Declared Managed Dependencies - Needs BOM replacement?
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = dependencyProvider.getDependencyFromNode(project, dependency);
            String oldBomGA = mavenDependency.getGroupId() + "|" + mavenDependency.getArtifactId();
            String newBomGAV = bomsMigration.getProperty(oldBomGA);

            if (newBomGAV != null) {
                String[] newBomGavSplited = newBomGAV.split("[|]");
                updateBomNode(dependency, newBomGavSplited[0], newBomGavSplited[1], newBomGavSplited[2]);
            }
        }
    }

    private void updateBomNode(Node dependencyNode, String groupId, String artifactId, String version) {
        NodeList childrenNodes = dependencyNode.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            Node node = childrenNodes.item(i);
            if (node.getNodeName().equals("groupId")) {
                node.setTextContent(groupId);
            } else if (node.getNodeName().equals("artifactId")) {
                node.setTextContent(artifactId);
            } else if (node.getNodeName().equals("version")) {
                node.setTextContent("${" + version + "}");
            }
        }
    }

    public void write(Document doc, OutputStream out) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // NOI18N
        t.setOutputProperty(OutputKeys.INDENT, "yes"); // NOI18N
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // NOI18N
        Source source = new DOMSource(doc);
        Result result = new StreamResult(out);
        t.transform(source, result);
    }

}
