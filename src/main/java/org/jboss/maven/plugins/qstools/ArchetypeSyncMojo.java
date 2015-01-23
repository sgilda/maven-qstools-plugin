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
import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.jgit.api.ApplyCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.PatchApplyException;
import org.eclipse.jgit.api.errors.PatchFormatException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
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
@Mojo(name = "archetypeSync", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true,
    threadSafe = true,
    aggregator = false)
public class ArchetypeSyncMojo extends AbstractMojo {

    private XPath xPath = XPathFactory.newInstance().newXPath();

    // Files that will have their content parsed by the interpolation process
    private static final String[] TEXT_EXTENSIONS = { "java", "css", "html", "xhtml", "md", "xml", "properties", "sql" };

    /**
     * Git repository that holds the maven project
     */
    @Parameter(required = true, readonly = true)
    private String projectGitRepo;

    /**
     * Branch/Tag of the git repository that holds the origin project
     */
    @Parameter(defaultValue = "master")
    private String branch;

    /**
     * Relative path to project that originates the Archetype
     */
    @Parameter(required = true, readonly = true)
    private String projectPath;

    /**
     * Root package of the origin project
     */
    @Parameter(required = true, readonly = true)
    private String rootPackage;

    /**
     * If a project is multi module, this flag should be enabled. This will use ${rootArtifactId} instead of ${artifactId}
     */
    @Parameter(defaultValue = "false", readonly = true)
    private boolean multiModuleProject;

    /**
     * Extra string values that should be replaced by ${artifactId} in file content, or __artifactId__ in file name
     */
    @Parameter(readonly = true)
    private String[] archetypeExpressionReplaceValues;

    /**
     * expressions that will be ignored when replacing the values specified by {@link #archetypeExpressionReplaceValues}
     */
    @Parameter(readonly = true)
    private String[] ignoredArchetypeExpressionReplaceValues = new String[] {};

    /**
     * Extra string values that should replaces the key for the ${value}. The value will be automatically transformed in a
     * expression by adding ${} around the value string.
     * 
     * Example: <html5mobi>tableSuffix</html5mobi> - html5mobi will be replaced by ${tableSuffix}
     */
    @Parameter(readonly = true)
    private Map<String, String> replaceValueWithExpression = new HashMap<String, String>();

    /**
     * This will apply a patch file to the generated archetype to modify the generated synch.
     */
    @Parameter(readonly = true)
    private File applyPatch;

    @Parameter(property = "project.build.directory", required = true)
    private String outputPath;

    @Parameter(property = "basedir", required = true, readonly = true)
    private String baseDir;

    // local reference to the origin project
    private File exampleProjectPath;

    // Extracted from original Pom Metadata
    private String originalGroupId = null;

    // Extracted from original Pom Metadata
    private String originalArtifactId = null;

    // Extracted from original Pom Metadata
    private String originalVersion = null;

    // artifactId or rootArtifactId base on multiModuleProject value
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
            applyPatch();
            getLog().info("Archetype synched with " + projectPath + " from " + branch + " branch. You can check what changed running git diff.");
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    /**
     * Apply a path file to the generated archetype
     * 
     * @throws IOException
     * @throws GitAPIException
     * @throws PatchApplyException
     * @throws PatchFormatException
     * 
     * @see {@link ArchetypeSyncMojo#applyPatch}
     */
    private void applyPatch() throws IOException, PatchApplyException {
        if (this.applyPatch != null) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.findGitDir(new File(baseDir))
                .build();

            Git git = new Git(repository);
            ApplyCommand applyCommand = git.apply();
            applyCommand.setPatch(new FileInputStream(applyPatch));
            try {
                applyCommand.call();
            } catch (GitAPIException e) {
                throw new PatchApplyException("Can't apply " + applyPatch, e);
            }
            getLog().info("Patch " + applyPatch + " applied");
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
        getLog().info("Removing old files from " + archetypeOutputDir);
        cleanOldArchetype(archetypeOutputDir);
        getLog().info("Copying new files to " + archetypeOutputDir);
        copyFiles(exampleProjectPath, archetypeOutputDir);
    }

    /**
     * Removes all old archetype files
     * 
     * @param archetypeOutputDir
     */
    private void cleanOldArchetype(File archetypeOutputDir) {
        for (File file : archetypeOutputDir.listFiles()) {
            if (file.isFile()) {
                file.delete();
            } else {
                cleanOldArchetype(file);
            }
        }

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
                String rootPackagePath = rootPackage.replace(".", File.separator);
                String relativePath = file.getPath().replace(rootPath, "");
                String relativePathWithoutPackage = relativePath.replace(rootPackage.replace(".", File.separator), "");
                String pathInterpolated = relativePathWithoutPackage;
                for (String value : archetypeExpressionReplaceValues) {
                    pathInterpolated = pathInterpolated.replace(value, "__" + artifactExpression + "__");
                }
                // default interpolation
                pathInterpolated = pathInterpolated.replace(projectPath, "__" + artifactExpression + "__");

                File dest = new File(archetypeOutputDir, pathInterpolated);
                dest.getParentFile().mkdirs();
                dest.createNewFile();

                if (isTextFile(file)) {
                    // Filtered copy files
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(dest));
                    getLog().debug("Copying from " + file + " to " + dest);

                    while (br.ready()) {
                        String line = br.readLine();
                        String content = line;

                        if (dest.getName().equals("pom.xml")) {
                            // pom.xml has special treatment
                            content = getPomLine(line);
                        }

                        // Verifies if there is a ignored expression
                        boolean ignored = false;
                        for (String ignoredString : ignoredArchetypeExpressionReplaceValues) {
                            if (content.contains(ignoredString)) {
                                ignored = true;
                            }
                        }
                        if (!ignored) {
                            for (String key : archetypeExpressionReplaceValues) {
                                content = content.replace(key, "${" + artifactExpression + "}");
                            }
                            for (String key : replaceValueWithExpression.keySet()) {
                                String value = "${" + replaceValueWithExpression.get(key) + "}";
                                content = content.replace(key, value);
                            }
                            // default content interpolation
                            content = content
                                .replace(rootPackage, "${package}")
                                .replace(rootPackagePath, "${packagePath}")
                                .replace(projectPath, "${" + artifactExpression + "}");
                        }

                        bw.write(content + "\n");
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
            return line.replace(originalGroupId, "${groupId}");
        }
        if (line.trim().startsWith("<artifactId>" + originalArtifactId + "</artifactId>")) {
            return line.replace(originalArtifactId, "${" + artifactExpression + "}");
        }
        if (line.trim().startsWith("<version>" + originalVersion + "</version>")) {
            return line.replace(originalVersion, "${version}");
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
     * @throws InvalidRemoteException
     * @throws TransportException
     * @throws GitAPIException
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
