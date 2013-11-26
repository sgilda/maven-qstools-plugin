package org.jboss.maven.plugins.qstools.fixers;

import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.jboss.maven.plugins.qstools.AbstractProjectWalker;
import org.jboss.maven.plugins.qstools.Violation;
import org.w3c.dom.Document;

public abstract class AbstractBaseFixerAdapter extends AbstractProjectWalker {

    @Override
    public String getCheckerDescription() {
        // Empty method
        return null;
    }

    @Override
    public void checkProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        // Empty method
    }

}
