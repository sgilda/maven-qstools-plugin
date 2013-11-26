package org.jboss.maven.plugins.qstools.xml;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLWriter {

    public static void writeXML(Document doc, File file) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // NOI18N
        t.setOutputProperty(OutputKeys.INDENT, "yes"); // NOI18N
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // NOI18N
        Source source = new DOMSource(doc);
        Result result = new StreamResult(new FileOutputStream(file));
        t.transform(source, result);
    }
}
