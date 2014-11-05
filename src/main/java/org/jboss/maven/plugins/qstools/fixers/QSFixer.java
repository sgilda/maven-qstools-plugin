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
package org.jboss.maven.plugins.qstools.fixers;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jboss.maven.plugins.qstools.QSToolsException;

public interface QSFixer {

    /** The Plexus role identifier. */
    String ROLE = QSFixer.class.getName();

    public void fix(final MavenProject project, final MavenSession mavenSession, final List<MavenProject> reactorProjects,
        final Log log) throws QSToolsException;

    /**
     * Defines the execution order (priority) of the fixer.
     * 
     * Higher values will be executed later then lower values)
     * 
     * @return the order of the fixer.
     */
    public int order();

    /**
     * Defines the description on what the fixer will fix
     * 
     * @return the description of the fixer
     */
    public String getFixerDescription();
}
