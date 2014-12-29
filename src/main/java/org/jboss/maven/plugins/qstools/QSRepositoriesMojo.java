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

import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.PlexusContainer;
import org.jboss.maven.plugins.qstools.common.ProjectUtil;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.fixers.PomElementOrderFixer;
import org.jboss.maven.plugins.qstools.fixers.QSFixer;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This Mojo is used to check if all Dependencies declared in a {@code<dependencyManagement/>} section of a BOM is resolvable.
 * 
 * @author Rafael Benevides
 * 
 */
@Mojo(name = "repositories", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true, threadSafe = true, aggregator = true)
public class QSRepositoriesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    /**
     * Overwrite the config file
     */
    @Parameter(property = "qstools.configFileURL",
        defaultValue = "https://raw.github.com/jboss-developer/maven-qstools-plugin/master/config/qstools_config.yaml")
    private URL configFileURL;

    @Parameter(property = "reactorProjects", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Component
    private ConfigurationProvider configurationProvider;

    @Component
    private PlexusContainer container;

    @Component
    private ProjectUtil projectUtil;

    @Component
    private BuildPluginManager pluginManager;

    private XPath xPath = XPathFactory.newInstance().newXPath();

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog()
                .warn(
                    "Running this plugin CAN MODIFY your files. Make sure to have your changes commited before running this plugin");
            getLog().warn("Do you want to continue[yes/NO]");
            Console console = System.console();
            String answer = console.readLine();
            if (answer.equalsIgnoreCase("yes")) {
                configurePlugin();
                Rules rules = configurationProvider.getQuickstartsRules(mavenProject.getGroupId());
                Map<String, String> repositories = rules.getMavenApprovedRepositories();
                Set<String> repositoriesSelected = new HashSet<String>();
                List<String> repoIds = new ArrayList<String>(repositories.keySet());
                while (!answer.matches("(Q|q)|(R|r)")) {
                    getLog().warn("Please select the Maven repositories that you want to define. Selected repositories are denoted by a leading '*'.");
                    int x = 0;
                    StringBuilder sb = new StringBuilder("\n");
                    for (String repository : repoIds) {
                        x++;
                        String part1 =
                            "  " + (repositoriesSelected.contains(repository) ? "* " : "  ") + x + " - " + repository;
                        String part1padded = StringUtils.rightPad(part1, 45);
                        String part2 = " - " + repositories.get(repository) + "\n";
                        sb.append(part1padded + part2);
                    }
                    sb.append("    A - Select All                            - Add all repositories to the list to be run\n");
                    sb.append("    N - Select None                           - Remove all currently selected repositories from the list to be run\n");
                    sb.append("\n");
                    sb.append("    R - Run the plugin to add the selected repositories (denoted by an *)\n");
                    sb.append("    Q - Quit without running any fixers\n");
                    sb.append("\n");
                    sb.append("Enter the number of the repository to select/deselect it. Enter 'A' to select all. Enter 'N' to deselect all. ");
                    sb.append("\n\nWhen ready, enter 'R' to add the selected repositories or 'Q' to quit.");
                    getLog().info(sb);
                    answer = console.readLine();

                    // if selected a fixer (number from 1 to 99)
                    if (answer.matches("[1-9][0-9]*")) {
                        String selectedRepository = repoIds.get((Integer.parseInt(answer) - 1));
                        if (repositoriesSelected.contains(selectedRepository)) {
                            repositoriesSelected.remove(selectedRepository);
                        } else {
                            repositoriesSelected.add(selectedRepository);
                        }
                    } else if (answer.equalsIgnoreCase("A")) {
                        repositoriesSelected.addAll(repositories.keySet());
                    } else if (answer.equalsIgnoreCase("N")) {
                        repositoriesSelected.clear();
                    }
                }
                // Execute mojo
                if (answer.equalsIgnoreCase("R")) {
                    removePreviousRepositories();
                    setupRepositories(repositories, repositoriesSelected, rules);
                    getLog().info(
                        " ***** All projects were processed! Total Processed: " + reactorProjects.size()
                            + "\nRun [mvn clean compile] to get sure that everything is working"
                            + "\nRun [git diff] to see the changes made." + "\n");
                }
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

    private void removePreviousRepositories() throws Exception {
        getLog().info("Removing previous repositories definition...");
        for (MavenProject project : reactorProjects) {
            if (!projectUtil.isSubProjec(project)) {
                Document doc = PositionalXMLReader.readXML(new FileInputStream(project.getFile()));
                removeRepositoryDefinition(project, doc, "repositories");
                removeRepositoryDefinition(project, doc, "pluginRepositories");
                XMLUtil.writeXML(doc, project.getFile());
            }
        }
    }

    private void removeRepositoryDefinition(MavenProject project, Document doc, String repository) throws XPathExpressionException {
        Node repositoriesNode = (Node) xPath.evaluate("//project/" + repository, doc, XPathConstants.NODE);
        if (repositoriesNode != null) {
            // Get comment over the element
            Node commentNode = null;
            if (repositoriesNode.getPreviousSibling() != null
                && repositoriesNode.getPreviousSibling().getPreviousSibling() != null
                && repositoriesNode.getPreviousSibling().getPreviousSibling().getNodeType() == Node.COMMENT_NODE) {
                commentNode = repositoriesNode.getPreviousSibling().getPreviousSibling();
            }
            // If the element had a comment, remove it too.
            if (commentNode != null) {
                XMLUtil.removePreviousWhiteSpace(commentNode);
                commentNode.getParentNode().removeChild(commentNode);
            }
            XMLUtil.removePreviousWhiteSpace(repositoriesNode);
            repositoriesNode.getParentNode().removeChild(repositoriesNode);

        }
    }

    private void setupRepositories(Map<String, String> repositories, Set<String> repositoriesSelected, Rules rules) throws Exception {
        getLog().info("Adding selected repositories");
        for (MavenProject project : reactorProjects) {
            if (!projectUtil.isSubProjec(project)) {
                Document doc = PositionalXMLReader.readXML(new FileInputStream(project.getFile()));
                createRepositoryDefinition(project, doc, repositories, repositoriesSelected, rules, "repositories", "repository", true);
                createRepositoryDefinition(project, doc, repositories, repositoriesSelected, rules, "pluginRepositories", "pluginRepository", false);
                XMLUtil.writeXML(doc, project.getFile());
                getLog().debug("Sorting " + project.getFile() + "elements order");
                // Put the element at the right order
                PomElementOrderFixer pomElementOrderFixer = container.lookup(PomElementOrderFixer.class, QSFixer.ROLE, "PomElementOrderFixer");
                pomElementOrderFixer.fixProject(project, doc);
            }
        }

    }

    private void createRepositoryDefinition(MavenProject project, Document doc, Map<String, String> repositories, Set<String> repositoriesSelected, Rules rules,
        String repositoriesType, String repositoryType,
        boolean createComment) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
        String complement =
            "<complement><releases>\n                <enabled>RELEASE</enabled>\n            </releases><snapshots>\n                <enabled>SNAPSHOT</enabled>\n            </snapshots></complement>";
        Node repositoriesElement = (Node) xPath.evaluate("//project/" + repositoriesType, doc, XPathConstants.NODE);
        // create repositories element if not exists
        if (repositoriesElement == null && repositoriesSelected.size() > 0) {
            Comment comment = doc.createComment(rules.getMavenRepositoryComment());
            repositoriesElement = doc.createElement(repositoriesType);
            Element projectElement = (Element) xPath.evaluate("/project", doc, XPathConstants.NODE);
            if (createComment) {
                projectElement.appendChild(comment);
                projectElement.appendChild(doc.createTextNode("\n    "));
            }
            projectElement.appendChild(repositoriesElement);
        }
        for (String repository : repositoriesSelected) {
            getLog().debug("Adding " + repository + " to " + project.getFile());
            Element repositoryElement = doc.createElement(repositoryType);
            Element idElement = doc.createElement("id");
            idElement.setTextContent(repository);
            repositoryElement.appendChild(idElement);
            Element urlElement = doc.createElement("url");
            String[] repo = repositories.get(repository).split("[|]");
            urlElement.setTextContent(repo[0]);
            repositoryElement.appendChild(urlElement);
            String parsedComplement = complement.replace("RELEASE", repo[1]).replace("SNAPSHOT", repo[2]);
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(parsedComplement)));
            repositoryElement.appendChild(doc.importNode(d.getFirstChild().getFirstChild(), true));
            repositoryElement.appendChild(doc.importNode(d.getFirstChild().getLastChild(), true));
            repositoriesElement.appendChild(repositoryElement);
        }

    }
}
