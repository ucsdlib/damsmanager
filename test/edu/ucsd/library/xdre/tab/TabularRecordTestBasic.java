package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * Utility methods for testing TabularEditRecord
 * @author lsitu
 *
 */
public class TabularRecordTestBasic {
    protected static void addAttribute(Element e, String name, Namespace ns, String value)
    {
        e.addAttribute( new QName(name,ns), value );
    }

    protected Element createDocumentRoot(String uri) {
        Document doc = new DocumentFactory().createDocument();
        Element rdf = TabularRecord.createRdfRoot (doc);

        Element e = TabularRecord.addElement(rdf, "Object", TabularRecord.damsNS);

        addAttribute(e, "about", TabularRecord.rdfNS, uri);
        return e;
    }

    protected TabularEditRecord createdRecordWithOverlay(Map<String, String> data, Map<String, String> overlayData)
            throws Exception {
        String objUrl = TabularEditRecord.getArkUrl(data.get(TabularRecord.OBJECT_ID));
        data.put("ark", objUrl);
        Record record = new TabularRecord( data, null);
        Document doc = record.toRDFXML();

        return createdRecordWithOverlay(doc, overlayData);
    }

    protected TabularEditRecord createdRecordWithOverlay(Document doc, Map<String, String> overlayData)
            throws Exception {
        return new TabularEditRecord(overlayData, null, doc);
    }

    protected Map<String, String> createDataWithTitle(String oid, String title) {
        Map<String, String> data = new HashMap<>();
        data.put(TabularRecord.OBJECT_ID, oid);
        data.put(TabularRecord.OBJECT_COMPONENT_TYPE, "object");
        data.put("title", title);

        return data;
    }

    protected File getResourceFile(String fileName) throws IOException {
        File resourceFile = new File(fileName);
        resourceFile.deleteOnExit();

        byte[] buf = new byte[4096];
        try(InputStream in = getClass().getResourceAsStream("/resources/" + fileName);
                FileOutputStream out = new FileOutputStream(resourceFile)) {

            int bytesRead = 0;
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }
        return resourceFile;
    }

    protected String getOverlayValue(String value) {
        return value + " overlay";
    }
}
