
package org.jboss.maven.plugins.qschecker;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;


public interface QSChecker {

    /** The Plexus role identifier. */
    String ROLE = QSChecker.class.getName();
    
    public Map<String, List<Violation>> check(final MavenProject project, final MavenSession mavenSession, final List<MavenProject> reactorProjects, final Log log) throws QSCheckerException;
    
    public String getCheckerDescription();

}
