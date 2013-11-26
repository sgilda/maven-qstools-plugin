package org.jboss.maven.plugins.qstoolsc.common;

import static org.jboss.maven.plugins.qstools.Constants.TARGET_PRODUCT_TAG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.config.Rules;

@Component(role = PomNameUtil.class)
public class PomNameUtil {

    public String getExpectedPattern(MavenProject project, Rules rules) throws IOException {
        String pomNamePattern = rules.getPomNamePattern();
        String pomNamePatternSubmodule = rules.getPomNamePatternForSubmodule();
        String folderName = project.getBasedir().getName();
        String parentFolder = project.getBasedir().getParentFile().getName();
        String pattern;
        if (isSubProjec(project)) {
            // Get Target Product from parent Readme
            File parentReadme = new File(project.getBasedir().getParent(), "README.md");
            String targetProject = getTargetProduct(parentReadme);
            pattern = pomNamePatternSubmodule.replace("<target-product>", targetProject).replace("<project-folder>", parentFolder).replace("<submodule-folder>", folderName);
        } else {
            File readme = new File(project.getBasedir(), "README.md");
            if (readme.exists()) {
                String targetProject = getTargetProduct(readme);
                pattern = pomNamePattern.replace("<target-product>", targetProject).replace("<project-folder>", folderName);
            } else {
                // Not able to get the targetProject. Using the existing name to avoid wrong violations
                pattern = project.getName();
            }
        }
        return pattern;
    }

    /**
     * @param file
     * @return empty string if can't find the target product
     * @throws IOException
     */
    private String getTargetProduct(File readme) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(readme));
        try {
            while (br.ready()) {
                String line = br.readLine();
                if (line.startsWith(TARGET_PRODUCT_TAG)) {
                    return line.substring(TARGET_PRODUCT_TAG.length(), line.length()).trim();
                }
            }
            return "";
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * HAcky way to determine if the project is a maven submodule or not. It uses the presence of root README.md to determine if
     * it is a submodule
     * 
     * @param project
     * @return
     */
    private boolean isSubProjec(MavenProject project) {
        return ((!new File(project.getBasedir(), "README.md").exists()) && (new File(project.getBasedir().getParent(), "README.md").exists()));
    }

}
