package edu.ucsd.library.xdre.tab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.QName;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Standard InputStreamEditRecord constructed from Excel source,
 * which provide utility functions for post processing.
 * @author lsitu
 */
public class InputStreamEditRecord extends InputStreamRecord {

    public InputStreamEditRecord(Record record, DAMSClient damsClient) throws Exception {
        super(record, damsClient);

        initEditing((TabularEditRecord)record);
    }

    private void initEditing(TabularEditRecord record) throws UnsupportedEncodingException,
            IOException, DocumentException, Exception {
        record.setDocument(damsClient.getRecord(id));
        record.editDocument();
        this.rdf = record.toRDFXML();

        // Collections post-processing for predicate linking
       applyCollectionPredicates();
    }

    /*
     * Post processing: Lookup linked collection to replace the collection predicate
     */
    private void applyCollectionPredicates() throws Exception {
        List<Node> resNodes = rdf.selectNodes("/rdf:RDF//dams:collection/@rdf:resource");
        for (Node resNode : resNodes) {
            String uri = resNode.getStringValue();
            Document colDoc = damsClient.getRecord(uri);
            String colType = colDoc.selectSingleNode("//*[@rdf:about='" + uri + "']").getName();

            Node editNode = rdf.selectSingleNode("/rdf:RDF/*");
            String preName = "";
            if (editNode.getName().contains("Collection")) {
                // collection hierarchy linking
                preName = "has" + (colType.contains("Part") ? "Part" : colType);
            } else {
                // object collection linking
                preName = colType.substring(0, 1).toLowerCase() + colType.substring(1);
            }

            resNode.getParent().setQName(new QName(preName, resNode.getParent().getNamespaceForPrefix("dams")));
        }
    }
}
