package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;

import com.hp.hpl.jena.rdf.model.Resource;

import edu.ucsd.library.xdre.tab.RDFExcelConvertor;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.RDFStore;

/**
 * 
 * BatchExportHandler: export metadata in batch
 * @author lsitu@ucsd.edu
 */
public class BatchExportHandler extends MetadataExportHandler {
    private static Logger log = Logger.getLogger(BatchExportHandler.class);

    public static String DAMS42JSON_EXPORT_XSL_FILE = "/resources/dams42json-export.xsl";

    private int count = 0;
    private int failedCount = 0;
    private RDFStore rdfStore = null;
    private String format = null;
    private String fileUri = null;
    private OutputStream out = null;

    /**
     * Constructor for MetadataExportHandler
     * @param damsClient
     * @param collectionId
     * @param predicates
     * @param components
     * @throws Exception
     */
    public BatchExportHandler(DAMSClient damsClient, String collectionId, String format,boolean components,
            OutputStream out) throws Exception{
        super(damsClient, null);
        this.format = format;
        this.out = out;
        this.components = components;
        initHandler(collectionId);
    }

    private void initHandler(String collectionId) throws Exception{
        rdfStore = new RDFStore();
        items = new ArrayList<>();

        // handling items in the collection list
        if (StringUtils.isNotBlank(collectionId)) {
            String[] collections = collectionId.split("\\,");
            for (int i = 0; i< collections.length; i++) {
                int idx = collections[i].lastIndexOf("/");
                if (collections[i] != null && collections[i].trim().length() > 0) {
                    List<String> objs = damsClient.listObjects(collections[i].substring(idx + 1).trim());
                    items.addAll(objs);
                }
            }
        }
        
    }

    public void addItems(List<String> items) {
        this.items.addAll(items);
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    /**
     * Procedure to populate the RDF metadata
     */
    public boolean execute() throws Exception {

        String subjectId = null;
        RDFStore iStore = null;
        Resource res = null;
        itemsCount = items.size();
        for (int i = 0; i < itemsCount; i++) {
            count++;
            subjectId = items.get(i);
            try {
                setStatus("Processing export for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
                iStore =  new RDFStore();
                if (format.equalsIgnoreCase("csv") || format.equalsIgnoreCase("excel")) {
                    iStore.loadRDFXML(damsClient.getFullRecord(subjectId, true).asXML());
                } else {
                    iStore.loadRDFXML(damsClient.getMetadata(subjectId, "xml"));
                }

                // exclude components from export
                if (!components) {
                    trimStatements(iStore);
                    // Merge the resources
                    List<Resource> resIds = iStore.listURIResources();
                    for(Iterator<Resource> it=resIds.iterator(); it.hasNext();){
                        res = it.next();
                        rdfStore.merge(iStore.querySubject(res.getURI()));
                    }
                } else {
                    rdfStore.mergeObjects(iStore);
                }

                logMessage("Metadata exported for subject " + subjectId + ".");
            } catch (Exception e) {
                failedCount++;
                e.printStackTrace();
                logError("Metadata export failed (" +(i+1)+ " of " + itemsCount + "): " + e.getMessage());
            }
            setProgressPercentage( ((i + 1) * 100) / itemsCount);

            try{
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                failedCount++;
                logError("Metadata export interrupted: " + subjectId  + ". Error: " + e1.getMessage() + ".");
                setStatus("Canceled");
                clearSession();
                break;
            }
        }

        if (format.equalsIgnoreCase("csv") || format.equalsIgnoreCase("excel")) {
            rdfStore.write(out, "RDF/XML-ABBREV");
            File rdfFile = getRdfFile("" + submissionId);

            if (format.equalsIgnoreCase("excel")) {
                // Batch export in excel format
                File destFile = new File(Constants.TMP_FILE_DIR, "batchExport-" + submissionId + ".xlsx");
                try (InputStream jsonConvertExportXslInput = getDams42JsonExportXsl();) {
                    RDFExcelConvertor converter = new RDFExcelConvertor(rdfFile.getAbsolutePath(), jsonConvertExportXslInput);
                    Workbook workbook = converter.convert2Excel();
                    FileOutputStream out = new FileOutputStream(destFile);
                    workbook.write(out);
                    out.close();
                }
            } else {
                // Batch export in CSV format
                File destFile = new File(Constants.TMP_FILE_DIR, "batchExport-" + submissionId + ".csv");
                try (InputStream jsonConvertExportXslInput = getDams42JsonExportXsl();
                        OutputStream outDest = new FileOutputStream(destFile)) {
                    RDFExcelConvertor converter = new RDFExcelConvertor(rdfFile.getAbsolutePath(), jsonConvertExportXslInput);
                    String csvValue = converter.convert2CSV();
                    outDest.write(csvValue.getBytes("UTF-8"));
                }
            }
        } else {
            rdfStore.write(out, format);
        }
        return exeResult;
    }

    /**
     * Execution result message
     */
    public String getExeInfo() {
        exeReport.append((format.startsWith("RDF/XML")?"RDF/XML":format.toUpperCase()) + " metadata export ");
        if(exeResult)
            exeReport.append(" is ready" + (fileUri!=null?" for <a href=\"" + fileUri + "\">download</a>":"") + ":\n");
        else
            exeReport.append("failed (" + failedCount + " of " + count + " failed): \n ");    
        exeReport.append("- Total items found " + itemsCount + ". \n- Number of items processed " + count + ".");
        String exeInfo = exeReport.toString();
        logMessage(exeInfo);
        return exeInfo;
    }

    /**
     * Retrieve the dams42Json-export XSL as InputStream from provided in source code
     * @return
     * @throws FileNotFoundException
     */
    public InputStream getDams42JsonExportXsl() throws FileNotFoundException {
        return getClass().getResourceAsStream(DAMS42JSON_EXPORT_XSL_FILE);
    }

    public static File getRdfFile(String submissionId) {
        return new File(Constants.TMP_FILE_DIR, "batchExport-" + submissionId + "-rdf.xml");
    }

    /**
     * CSVl export filename
     * @param submissionId
     */
    public static File getCsvFile(String submissionId) {
        return new File(Constants.TMP_FILE_DIR, "batchExport-" + submissionId + ".csv");
    }

    /**
     * Excel export filename
     * @param submissionId
     */
    public static File getExcelFile(String submissionId) {
        return new File(Constants.TMP_FILE_DIR, "batchExport-" + submissionId + ".xlsx");
    }
}
