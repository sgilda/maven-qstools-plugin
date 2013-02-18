package org.jboss.maven.plugins.qschecker;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;


public interface QSChecker {

    /** The Plexus role identifier. */
    String ROLE = QSChecker.class.getName();
    
    public void setup(MavenProject project, Log log);

    public void check() throws QSCheckerException;

}
