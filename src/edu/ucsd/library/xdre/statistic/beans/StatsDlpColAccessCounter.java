package edu.ucsd.library.xdre.statistic.beans;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

/**
 * Class StatsDlpColAccessCounter the counter for collection accesses
 *
 * @author lsitu@ucsd.edu
 */
public class StatsDlpColAccessCounter {
    private static Logger log = Logger.getLogger(StatsDlpColAccessCounter.class);

    protected Map<String, Integer> collsAccessMap = null;
    protected Map<String, String> collsMap = null;

    public StatsDlpColAccessCounter(Map<String, String> collsMap) {
        collsAccessMap = new HashMap<String, Integer>();
        this.collsMap = collsMap;
    }

    public Map<String, String> getCollsMap() {
        return collsMap;
    }

    public void setCollsMap(Map<String, String> collsMap) {
        this.collsMap = collsMap;
    }

    public Map<String, Integer> getCollsAccessMap() {
        return collsAccessMap;
    }

    public void addAccess(String colId) throws UnsupportedEncodingException{
        increaseCollectionAccess(colId, collsAccessMap, collsMap);
    }

    public int export(PreparedStatement ps, int id) throws Exception {
        //STATS_DLP_COLLECTION_ACCESS_INSERT insert
        int returnValue = 0;
        int numAccess = 0;
        for (String key : collsAccessMap.keySet()) {
            numAccess = collsAccessMap.get(key);
            ps.setInt(1, id);
            ps.setString(2, key);
            ps.setInt(3, numAccess);
            returnValue += ps.executeUpdate();
            ps.clearParameters();
        }

        return returnValue;
    }

    public String getCollection(Document doc) {
        if (doc != null) {
            Node node = doc.selectSingleNode("//doc/arr[@name='collections_tesim']/str");
            if (node != null) {
                return node.getText();
            }
        }
        return null;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (collsAccessMap != null) {
            Iterator<String> it = collsAccessMap.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                builder.append(key + " -> " + collsAccessMap.get(key) + "\n");
            }
        }
        return builder.toString();
    }

    /**
     * Count collections access
     * @param uri
     * @param collsAccessMap
     * @param collsMap
     */
    public static void increaseCollectionAccess(String colid, Map<String, Integer> collsAccessMap, Map<String, String> collsMap) {
        Integer count = null;
        if (colid != null) {
            colid = colid.trim().replaceAll("[;:,?'\" ]*", "");
            // Only counting the ark ids for now
            if (colid.length() == 10) {
                count = collsAccessMap.get(colid);
                if (count == null)
                    collsAccessMap.put(colid, 1);
                else
                    collsAccessMap.put(colid, ++count);
            }
        }
    }
}
