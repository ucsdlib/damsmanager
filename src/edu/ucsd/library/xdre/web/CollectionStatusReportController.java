package edu.ucsd.library.xdre.web;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class CollectionsReportController generate reports for DAMS collection changes
 * @author lsitu
 *
 */
public class CollectionStatusReportController implements Controller {
    private static Logger log = Logger.getLogger(CollectionStatusReportController.class);
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String beginDate = request.getParameter("beginDate");
        String endDate = request.getParameter("endDate");
        boolean exportOnly = request.getParameter("export") != null;

        DAMSClient damsClient = null;
        DAMSClient damsClientEvent = null; // query events
        Map<String, Object> dataMap = new HashMap<>();
        if (StringUtils.isNotBlank(beginDate)) {
            try {
                damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
                Map<String, String> colsMap = damsClient.listCollections();
                Map<String, String> colArkMap = CollectionReleaseController.revertMap(colsMap);
    
                damsClientEvent = new DAMSClient(Constants.DAMS_STORAGE_URL);
                damsClientEvent.setTripleStore("events"); // set event triplestore
    
                // released collections
                String sparql = buildEventSparql(DAMSClient.RECORD_RELEASED, beginDate, endDate);

                List<Map<String, String>> events = damsClientEvent.sparqlLookup(sparql);
                List<String> records = lookupRecordsByEventIds(damsClient, events);

                Map<String, String> collectionsReleased = buildCollections(colArkMap, records);

                // collections with objects added
                sparql = buildEventSparql(DAMSClient.RECORD_ADDED, beginDate, endDate);
                events = damsClientEvent.sparqlLookup(sparql);
                records = lookupRecordsByEventIds(damsClient, events);
                Map<String, String> collectionsAdded = buildCollections(colArkMap, records);

                // collections with objects removed
                sparql = buildEventSparql(DAMSClient.RECORD_REMOVED, beginDate, endDate);
                events = damsClientEvent.sparqlLookup(sparql);
                records = lookupRecordsByEventIds(damsClient, events);
                Map<String, String> collectionsRemoved = buildCollections(colArkMap, records);

                // collections with objects edited
                Map<String, String> collectionsEdited = lookupCollectionsByEventType(damsClient,
                        damsClientEvent, colArkMap, DAMSClient.RECORD_EDITED, beginDate, endDate);

                if (exportOnly) {
                    // Export collection status report result in CSV format
                    StringBuilder reportBuilder = new StringBuilder();
                    reportBuilder.append("Collection Status Report (" + beginDate + ")\n");
                    reportBuilder.append("ARK,Title\n");

                    addCsvReport(reportBuilder, "Collection Released", collectionsReleased);
                    addCsvReport(reportBuilder, "Collection with Object(s) Added", collectionsAdded);
                    addCsvReport(reportBuilder, "Collection with Object(s) Removed", collectionsRemoved);
                    addCsvReport(reportBuilder, "Collection with Object(s) Edited", collectionsEdited);

                    String fileName = "collection_report" + beginDate.replace("-", "") + ".csv";
                    OutputStream out = response.getOutputStream();
                    response.setHeader("Content-Disposition", "inline; filename=" + fileName);
                    response.setContentType("text/csv");
                    out.write(reportBuilder.toString().getBytes());
                    out.close();
                    return null;
                } else {
                    dataMap.put("recordReleased", collectionsReleased);
                    dataMap.put("recordAdded", collectionsAdded);
                    dataMap.put("recordRemoved", collectionsRemoved);
                    dataMap.put("recordEdited", collectionsEdited);
                    dataMap.put("beginDate", beginDate);
                    dataMap.put("endDate", endDate);
                }
            } catch (Exception e){
                e.printStackTrace();
                dataMap.put("message", "Error: " + e.getMessage());
            } finally {
                if (damsClient != null)
                    damsClient.close();
                if (damsClientEvent != null)
                    damsClientEvent.close();
            }
        } else {
            dataMap.put("recordReleased", new HashMap<String, String>());
            dataMap.put("recordAdded", new HashMap<String, String>());
            dataMap.put("recordRemoved", new HashMap<String, String>());
            dataMap.put("recordEdited", new HashMap<String, String>());
            dataMap.put("beginDate", "");
            dataMap.put("endDate", "");
        }
        return new ModelAndView("collectionStatusReport", "model", dataMap);
    }

    public static List<String> getValues(Document doc, String xPath){
        List<String> values = new ArrayList<String>();
        List<Node> nodes = doc.selectNodes(xPath);
        for(Iterator<Node> it=nodes.iterator(); it.hasNext();)
            values.add(it.next().getText());
        return values;
    }

    /* 
     * Build SPARQL for events:
     * predicate dams:type http://library.ucsd.edu/ark:/20775/bd3106617w
     * predicate dams:eventDate http://library.ucsd.edu/ark:/20775/bd5120287c
     * @param eventType
     * @param beginDate
     * @return
     */
    public static String buildEventSparql(String eventType, String beginDate, String endDate) throws ParseException {
        String searchEndDate = endDate;
        if (StringUtils.isNotBlank(endDate)) {
            // set date range to include the whole end date
            Calendar eCal = Calendar.getInstance();
            eCal.setTime(dateFormat.parse(endDate));
            eCal.add(Calendar.DATE, 1);
            searchEndDate = dateFormat.format(eCal.getTime());
        }

        String sparql = "SELECT ?e WHERE {?e <" + DAMSClient.PREDICATE_EVENT_TYPE + "> '\""
                + eventType  + "\"' . ?e <" + DAMSClient.PREDICATE_EVENT_DATE + "> ?date";

        if (StringUtils.isNotBlank(beginDate) || StringUtils.isNotBlank(endDate)) {
            sparql += " . FILTER (" 
                            + (StringUtils.isNotBlank(beginDate) ? "?date > '\"" + beginDate + "\"'"
                                : StringUtils.isNotBlank(beginDate) && StringUtils.isNotBlank(searchEndDate) ? " && "
                                : StringUtils.isNotBlank(searchEndDate) ?  "?date < '\"" + searchEndDate + "\"'"
                                : "")
                            + ")";

        }
        sparql += "}";

        return sparql;
    }

    /* 
     * Build SPARQL for events:
     * predicate dams:event http://library.ucsd.edu/ark:/20775/bd45400748
     * @param eventType
     * @param beginDate
     * @return
     */
    private static String buildSparqlWithEventId(String eventId) {
        return "SELECT ?sub WHERE {?sub <http://library.ucsd.edu/ark:/20775/bd45400748> <" + eventId + ">}";
    }

   /*
    * lookup collections by event type
    * @param damsClient
    * @param damsClientEvent
    * @param colMap
    * @param eventType
    * @param beginDate
    * @return
    * @throws Exception
    */
    private Map<String, String> lookupCollectionsByEventType(
            DAMSClient damsClient,
            DAMSClient damsClientEvent,
            Map<String, String> colArkMap,
            String eventType,
            String beginDate,
            String endDate) throws Exception {
        String sparql = buildEventSparql(eventType, beginDate, endDate);
        List<Map<String, String>> events = damsClientEvent.sparqlLookup(sparql);
        List<String> records = lookupRecordsByEventIds(damsClient, events);
        Map<String, String> collections = new TreeMap<String, String>();
        for (String record : records) {
            Document doc = damsClient.getRecord(record);
            List<String> colIds = getValues(doc, MetadataImportController.objectClrPath);
            for (String colId : colIds) {
                if (StringUtils.isNotBlank(colId) && colArkMap.containsKey(colId) && !collections.containsKey(colId)) {
                    String colTitle = colArkMap.get(colId);
                    colId = "https://" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu/collection" + colId.substring(record.lastIndexOf("/"));
                    collections.put(colId, colTitle.substring(0, colTitle.lastIndexOf("[")).trim());
                }
            }
        }
        return collections;
    }

    /*
     * lookup records with event IDs.
     * @param damsClient
     * @param events
     * @return
     * @throws Exception
     */
    private List<String> lookupRecordsByEventIds(DAMSClient damsClient, List<Map<String, String>> events) throws Exception {
        List<String> records = new ArrayList<>();
        for (Map<String, String> event : events) {
            String sparql = buildSparqlWithEventId(event.get("e"));
            List<Map<String, String>> solutions = damsClient.sparqlLookup(sparql);
            for (Map<String, String> solution : solutions) {
                String record = solution.get("sub");
                if (record.startsWith("http://") && !records.contains(record))
                    records.add(record);
            }
        }
        return records;
    }

    /*
     * build the collection map for UI display
     * @param colArkMap
     * @param records
     * @return
     */
    private Map<String, String> buildCollections(Map<String, String> colArkMap, List<String> records) {
        Map<String, String> results = new TreeMap<String, String>();
        for (String record : records) {
            if (colArkMap.containsKey(record)) {
                String colTitle = colArkMap.get(record);
                String colId = "https://" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu/collection" + record.substring(record.lastIndexOf("/"));
                results.put(colId, colTitle.substring(0, colTitle.lastIndexOf("[")).trim());
            }
        }
        return results;
    }

    /*
     * Build report in CSV format
     * @param reportBuilder
     * @param reportTitle
     * @param collections
     */
    private void addCsvReport(StringBuilder reportBuilder, String reportTitle, Map<String, String> collections) {
        if (collections.size() > 0) {
            reportBuilder.append(Statistics.escapeCsv(reportTitle) + "\n");
            for (String ark : collections.keySet()) {
                reportBuilder.append(Statistics.escapeCsv(ark) + "," + Statistics.escapeCsv(collections.get(ark)) + "\n");
            }
            reportBuilder.append("\n");
        }
    }
}
