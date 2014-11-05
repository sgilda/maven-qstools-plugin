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
package org.jboss.maven.plugins.qstools.checkers;

import org.apache.maven.project.MavenProject;
import org.jboss.maven.plugins.qstools.AbstractProjectWalker;
import org.w3c.dom.Document;

public abstract class AbstractBaseCheckerAdapter extends AbstractProjectWalker {

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        // Empty method
    }

    @Override
    public String getFixerDescription() {
        // Empty method
        return null;
    }

    @Override
    public int order() {
        return 0;
    }
    
}
