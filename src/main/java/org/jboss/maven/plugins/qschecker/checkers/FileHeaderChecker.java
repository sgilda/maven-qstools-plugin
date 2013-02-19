package org.jboss.maven.plugins.qschecker.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.plugin.checkstyle.CheckstyleExecutor;
import org.apache.maven.plugin.checkstyle.CheckstyleExecutorRequest;
import org.apache.maven.plugin.checkstyle.CheckstyleResults;
import org.apache.maven.plugin.checkstyle.DefaultCheckstyleExecutor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.resource.ResourceManager;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.QSCheckerException;
import org.jboss.maven.plugins.qschecker.Violation;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;

@Component(role = QSChecker.class, hint = "fileheader")
public class FileHeaderChecker implements QSChecker {

    @Requirement(role = CheckstyleExecutor.class)
    private DefaultCheckstyleExecutor checkstyleExecutor;

    @Requirement(role = ResourceManager.class)
    private ResourceManager locator;

    private MavenProject project;

    private List<MavenProject> reactorProjects;

    private Log log;

    @Override
    public void setup(MavenProject project, List<MavenProject> reactorProjects, Log log) {
        this.project = project;
        log.debug("Configuring project: " + project);
        this.reactorProjects = reactorProjects;
        log.debug("Reactor projects: " + reactorProjects);
        this.log = log;
    }

    @Override
    public Map<String, List<Violation>> check() throws QSCheckerException {
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        CheckstyleExecutorRequest executorRequest = new CheckstyleExecutorRequest();
        executorRequest
            .setAggregate(true)
            .setReactorProjects(reactorProjects)
            .setSourceDirectory(project.getBasedir())
            .setFailsOnError(false)
            .setProject(project)
            .setConfigLocation("checkstyle-quickstarts.xml")
            .setLog(log)
            .setEncoding("UTF-8")
            .setHeaderLocation("header.txt")
            .setIncludes("**/*.java, **/*.xml, **/*.properties");
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
                        violations.add(new Violation(event.getLine(), event.getMessage()));
                    }
                    results.put(file, violations);
                }
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
        return results;
    }

    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Verifies if project files (*.java, *.xml, *.properties) contains license header";
    }

}