package org.jboss.maven.plugins.qstools.xml;

import org.w3c.dom.Comment;
import org.w3c.dom.Node;

public class XMLUtil {

    public static Comment getPreviousCommentElement(Node node) {
        Node prevSibling = node.getPreviousSibling();
        while (prevSibling != null) {
            if (prevSibling.getNodeType() == Node.COMMENT_NODE) {
                return (Comment) prevSibling;
            }
            prevSibling = prevSibling.getPreviousSibling();
        }

        return null;
    }

    public static void removePreviousWhiteSpace(Node node) {

        Node prev = node.getPreviousSibling();
        if (prev != null && prev.getNodeType() == Node.TEXT_NODE && prev.getNodeValue().trim().length() == 0) {
            node.getParentNode().removeChild(prev);
        }
    }

    public static int getLineNumberFromNode(Node node) {
        if (node == null) {
            return 0;
        }
        return (Integer) node.getUserData(PositionalXMLReader.BEGIN_LINE_NUMBER_KEY_NAME);
    }

}
