package org.jboss.maven.plugins.qstools.fixers;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component(role = QSFixer.class, hint = "GroupIdFixer")
public class GroupIdFixer extends AbstractBaseFixerAdapter {

    @Override
    public String getFixerDescription() {
        return "Make all projects to use the same groupId especified with -Dqstool.groupId ";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        String groupId = System.getProperty("qstools.groupId");
        if (groupId == null){
            throw new IllegalAccessException("You should specifiy -Dqstool.groupId at the command line for this checker"); 
        }
        Node node = (Node) getxPath().evaluate("/project/groupId", doc, XPathConstants.NODE);
        if (node != null && !project.getGroupId().equals(groupId)) {
            node.setTextContent(groupId);
        }
        if (project.getParent() != null && project.getParent().getFile() != null){
            Node nodeParent = (Node) getxPath().evaluate("/project/parent/groupId", doc, XPathConstants.NODE);
            nodeParent.setTextContent(groupId);
        }
        XMLUtil.writeXML(doc, project.getFile());
    }

}
