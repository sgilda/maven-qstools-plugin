package org.jboss.maven.plugins.qstools.fixers;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.checkers.LicenseChecker;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Fixer for {@link LicenseChecker}
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "LicenseFixer")
public class LicenseFixer extends AbstractBaseFixerAdapter {

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        Node licenseURL = (Node) getxPath().evaluate("/project/licenses/license/url", doc, XPathConstants.NODE);
        if (licenseURL == null || !licenseURL.getTextContent().contains("apache")) {
            Node licensesElement = (Node) getxPath().evaluate("/project/licenses", doc, XPathConstants.NODE);
            // Create <licenses/> if it doesn't exists
            if (licensesElement == null) {
                licensesElement = doc.createElement("licenses");

                Element properties = (Element) getxPath().evaluate("/project/properties", doc, XPathConstants.NODE);
                properties.getParentNode().insertBefore(licensesElement, properties);
                properties.getParentNode().insertBefore(doc.createTextNode("\n\n    "), properties); // LF + LF + 4 spaces
            }
            Element license = doc.createElement("license");

            Element name = doc.createElement("name");
            name.setTextContent("Apache License, Version 2.0");

            Element distribution = doc.createElement("distribution");
            distribution.setTextContent("repo");

            Element url = doc.createElement("url");
            url.setTextContent("http://www.apache.org/licenses/LICENSE-2.0.html");

            license.appendChild(name);
            license.appendChild(distribution);
            license.appendChild(url);

            licensesElement.appendChild(doc.createTextNode("\n        ")); // LF + 8 spaces
            licensesElement.appendChild(license);
        }
        XMLWriter.writeXML(doc, project.getFile());
    }

}
