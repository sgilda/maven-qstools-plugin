package org.jboss.maven.plugins.qschecker.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.jdf.stacks.client.StacksClient;
import org.jboss.jdf.stacks.model.Bom;
import org.jboss.jdf.stacks.model.Stacks;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.QSCheckerException;
import org.jboss.maven.plugins.qschecker.Violation;
import org.jboss.maven.plugins.qschecker.maven.MavenDependency;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component(role = QSChecker.class, hint = "bomChecker")
public class BomChecker implements QSChecker {

    private Stacks stacks = new StacksClient().getStacks();

    private MavenSession mavenSession;

    private XPath xPath = XPathFactory.newInstance().newXPath();

    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession,
            List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        this.mavenSession = mavenSession;
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        processProject(project, results);
        for (MavenProject mavenProject : reactorProjects) {
            processProject(mavenProject, results);
        }

        return results;
    }

    private void processProject(final MavenProject project, final Map<String, List<Violation>> results)
            throws QSCheckerException {

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(project.getFile());
            NodeList dependencies = (NodeList) xPath.evaluate("/project/dependencyManagement/dependencies/dependency", doc,
                    XPathConstants.NODESET);
            List<MavenDependency> managedDependencies = new ArrayList<MavenDependency>();
            // Iterate over all Declared Managed Dependencies
            for (int x = 0; x < dependencies.getLength(); x++) {
                Bom bomUsed = null;
                Node dependency = dependencies.item(x);
                MavenDependency mavenDependency = getDependencyFromNode(project, dependency);
                managedDependencies.add(mavenDependency);
                for (Bom bom : stacks.getAvailableBoms()) {
                    if (bom.getGroupId().equals(mavenDependency.getGroupId())
                            && bom.getArtifactId().equals(mavenDependency.getArtifactId())) {
                        bomUsed = bom;
                    }
                }
                if (bomUsed == null) {
                    addViolation(project, results, new Violation(BomChecker.class, 0, mavenDependency
                            + " isn't a JBoss/JDF BOM"));
                }
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }

    }

    private MavenDependency getDependencyFromNode(MavenProject project, Node dependency) {
        String groupId = null;
        String artifactId = null;
        String version = null;
        String type = null;
        String scope = null;
        for (int x = 0; x < dependency.getChildNodes().getLength(); x++) {
            Node node = dependency.getChildNodes().item(x);
            if ("groupId".equals(node.getNodeName())) {
                groupId = node.getTextContent();
            }
            if ("artifactId".equals(node.getNodeName())) {
                artifactId = node.getTextContent();
            }
            if ("version".equals(node.getNodeName())) {
                String rawProperty = node.getTextContent().replace("${", "").replace("}", "");
                String projectVersion = project.getProperties().getProperty(rawProperty);
                version = projectVersion==null?rawProperty:projectVersion;
            }
            if ("type".equals(node.getNodeName())) {
                type = node.getTextContent();
            }
            if ("scope".equals(node.getNodeName())) {
                scope = node.getTextContent();
            }
        }
        return new MavenDependency(groupId, artifactId, version, type, scope);
    }

    private void addViolation(final MavenProject mavenProject, final Map<String, List<Violation>> results,
            final Violation violation) {
        // Get relative path based on maven work dir
        String fileAsString = mavenProject.getFile().getAbsolutePath()
                .replaceAll((mavenSession.getRequest().getBaseDirectory() + "/"), "");
        if (results.get(fileAsString) == null) {
            results.put(fileAsString, new ArrayList<Violation>());
        }
        results.get(fileAsString).add(violation);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check and verify if all quickstarts are using the same/latest BOM versions";
    }

}