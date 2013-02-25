package org.jboss.maven.plugins.qschecker.maven;

public class MavenDependency {
    

    private String groupId;
    
    private String artifactId;
        
    private String version;
    
    private String type;
    
    private String scope;

    public MavenDependency( String groupId, String artifactId, String version, String type, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.scope = scope;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("MavenDependency [groupId=%s, artifactId=%s, version=%s, type=%s, scope=%s]", groupId, artifactId,
                version, type, scope);
    }
    
    

}
