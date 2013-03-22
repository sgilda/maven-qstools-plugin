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

public class MavenDependency {

    private String groupId;

    private String artifactId;

    private String declaredVersion;

    private String interpoledVersion;

    private String type;

    private String scope;

    public MavenDependency(String groupId, String artifactId, String declaredVersion, String interpoledVersion, String type, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.declaredVersion = declaredVersion;
        this.interpoledVersion = interpoledVersion;
        this.type = type;
        this.scope = scope;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getDeclaredVersion() {
        return declaredVersion;
    }

    public String getInterpoledVersion() {
        return interpoledVersion;
    }

    public String getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("MavenDependency [groupId=%s, artifactId=%s, declaredVersion=%s, interpoledVersion=%s, type=%s, scope=%s]", groupId, artifactId, declaredVersion,
                interpoledVersion, type, scope);
    }


}
