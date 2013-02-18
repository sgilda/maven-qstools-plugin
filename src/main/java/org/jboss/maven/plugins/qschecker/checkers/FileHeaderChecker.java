package org.jboss.maven.plugins.qschecker.checkers;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.checkstyle.CheckstyleExecutor;
import org.apache.maven.plugin.checkstyle.CheckstyleExecutorRequest;
import org.apache.maven.plugin.checkstyle.CheckstyleResults;
import org.apache.maven.plugin.checkstyle.DefaultCheckstyleExecutor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.QSCheckerException;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;

@Component(role=QSChecker.class, hint="fileheader")
public class FileHeaderChecker implements QSChecker {
    
    @Requirement(role=CheckstyleExecutor.class)
    private DefaultCheckstyleExecutor checkstyleExecutor;
    
    private MavenProject project;

    private Log log;
    
    @Override
    public void setup(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
    }
    
    @Override
    public void check() throws QSCheckerException {
        CheckstyleExecutorRequest executorRequest = new CheckstyleExecutorRequest();
        executorRequest
            .setConfigLocation("checkstyle-quickstarts.xml")
            .setProject(project)
            .setLog(log)
            .setEncoding("UTF-8")
            .setHeaderLocation("header.txt")
            .setSourceDirectory(new File(project.getBuild().getSourceDirectory()).getParentFile())
            .setIncludes("**/*.java, **/*.xml, **/*.properties");
        System.out.println(executorRequest.getSourceDirectory());

        try {
            CheckstyleResults results = checkstyleExecutor.executeCheckstyle(executorRequest);
            Map<String, List<AuditEvent>> files = results.getFiles();
            for (String file: files.keySet()){
                System.out.println(file);
                List<AuditEvent> events = files.get(file);
                for (AuditEvent event: events){
                    System.out.println(event.getMessage());
                    System.out.println(event.getLine());
                }
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }

    }

}