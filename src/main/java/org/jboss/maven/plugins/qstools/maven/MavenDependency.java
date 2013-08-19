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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((interpoledVersion == null) ? 0 : interpoledVersion.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        MavenDependency other = (MavenDependency) obj;
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
        if (interpoledVersion == null) {
            if (other.interpoledVersion != null)
                return false;
        } else if (!interpoledVersion.equals(other.interpoledVersion))
            return false;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
