package org.jboss.maven.plugins.qschecker.checkers;

import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qschecker.QSChecker;

@Component(role = QSChecker.class, hint = "fileFormatChecker")
public class FileFormatChecker extends AbstractCheckstyleChecker {


    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Verifies if project files (*.java, *.xml, *.properties) is using proper identation and spaces as tab";
    }

    @Override
    String getIncludes() {
        return "**/*.java";
    }

    @Override
    String getCheckstyleConfig() {
       return "checkstyle-format.xml";
    }

}