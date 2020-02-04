package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        return createdRecordWithOverlay(data, overlayData, new ArrayList<String>());
    }

    protected TabularEditRecord createdRecordWithOverlay(Map<String, String> data, Map<String, String> overlayData,
            List<String> eventUrls) throws Exception {
        String objUrl = TabularEditRecord.getArkUrl(data.get(TabularRecord.OBJECT_ID));
        data.put("ark", objUrl);
        Record record = new TabularRecord( data, null);
        Document doc = record.toRDFXML();
        if (eventUrls != null && eventUrls.size() > 0) {
            Element el = (Element)doc.selectSingleNode("//dams:Object");
            for (String eventUrl : eventUrls) {
                el.addElement(new QName("event", TabularRecord.damsNS))
                  .addAttribute(new QName("resource", TabularRecord.rdfNS), eventUrl);
            }
        }

        return createdRecordWithOverlay(doc, overlayData);
    }

    protected TabularEditRecord createdRecordWithOverlay(Document doc, Map<String, String> overlayData)
            throws Exception {
        return createdRecordWithOverlay(doc, overlayData, null);
    }

    protected TabularEditRecord createdRecordWithOverlay(Document doc, Map<String, String> objectData,
            Map<String, String> compData) throws Exception {
        TabularEditRecord editRecord = new TabularEditRecord(objectData, null, doc);

        if (compData != null && compData.size() > 0) {
            TabularEditRecord comp = new TabularEditRecord();
            comp.setData(compData);
            editRecord.addComponent(comp);
        }

        return editRecord;
    }

    protected TabularEditRecord createdRecordWithOverlay(Document doc, Map<String, String> objectData,
            Map<String, String> compData, Map<String, String> subcompData) throws Exception {
        TabularEditRecord editRecord = new TabularEditRecord(objectData, null, doc);

        TabularEditRecord comp = null;
        if (compData != null && compData.size() > 0) {
            // component
            comp = new TabularEditRecord();
            comp.setData(compData);
            editRecord.addComponent(comp);

            // sub-component
            if (subcompData != null && subcompData.size() > 0) {
                TabularEditRecord subcomp = new TabularEditRecord();
                subcomp.setData(subcompData);
                comp.addComponent(subcomp);
            }
        }

        return editRecord;
    }

    protected Map<String, String> createDataWithTitle(String oid, String title) {
        return createDataWithTitle(oid, title, "object");
    }

    protected Map<String, String> createDataWithTitle(String oid, String title, String level) {
        Map<String, String> data = new HashMap<>();
        data.put(TabularRecord.OBJECT_ID, oid);
        data.put(TabularRecord.OBJECT_COMPONENT_TYPE, level);
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
