package org.jboss.maven.plugins.qschecker.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.checkstyle.CheckstyleExecutor;
import org.apache.maven.plugin.checkstyle.CheckstyleExecutorRequest;
import org.apache.maven.plugin.checkstyle.CheckstyleResults;
import org.apache.maven.plugin.checkstyle.DefaultCheckstyleExecutor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.QSCheckerException;
import org.jboss.maven.plugins.qschecker.Violation;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;

public abstract class AbstractCheckstyleChecker implements QSChecker {
    

    @Requirement(role = CheckstyleExecutor.class)
    private DefaultCheckstyleExecutor checkstyleExecutor;



    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession,
            List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        CheckstyleExecutorRequest executorRequest = new CheckstyleExecutorRequest();

        executorRequest
            .setReactorProjects(reactorProjects)
            .setSourceDirectory(project.getBasedir())
            .setTestSourceDirectory(project.getBasedir())
            .setFailsOnError(false)
            .setProject(project)
            .setConfigLocation(getCheckstyleConfig())
            .setLog(log)
            .setEncoding("UTF-8")
            .setHeaderLocation("header.txt")
            .setIncludes(getIncludes())
            .setExcludes("**/target/**, **/.*/*.*");
        
        try {
            CheckstyleResults checkstyleResults = checkstyleExecutor.executeCheckstyle(executorRequest);
            Map<String, List<AuditEvent>> files = checkstyleResults.getFiles();
            for (String file : files.keySet()) {
                List<AuditEvent> events = files.get(file);
                //If file has events/violations
                if (events.size() > 0){
                    List<Violation> violations = new ArrayList<Violation>();
                    for(AuditEvent event: events){
                        //Add each checktyle AuditEvent as a new Violation
                        violations.add(new Violation(this.getClass(), event.getLine(), event.getMessage()));
                    }
                    results.put(file, violations);
                }
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
        return results;
    }



   abstract String getIncludes();



   abstract String getCheckstyleConfig();


}
