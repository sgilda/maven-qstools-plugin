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
package org.jboss.maven.plugins.qstools.checkers;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.jdf.stacks.client.StacksClient;
import org.jboss.jdf.stacks.model.Bom;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.maven.MavenDependency;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "dependencyChecker")
public class DependencyChecker extends AbstractProjectChecker {

    /**
     * List of all managed Dependencies and what BOMs it is present
     */
    private static Map<MavenGA, Set<Bom>> managedDependencies;

    @Requirement
    private RepositorySystem repositorySystem;

    /**
     * Parse all BOMs to find all dependencies that it manages
     */
    private void setupManagedDependencies(MavenProject project) throws Exception {
        managedDependencies = new HashMap<MavenGA, Set<Bom>>();
        StacksClient sc = new StacksClient();
        List<Bom> boms = sc.getStacks().getAvailableBoms();
        for (Bom bom : boms) {
            readBOMArtifact(project, bom, bom.getGroupId(), bom.getArtifactId(), bom.getRecommendedVersion());
        }
    }

    /**
     * Resolve Each Maven Artifact from BOM Information
     * 
     * @param mavenProject the project used to retrieve the remote artifact repositories
     * @param bom the bom model that is being parsed
     * @param groupId
     * @param artifactId
     * @param version
     * @throws Exception
     */
    private void readBOMArtifact(MavenProject mavenProject, Bom bom, String groupId, String artifactId, String version) throws Exception {
        Artifact pomArtifact = repositorySystem.createArtifact(groupId, artifactId, version, "", "pom");
        ArtifactResolutionRequest arr = new ArtifactResolutionRequest();

        arr.setArtifact(pomArtifact).setRemoteRepositories(mavenProject.getRemoteArtifactRepositories()).setLocalRepository(getMavenSession().getLocalRepository());
        repositorySystem.resolve(arr);
        // Given the resolved maven artifact for BOM, parse it.
        readBOM(mavenProject, bom, pomArtifact);

    }

    /**
     * 
     * Parse the BOM file recursively to find all managed dependencies
     * 
     * @param mavenProject
     * @param bom the BOM model that originates the request
     * @param pomArtifact the maven artifact that represents the BOM
     * @throws Exception
     */
    private void readBOM(MavenProject mavenProject, Bom bom, Artifact pomArtifact) throws Exception {
        if (pomArtifact.getFile().exists()) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomArtifact.getFile()));
            // recursive parent search
            if (model.getParent() != null) {
                Parent p = model.getParent();
                readBOMArtifact(mavenProject, bom, p.getGroupId(), p.getArtifactId(), p.getVersion());
            }
            if (model.getDependencyManagement() != null) {
                for (Dependency dep : model.getDependencyManagement().getDependencies()) {
                    // For each dependency add its bom
                    MavenGA mvnDependency = new MavenGA(dep.getGroupId(), dep.getArtifactId());
                    if (managedDependencies.get(mvnDependency) == null) {
                        managedDependencies.put(mvnDependency, new HashSet<Bom>());
                    }
                    managedDependencies.get(mvnDependency).add(bom);
                }
            }
        } else {
            String msg = String.format("BOM %s (from jdf-stacks) was not found. You may need to configure an EAP/WFK repository in your settings.xml.", pomArtifact);
            getLog().debug(msg);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if all dependencies are using a BOM (not declare a version) and suggest what BOMs to use";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        if (managedDependencies == null) {
            setupManagedDependencies(project);
        }
        NodeList dependencies = (NodeList) getxPath().evaluate("/project/dependencies/dependency", doc, XPathConstants.NODESET);
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = getDependencyProvider().getDependencyFromNode(project, dependency);
            int lineNumber = getLineNumberFromNode(dependency);
            MavenGA ga = new MavenGA(mavenDependency.getGroupId(), mavenDependency.getArtifactId());
            if (mavenDependency.getDeclaredVersion() != null) {
                StringBuilder sb = new StringBuilder(String.format("You should NOT declare a version for %s:%s:%s. Consider using a BOM. ", mavenDependency.getGroupId(),
                    mavenDependency.getArtifactId(), mavenDependency.getDeclaredVersion()));
                // If has a BOM for it
                if (managedDependencies.get(ga) != null) {
                    sb.append("Recommended BOMs with this dependency: ");
                    for (Bom bom : managedDependencies.get(ga)) {
                        sb.append(String.format("%s:%s:%s / ", bom.getGroupId(), bom.getArtifactId(), bom.getRecommendedVersion()));
                    }
                }
                addViolation(project.getFile(), results, lineNumber, sb.toString());
            }
        }
    }

    /**
     * Represents the Maven GroupId and ArtifactId
     * 
     */
    private class MavenGA {
        private String groupId;

        private String artifactId;

        public MavenGA(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MavenGA other = (MavenGA) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (artifactId == null) {
                if (other.artifactId != null)
                    return false;
            } else if (!artifactId.equals(other.artifactId))
                return false;
            if (groupId == null) {
                if (other.groupId != null)
                    return false;
            } else if (!groupId.equals(other.groupId))
                return false;
            return true;
        }

        private DependencyChecker getOuterType() {
            return DependencyChecker.this;
        }

    }

}
