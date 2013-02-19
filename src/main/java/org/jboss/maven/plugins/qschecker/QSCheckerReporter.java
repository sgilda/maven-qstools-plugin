/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.maven.plugins.qschecker;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.PlexusContainer;

/**
 * @author rafaelbenevides
 * 
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, requiresProject = true, aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class QSCheckerReporter extends AbstractMavenReport {

    @Component
    private PlexusContainer container;

    @Component
    private Renderer siteRenderer;

    @Component
    private MavenProject mavenProject;

    @Parameter(property = "project.reporting.outputDirectory", required = true)
    private File outputDirectory;

    @Parameter(property = "reactorProjects", readonly = true)
    private List<MavenProject> reactorProjects;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    @Override
    public String getDescription(Locale arg0) {
        return "Quickstarts violations";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    @Override
    public String getName(Locale locale) {
        return "Quickstarts Checker Report";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    @Override
    public String getOutputName() {
        return "qschecker";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    @Override
    protected String getOutputDirectory() {
        return outputDirectory.getAbsolutePath();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    @Override
    protected MavenProject getProject() {
        return mavenProject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        Sink sink = getSink();
        startReport(sink, locale);

        List<QSChecker> checkers;
        try {
            checkers = container.lookupList(QSChecker.class);

            for (QSChecker qc : checkers) {
                sink.section1();
                doCheckerHeader(sink, qc);

                // Configure and execute the checker
                qc.setup(mavenProject, reactorProjects, getLog());
                Map<String, List<Violation>> violations = qc.check();

                doCheckerTableResult(sink, violations);
                sink.section1_();
            }
        } catch (Exception e) {
            throw new MavenReportException(e.getMessage(), e);
        }
        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * @param sink
     * @param violations
     */
    private void doCheckerTableResult(Sink sink, Map<String, List<Violation>> violations) {

        // File Sections
        for (String file : violations.keySet()) {
            sink.section2();
            sink.sectionTitle2();
            sink.text(file);
            sink.sectionTitle2_();
            sink.section2_();

            List<Violation> fileViolations = violations.get(file);

            sink.table();
            // Headers
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text("Message");
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text("Line num.");
            sink.tableHeaderCell_();
            sink.tableRow_();
            
            //Each file violation
            for (Violation violation : fileViolations) {
                sink.tableRow();
                sink.tableCell();
                sink.text(violation.getViolationMessage());
                sink.tableCell_();
                sink.tableCell();
                sink.text(String.valueOf(violation.getLineNumber()));
                sink.tableCell_();
                sink.tableRow_();
            }
            sink.table_();

        }
    }

    /**
     * @param sink
     * @param qc
     */
    private void doCheckerHeader(Sink sink, QSChecker qc) {
        sink.sectionTitle1();
        sink.text(qc.getClass().getSimpleName());
        sink.sectionTitle1_();
        sink.text(qc.getCheckerDescription());
    }

    /**
     * @param locale
     * @param sink
     * 
     */
    private void startReport(Sink sink, Locale locale) {
        sink.head();
        sink.title();
        sink.text(getName(locale));
        sink.title_();
        sink.head_();
        sink.body();
    }

}
