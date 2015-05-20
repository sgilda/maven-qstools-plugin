package org.jboss.maven.plugins.qstools.fixers;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component(role = QSFixer.class, hint = "GroupIdFixer")
public class GroupIdFixer extends AbstractBaseFixerAdapter {

    private List<GA> changedGA = new ArrayList<GroupIdFixer.GA>();

    @Override
    public String getFixerDescription() {
        return "Make all projects to use the same groupId especified with -Dqstools.groupId ";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        String groupId = System.getProperty("qstools.groupId");
        if (groupId == null) {
            throw new IllegalAccessException("You should specifiy -Dqstools.groupId at the command line for this checker");
        }
        Node node = (Node) getxPath().evaluate("/project/groupId", doc, XPathConstants.NODE);
        if (node != null && !project.getGroupId().equals(groupId)) {
            changedGA.add(new GA(node.getTextContent(), project.getArtifactId()));
            node.setTextContent(groupId);
        }
        if (project.getParent() != null && project.getParent().getFile() != null) {
            Node nodeParent = (Node) getxPath().evaluate("/project/parent/groupId", doc, XPathConstants.NODE);
            nodeParent.setTextContent(groupId);
        }
        // Update each incorrect groupId dependency
        for (GA ga : changedGA) {
            // It can have more than one occurrence on the same file
            NodeList dependencyNodes = (NodeList) getxPath().evaluate("//dependency", doc, XPathConstants.NODESET);
            for (int x = 0; x < dependencyNodes.getLength(); x++) {
                Node dependencyNode = dependencyNodes.item(x);
                NodeList childs = dependencyNode.getChildNodes();
                String dependencyGroupId = null;
                Node groupIdNode = null;
                String artifactId = null;
                for (int i = 0; i < childs.getLength(); i++) {
                    Node nodeDependency = childs.item(i);
                    if (nodeDependency.getNodeName().equals("groupId")) {
                        dependencyGroupId = nodeDependency.getTextContent();
                        groupIdNode = nodeDependency;

                    }
                    if (nodeDependency.getNodeName().equals("artifactId")) {
                        artifactId = nodeDependency.getTextContent();
                    }
                }
                if (dependencyGroupId.equals(ga.getGroupId()) && artifactId.equals(ga.getArtifactId())) {
                    groupIdNode.setTextContent(groupId);
                }
            }
        }

        XMLUtil.writeXML(doc, project.getFile());
    }

    private class GA {

        private String groupId;
        private String artifactId;

        public GA(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }
        
        @Override
        public String toString() {
            return "GA - " + groupId + ":" + artifactId;
        }

    }

}
