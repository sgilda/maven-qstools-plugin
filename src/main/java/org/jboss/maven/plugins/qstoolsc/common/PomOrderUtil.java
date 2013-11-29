package org.jboss.maven.plugins.qstoolsc.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component(role = PomOrderUtil.class)
public class PomOrderUtil {

    public Map<String, Node> getElementsOrder(MavenProject project, Document doc, List<String> pomOrder)
        throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Map<String, Node> elementsFound = new LinkedHashMap<String, Node>();
        // Find all elements position
        for (String element : pomOrder) {
            Node elementNode = (Node) xPath.evaluate("/project/" + element, doc, XPathConstants.NODE);
            if (elementNode != null) {
                elementsFound.put(element, elementNode);
            }

        }
        return elementsFound;
    }
}
