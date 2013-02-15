package org.jboss.maven.plugins.qschecker;

public interface QSChecker {
    
    /** The Plexus role identifier. */
    String ROLE = QSChecker.class.getName();
    
    public void check();


}
