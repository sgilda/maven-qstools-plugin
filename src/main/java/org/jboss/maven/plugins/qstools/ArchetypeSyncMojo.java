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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * If this plugin is used on an Archetype, it will sync the archetype-resources to its original project.
 * 
 * @author Rafael Benevides
 * 
 */
@Mojo(name = "archetypeSync", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true, aggregator = false)
public class ArchetypeSyncMojo extends AbstractMojo {

    private XPath xPath = XPathFactory.newInstance().newXPath();

    private static final String[] TEXT_EXTENSIONS = { "java", "css", "html", "xhtml", "md", "xml", "properties", "sql" };

    /**
     * Git repository that holds the maven project
     */
    @Parameter(property = "qstools.projectGitRepo", required = true, readonly = true)
    private String projectGitRepo;

    /**
     * Branch/Tag of the git repository that holds the origin project
     */
    @Parameter(property = "qstools.projectGitRepo.branch", defaultValue = "master")
    private String branch;

    /**
     * Relative path to project that originates the Archetype
     */
    @Parameter(property = "qstools.projectPath", required = true, readonly = true)
    private String projectPath;

    /**
     * Root package of the origin project
     */
    @Parameter(property = "qstools.rootPackage", required = true, readonly = true)
    private String rootPackage;

    /**
     * If a project is multi module, this flag should be enabled. This will use ${rootArtifactId} instead of ${artifactId}
     */
    @Parameter(property = "qstools.multiModuleProject", defaultValue = "false", readonly = true)
    private boolean multiModuleProject;

    /**
     * Extra string values that should be replaces by ${artifactId}, __artifactId__
     */
    @Parameter(property = "qstools.extraReplaceValues", readonly = true)
    private String[] extraReplaceValues;

    @Parameter(property = "project.build.directory")
    private String outputPath;

    @Parameter(property = "basedir")
    private String baseDir;

    private File exampleProjectPath;

    private boolean ignoreMode = false;

    private String originalGroupId = null;

    private String originalArtifactId = null;

    private String originalVersion = null;

    private String originalBomVersion = null;

    private String artifactExpression;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            exampleProjectPath = new File(outputPath + File.separator + "git", projectPath);
            artifactExpression = multiModuleProject ? "rootArtifactId" : "artifactId";
            cloneOriginProject();
            generateArchetype();
            getLog().info("Archetype synched with " + projectPath + " from " + branch + " branch. You can check what changed running git diff.");
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    /**
     * Generate the archetype by copying the origin project to src/main/resources/archetype-resources
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * 
     */
    private void generateArchetype() throws IOException, SAXException, XPathExpressionException {
        getLog().info("Generating archetype from " + exampleProjectPath);
        File archetypeOutputDir = new File(baseDir, "src/main/resources/archetype-resources");
        copyFiles(exampleProjectPath, archetypeOutputDir);
    }

    /**
     * Copy files from sourceDir to archetypeOutputDir
     * 
     * @param sourceDir
     * @param archetypeOutputDir
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    private void copyFiles(File sourceDir, File archetypeOutputDir) throws IOException, SAXException, XPathExpressionException {
        for (File file : sourceDir.listFiles()) {
            if (file.isFile()) {
                if (file.getName().equals("pom.xml")) {
                    findPomMetadata(file);
                }

                String rootPath = exampleProjectPath.getPath();
                String relativePath = file.getPath().replaceAll(rootPath, "");
                String relativePathWithoutPackage = relativePath.replace(rootPackage.replaceAll("\\.", File.separator), "");
                String pathInterpolated = relativePathWithoutPackage;
                for (String value : extraReplaceValues) {
                    pathInterpolated = pathInterpolated.replaceAll(value, "__" + artifactExpression + "__");
                }
                // default interpolation
                pathInterpolated = pathInterpolated.replaceAll(projectPath, "__" + artifactExpression + "__");

                File dest = new File(archetypeOutputDir, pathInterpolated);
                dest.getParentFile().mkdirs();
                dest.createNewFile();

                if (isTextFile(file)) {
                    // Filtered copy files
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(dest));
                    // getLog().info("Copying from " + file + " to " + dest);
                    while (br.ready()) {
                        String line = br.readLine();
                        String content = line;

                        if (dest.getName().equals("pom.xml")) {
                            // pom.xml has special treatment
                            content = getPomLine(line);
                        }

                        for (String value : extraReplaceValues) {
                            content = content.replaceAll(value, "\\${" + artifactExpression + "}");
                        }
                        // default content interpolation
                        content = content.replaceAll(rootPackage, "\\${package}").replaceAll(projectPath, "\\${" + artifactExpression + "}");

                        if (!ignoreMode) { // Don't write content to file while on Ignore mode
                            bw.write(content + "\n");
                        }
                    }
                    closeStreams(br, bw);
                } else {
                    // Simple binary copy
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));
                    int buffer = 0;
                    while ((buffer = bis.read()) != -1) {
                        bos.write(buffer);
                    }
                    closeStreams(bis, bos);
                }
            } else if (file.isDirectory()) {
                // Recursively copy files
                copyFiles(file, archetypeOutputDir);
            }
        }

    }

    /**
     * Examine file extension to check if is it a text or binary file
     * 
     * @param file
     * @return
     */
    private boolean isTextFile(File file) {
        int i = file.getName().lastIndexOf('.');
        String fileExtension = file.getName().substring(i + 1);
        for (String ext : TEXT_EXTENSIONS) {
            if (ext.equals(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param file
     * @throws XPathExpressionException
     * @throws SAXException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void findPomMetadata(File file) throws XPathExpressionException, FileNotFoundException, IOException, SAXException {
        getLog().info("Finding metadata for " + file);
        Document doc = PositionalXMLReader.readXML(new FileInputStream(file));
        Node node = (Node) xPath.evaluate("//groupId", doc, XPathConstants.NODE);
        originalGroupId = node.getTextContent();

        node = (Node) xPath.evaluate("//artifactId", doc, XPathConstants.NODE);
        originalArtifactId = node.getTextContent();

        node = (Node) xPath.evaluate("//version", doc, XPathConstants.NODE);
        originalVersion = node.getTextContent();
    }

    /**
     * POM file needs a special treatment
     * 
     * @param line
     * @param doc
     * @return
     */
    private String getPomLine(String line) {

        if (line.trim().startsWith("<groupId>" + originalGroupId + "</groupId>")) {
            return line.replaceAll(originalGroupId, "\\${groupId}");
        }
        if (line.trim().startsWith("<artifactId>" + originalArtifactId + "</artifactId>")) {
            return line.replaceAll(originalArtifactId, "\\${" + artifactExpression + "}");
        }
        if (line.trim().startsWith("<version>" + originalVersion + "</version>")) {
            return line.replaceAll(originalVersion, "\\${version}");
        }
        if (line.trim().startsWith("<version.jboss.bom>")) {
            ignoreMode = true;
            int i = line.indexOf("<version.jboss.bom>") + "<version.jboss.bom>".length();
            int j = line.indexOf("</version.jboss.bom>");
            originalBomVersion = line.substring(i, j);
        }
        if (line.trim().startsWith("<!-- <version.jboss.bom>")) {
            ignoreMode = false;
            line = ("#if ($enterprise == \"true\" || $enterprise == \"y\" || $enterprise == \"yes\" )\n"
                + "        <!-- Certified version of the JBoss EAP components we want to use -->\n"
                + "        <version.jboss.bom>${jboss-bom-enterprise-version}</version.jboss.bom>\n"
                + "        <!-- Alternatively, comment out the above line, and un-comment the\n"
                + "            line below to use version BOMVERSION which is based on community built dependencies. -->\n"
                + "        <!-- <version.jboss.bom>BOMVERSION</version.jboss.bom> -->\n"
                + "#else\n"
                + "        <version.jboss.bom>BOMVERSION</version.jboss.bom>\n"
                + "        <!-- Alternatively, comment out the above line, and un-comment the line\n"
                + "            below to use version ${jboss-bom-enterprise-version} which is a release certified to\n"
                + "            work with JBoss EAP 6. It requires you have access to the JBoss EAP 6\n"
                + "            maven repository. -->\n"
                + "        <!-- <version.jboss.bom>${jboss-bom-enterprise-version}</version.jboss.bom>> -->\n"
                + "#end").replaceAll("BOMVERSION", originalBomVersion);
        }
        return line;
    }

    /**
     * Close any Stream opened
     * 
     * @throws IOException
     */
    private void closeStreams(Closeable... closeables) throws IOException {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                closeable.close();
            }
        }

    }

    /**
     * Clone the informed git repository and checkout the informed branch
     * 
     * @throws IOException
     * @throws GitAPIException
     * @throws TransportException
     * @throws InvalidRemoteException
     * 
     */
    private void cloneOriginProject() throws IOException, InvalidRemoteException, TransportException, GitAPIException {
        File gitLocalRepo = new File(outputPath + File.separator + "git");
        if (!gitLocalRepo.exists()) {
            getLog().info("Cloning " + projectGitRepo + " to " + gitLocalRepo);
            CloneCommand clone = Git.cloneRepository();
            clone.setBare(false);
            clone.setCloneAllBranches(true);
            clone.setDirectory(gitLocalRepo).setURI(projectGitRepo);
            clone.call();
        }
        getLog().info("Checking out " + branch + " branch");
        Git.open(gitLocalRepo).checkout().setName(branch).call();
    }
}
