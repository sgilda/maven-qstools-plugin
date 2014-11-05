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
package org.jboss.maven.plugins.qstools.maven;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = DependencyProvider.class)
public class DependencyProvider {

    public MavenDependency getDependencyFromNode(MavenProject project, Node dependency) throws InterpolationException {
        String groupId = null;
        String artifactId = null;
        String declaredVersion = null;
        String interpoledVersion = null;
        String type = null;
        String scope = null;
        for (int x = 0; x < dependency.getChildNodes().getLength(); x++) {
            Node node = dependency.getChildNodes().item(x);
            if ("groupId".equals(node.getNodeName())) {
                groupId = node.getTextContent();
            }
            if ("artifactId".equals(node.getNodeName())) {
                artifactId = node.getTextContent();
            }
            if ("version".equals(node.getNodeName())) {
                declaredVersion = node.getTextContent();
                interpoledVersion = resolveMavenProperty(project, declaredVersion);
            }
            if ("type".equals(node.getNodeName())) {
                type = node.getTextContent();
            }
            if ("scope".equals(node.getNodeName())) {
                scope = node.getTextContent();
            }
        }
        return new MavenDependency(groupId, artifactId, declaredVersion, interpoledVersion, type, scope);
    }

    private String resolveMavenProperty(MavenProject project, String textContent) throws InterpolationException {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        // Associate project.model with ${project.*} and ${pom.*} prefixes
        PrefixedValueSourceWrapper modelWrapper = new PrefixedValueSourceWrapper(new ObjectBasedValueSource(project.getModel()), "project.", true);
        interpolator.addValueSource(modelWrapper);
        interpolator.addValueSource(new PropertiesBasedValueSource(project.getModel().getProperties()));
        return interpolator.interpolate(textContent);
    }

}
