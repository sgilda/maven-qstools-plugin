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
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.Violation;
import org.jboss.maven.plugins.qschecker.maven.MavenDependency;
import org.jboss.maven.plugins.qschecker.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "dependencyChecker")
public class DependencyChecker extends AbstractPomChecker {

    /**
     * List of all managed Dependencies from each BOM (Bill of Materials)
     */
    private static Map<MavenGA, Set<Bom>> managedDependencies;

    @Requirement
    private RepositorySystem repositorySystem;

    private void setupManagedDependencies(MavenProject project) throws Exception {
        managedDependencies = new HashMap<MavenGA, Set<Bom>>();
        StacksClient sc = new StacksClient();
        List<Bom> boms = sc.getStacks().getAvailableBoms();
        for (Bom bom : boms) {
            readBOMArtifact(project, bom, bom.getGroupId(), bom.getArtifactId(), bom.getRecommendedVersion());
        }
    }

    private void readBOMArtifact(MavenProject mavenProject, Bom bom, String groupId, String artifactId, String version) throws Exception {
        Artifact pomArtifact = repositorySystem.createArtifact(groupId, artifactId, version, "", "pom");
        ArtifactResolutionRequest arr = new ArtifactResolutionRequest();

        arr.setArtifact(pomArtifact).setRemoteRepositories(mavenProject.getRemoteArtifactRepositories()).setLocalRepository(mavenSession.getLocalRepository());
        repositorySystem.resolve(arr);
        readBOM(mavenProject, bom, pomArtifact);

    }

    /**
     * @param mavenProject
     * @param bomVersion
     * @param pomArtifact
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
                    MavenGA mvnDependency = new MavenGA(dep.getGroupId(), dep.getArtifactId());
                    if (managedDependencies.get(mvnDependency) == null) {
                        managedDependencies.put(mvnDependency, new HashSet<Bom>());
                    }
                    managedDependencies.get(mvnDependency).add(bom);
                }
            }
        } else {
            String msg = String.format("BOM %s (from jdf-stacks) was not found. Maybe you need to setup a EAP/WFK repository on your settings.xml", pomArtifact);
            log.warn(msg);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check if all dependencies are using a BOM (not declare a version) and suggest what BOMs to use";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        if (managedDependencies == null) {
            setupManagedDependencies(project);
        }
        NodeList dependencies = (NodeList) xPath.evaluate("/project/dependencies/dependency", doc, XPathConstants.NODESET);
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node dependency = dependencies.item(x);
            MavenDependency mavenDependency = getDependencyFromNode(project, dependency);
            int lineNumber = Integer.parseInt((String) dependency.getUserData(PositionalXMLReader.LINE_NUMBER_KEY_NAME));
            MavenGA ga = new MavenGA(mavenDependency.getGroupId(), mavenDependency.getArtifactId());
            if (mavenDependency.getDeclaredVersion() != null) {
                StringBuilder sb = new StringBuilder(String.format("You shoul NOT declare versions as declared for %s:%s:%s. Consider using a BOM. ", mavenDependency.getGroupId(),
                        mavenDependency.getArtifactId(), mavenDependency.getDeclaredVersion()));
                // If has a BOM for it
                if (managedDependencies.get(ga) != null) {
                    sb.append("Recommended BOMs with this dependency: ");
                    for (Bom bom : managedDependencies.get(ga)) {
                        sb.append(String.format("%s:%s:%s / ", bom.getGroupId(), bom.getArtifactId(), bom.getRecommendedVersion()));
                    }
                }
                addViolation(project, results, lineNumber, sb.toString());
            }
        }
    }

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
