package edu.ucsd.library.xdre.harvesting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.ucsd.library.xdre.tab.ExcelSource;
import edu.ucsd.library.xdre.tab.RDFExcelConvertor;
import edu.ucsd.library.xdre.tab.Record;
import edu.ucsd.library.xdre.tab.RecordSource;
import edu.ucsd.library.xdre.tab.TabularRecord;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * CIL to support CIL Harvesting: extract metadata from JSON source
 * and convert to DAMS4 RDF/XML and CSV format with Excel inputStream headings
 * @author lsitu
 */
public class CilHarvesting implements RecordSource {
    private static Logger log = Logger.getLogger(CilHarvesting.class);

    private static final String VALUE_DELIMITER = "; ";
    private static SimpleDateFormat YEAR_FORMATTER = new SimpleDateFormat("yyyy");
    private static final String CIL_TEXT = "CIL";
    private static final String COPYRIGHT_TEXT = "copyright";

    private static String[] SUBJECT_HEADINGS = {"subject", "person", "corporate"};

    private static final String SOURCE_ONTO_NAME_SUBFFIX = ".onto_name";
    private static final String SOURCE_FREE_TEXT_SUBFFIX = ".free_text";
    private static final String SOURCE_ONTO_ID_SUBFFIX = ".onto_id";

    private static final String[] TECHNICAL_DETAILS_NOTE_KEYS = {
        "CIL_CCDB.CIL.CORE.PREPARATION",
        "CIL_CCDB.CIL.CORE.RELATIONTOINTACTCELL",
        "CIL_CCDB.CIL.CORE.ITEMTYPE",
        "CIL_CCDB.CIL.CORE.IMAGINGMODE",
        "CIL_CCDB.CIL.CORE.PARAMETERIMAGED",
        "CIL_CCDB.CIL.CORE.SOURCEOFCONTRAST",
        "CIL_CCDB.CIL.CORE.VISUALIZATIONMETHODS",
        "CIL_CCDB.CIL.CORE.PROCESSINGHISTORY",
        "CIL_CCDB.CIL.CORE.DATAQUALIFICATION"
    };

    private Map<String, List<String>> fieldMappings = null;
    private Map<String, String> constantFields = null;
    private List<String> objectsFailed = new ArrayList<>();
    private StringBuilder errors = new StringBuilder();
    private Iterator<String> sourceFileIterator = null;

    private Map<String, String> subjectHeadings = null;

    public CilHarvesting(String srcMappingFile, Map<String, String> constantFields,
            List<String> srcFiles) throws Exception {
        this.constantFields = constantFields;
        this.sourceFileIterator = srcFiles.listIterator();
        initiateFieldMappings(srcMappingFile);
        subjectHeadings = new HashMap<>();
    }

    public CilHarvesting(Map<String, List<String>> fieldMappings, Map<String, String> constantFields,
            List<String> srcFiles) throws Exception {
        this.constantFields = constantFields;
        this.sourceFileIterator = srcFiles.listIterator();
        this.fieldMappings = fieldMappings;
        subjectHeadings = new HashMap<>();
    }

    public Record nextRecord() throws Exception {
        Map<String, String> data = new HashMap<>();
        String[] technicalDetailsNotes = new String[TECHNICAL_DETAILS_NOTE_KEYS.length];

        // add constant ingest fields: ensure lower case keys for Excel InputStream
        if (constantFields != null) {
            for (String key : constantFields.keySet()) {
                data.put(key.toLowerCase(), constantFields.get(key));
            }
        }

        if (!sourceFileIterator.hasNext())
            return null;

        String srcFile = sourceFileIterator.next();

        File jsonFile = new File(srcFile);

        // object id from file name
        String fileName = jsonFile.getName();
        String objId = fileName.substring(0, fileName.indexOf("."));
        data.put(TabularRecord.OBJECT_ID, objId);
        data.put(TabularRecord.OBJECT_COMPONENT_TYPE, "object");
        data.put(FieldMappings.IDENTIFIER_SAMPLENUMBER.toLowerCase(), objId);

        TabularRecord record = null;
        try (InputStreamReader srcIn = new InputStreamReader(new FileInputStream(srcFile))){

            // parse JSON data files
            JSONObject json = (JSONObject) JSONValue.parse(srcIn);
            record = new TabularRecord();
            record.setData(data);

            String currJsonPath = "";

            convertJsonData(record, fieldMappings, currJsonPath, null, json, data, technicalDetailsNotes);

            // generate technical details note
            buildTechnicalDetailsNote(data, technicalDetailsNotes);

            addSourceJsonComponent(record, fileName);

        } catch (Exception ex) {
            ex.printStackTrace();
            objectsFailed.add(srcFile);
            errors.append(srcFile + ": " + ex.getMessage());
        } finally {
            // If field CIL_CCDB.Citation.Title is missing, use Object Unique ID
            if (StringUtils.isBlank(data.get(FieldMappings.TITLE))) {
                data.put(FieldMappings.TITLE, objId);
                data.put(FieldMappings.NOTE_PREFERRED_CITATION.toLowerCase(), objId);
            }

            // collect subject headings for export
            collectSubjectHeadings(data);
        }

        if (record != null) {
            // Ignore copyrighted records (copyrightStatus: copyright)
            String copyrightStatus = record.getData().get(FieldMappings.COPYRIGHT_STATUS.toLowerCase());
            if (StringUtils.isNotBlank(copyrightStatus)) {
                if (copyrightStatus.equalsIgnoreCase(COPYRIGHT_TEXT)) {
                    return nextRecord();
                }
            }
        }

        return record;
    }

    /**
     * Build technical details note
     * @param data
     * @param notes
     */
    private void buildTechnicalDetailsNote(Map<String, String> data, String[] notes) {
        String noteValue = "";
        for (String note : notes) {
            if (StringUtils.isNotBlank(note)) {
                noteValue += (noteValue.length() > 0 ? "\r\n"  : "") + note;
            }
        }

        if (noteValue.length() > 0) {
            data.put(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase(), noteValue);
        }
    }

    /**
     * Collect subject headings in the json record
     * @param data
     */
    private void collectSubjectHeadings(Map<String, String> data) {
        Set<String> keys = new HashSet<>(data.keySet());

        for (String key : keys) {
            if (subjectHeading(key)) {
                if (subjectHeadings.containsKey(key)) {
                    String newValues = data.get(key);
                    String existingValues = subjectHeadings.get(key);
                    List<String> existingSubjects = Arrays.asList(existingValues.split("\\" + TabularRecord.DELIMITER));
                    for (String subject : newValues.split("\\" + TabularRecord.DELIMITER)) {
                        if (!existingSubjects.contains(subject)) {
                            existingValues += TabularRecord.DELIMITER + subject;
                        }
                    }
                    subjectHeadings.put(key, existingValues);
                } else {
                    subjectHeadings.put(key, data.get(key));
                }

                // Merge subject headings w/o closeMatch, which is exported in subject headings.
                String ingestField = key;
                if (key.contains("" + TabularRecord.DELIMITER)) {
                    String values = data.remove(key);
                    ingestField = key.substring(0, key.lastIndexOf(TabularRecord.DELIMITER));
                    if (data.containsKey(ingestField)) {
                        values = data.get(ingestField) + TabularRecord.DELIMITER + values;
                    }

                    data.put(ingestField, values);
                }
            }
        }
    }

    /**
     * Detect a subject heading for authority records
     * @param header
     * @return
     */
    private boolean subjectHeading(String header) {
        for (String subject : SUBJECT_HEADINGS) {
            if (header.toLowerCase().startsWith(subject)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get subject headings data map
     * @return
     */
    public Map<String, String> getSubjectHeadings() {
        return subjectHeadings;
    }

    /**
     * Get the subject headings in CSV format
     * @return
     */
    public String getSubjectHeadingsCsv() {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("subject type,exactMatch,closeMatch,subject term\n");
        for (String key : subjectHeadings.keySet()) {
            String[] values = subjectHeadings.get(key).split("\\" + TabularRecord.DELIMITER);

            // handle subject headings with closeMatch
            String origKey = key;
            String closeMatch = "";
            if (key.contains("" + TabularRecord.DELIMITER)) {
                key = origKey.substring(0, origKey.lastIndexOf(TabularRecord.DELIMITER));
                closeMatch = origKey.substring(origKey.lastIndexOf(TabularRecord.DELIMITER) + 1);
            }

            String header = capitalized(key);
            for (String value : values) {
                csvBuilder.append(RDFExcelConvertor.escapeCsv(header));
                csvBuilder.append(",,");

                if (StringUtils.isNotBlank(closeMatch)) {
                    csvBuilder.append(closeMatch);
                }

                csvBuilder.append(",");
                csvBuilder.append(RDFExcelConvertor.escapeCsv(value));
                csvBuilder.append("\n");
            }
        }
        return csvBuilder.toString();
    }

    /**
     * Capitalized the value
     * @param value
     * @return
     */
    public static String capitalized(String value) {
        if (StringUtils.isBlank(value))
            return value;

        return value.substring(0, 1).toUpperCase()
                + (value.length() > 1 ? value.substring(1) : "");
    }

    /*
     * Extract metadata from JSON data source
     * @param record
     * @param fieldsMap
     * @param path
     * @param jsonData
     * @param data
     * @param technicalDetailsNotes
     */
    private void convertJsonData(TabularRecord record, Map<String, List<String>> fieldsMap,
            String path, JSONObject parentData, JSONObject jsonData, Map<String, String> data, String[] technicalDetailsNotes) {
        Iterator<Object> keys = jsonData.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            String currPath = (path.length() > 0 ? path + "." : "") + key;

            if (fieldsMap.containsKey(currPath.toLowerCase())) {

                List<String> ingestFields = fieldsMap.get(currPath.toLowerCase());
                List<String> vals = getFieldValues(parentData, jsonData, currPath, key,
                        ingestFields.get(0), technicalDetailsNotes);

                // skip when there are no values presented
                if ((vals == null || vals.size() == 0) && ingestFields.size() == 1)
                    continue;

                if (currPath.equalsIgnoreCase(FieldMappings.SOURCE_ATTRIBUTION_DTAE)) {
                    // dams:dateCreated, Begin date, End date
                    for (String datePart : ingestFields) {
                        // add date:creation, Begin date and End date
                        addDataField(data, currPath, datePart, vals.get(0));
                    }
                } else if (currPath.equalsIgnoreCase(FieldMappings.SOURCE_IMAGE_FILE_TYPE)) {
                    // image file type: map to File use and component Title
                    for (String field : ingestFields) {
                        addDataField(data, currPath, field, vals.get(0));
                    }
                } else if (currPath.equalsIgnoreCase(FieldMappings.SOURCE_ALTERNATIVE_IMAGE_FILE_PATH)) {
                    // Alternative image file path: map to File name and component Title
                    for (String field : ingestFields) {
                        addDataField(data, currPath, field, vals.get(0));
                    }
                } else {
                    int idx = 0;

                    // map to the first ingest field, then other fields if a source field is mapping to more than one field
                    do {
                        String mValue = "";
                        String ingestField = ingestFields.get(idx);

                        if (ingestField.equalsIgnoreCase(FieldMappings.PERSON_RESEARCHER)) {
                            // Special process name researchers
                            vals = handleNameResearcher(vals);
                        }

                        for (String val : vals) {
                            // handle multiple values
                            mValue += (mValue.length() > 0 ? TabularRecord.DELIMITER : "") + val;
                        }

                        if (mValue.length() > 0) {
                            // extract closeMatch for subject headings
                            String closeMatch = null;
                            if (ingestField.toLowerCase().startsWith("subject:") && currPath.endsWith(SOURCE_ONTO_NAME_SUBFFIX)) {
                                String closeMatchPath = currPath.replace(SOURCE_ONTO_NAME_SUBFFIX, SOURCE_ONTO_ID_SUBFFIX);
                                List<String> closeMatchs = getFieldValues(parentData, jsonData, closeMatchPath,
                                        SOURCE_ONTO_ID_SUBFFIX.substring(1), ingestField, technicalDetailsNotes);
                                if (closeMatchs != null && closeMatchs.size() > 0)
                                    closeMatch = closeMatchs.get(0);
                            }

                            // multiple values mapping field
                            addDataField(data, currPath, ingestField, mValue, closeMatch);
                        }

                        // handle the mapping of a source field to multiple ingest fields
                        if (++idx < ingestFields.size()) {
                            vals = getFieldValues(parentData, jsonData, currPath, key, ingestFields.get(idx), technicalDetailsNotes);
                        }
                    } while(idx < ingestFields.size());
                }
            } else if (currPath.equalsIgnoreCase(FieldMappings.SOURCE_ATTRIBUTION_URLS)) {
                // Mapping for Related resource:related
                List<JSONObject> relatedList = getJsonSource(jsonData.get(key));

                for (JSONObject related : relatedList) {
                    convertJsonData(record, fieldMappings, currPath, jsonData, related, data, technicalDetailsNotes);
                }
            } else if (currPath.equalsIgnoreCase(FieldMappings.SOURCE_IMAGE_FILES)
                        || currPath.equalsIgnoreCase(FieldMappings.SOURCE_ALTERNATIVE_IMAGE_FILES)) {
                // mapping for components
                List<JSONObject> compJsonList = getJsonSource(jsonData.get(key));
                for (JSONObject compJson : compJsonList) {
                    Map<String, String> compData = new HashMap<>();
                    TabularRecord component = new TabularRecord();
                    component.setData(compData);
                    record.addComponent(component);
                    convertJsonData(component, fieldMappings, currPath, jsonData, compJson, compData, new String[TECHNICAL_DETAILS_NOTE_KEYS.length]);

                    compData.put(TabularRecord.OBJECT_ID, record.getData().get(TabularRecord.OBJECT_ID));
                    compData.put(TabularRecord.OBJECT_COMPONENT_TYPE, "Component");
                }

            } else {
                Object val = jsonData.get(key);
                if (val instanceof JSONObject) {
                    convertJsonData(record, fieldMappings, currPath, jsonData, (JSONObject)val, data, technicalDetailsNotes);
                } else if (val instanceof JSONArray) {
                    // JSONObject in JSONArray
                    Iterator<JSONArray> it = ((JSONArray)val).iterator();
                    while (it.hasNext()) {
                        Object obj = it.next();
                        if (obj instanceof JSONObject) {
                            convertJsonData(record, fieldMappings, currPath, jsonData, (JSONObject)obj, data, technicalDetailsNotes);
                        } else {
                            log.warn("No mapping for key " + key + " in source record " + record.getData().get(TabularRecord.OBJECT_ID) + ".");
                        }
                    }
                } else {
                    log.warn("No mapping for key " + key + " in source record " + record.getData().get(TabularRecord.OBJECT_ID) + ".");
                }
            }
        }
    }

    /*
     * Restructure JSON source data in List for processing
     * @param json
     * @return
     */
    private List<JSONObject> getJsonSource(Object json) {
        List<JSONObject> jsonList = new ArrayList<>();
        if (json instanceof JSONObject) {
            jsonList.add((JSONObject)json);
        } else if (json instanceof JSONArray) {
            Iterator<Object> it = ((JSONArray)json).iterator();
            while (it.hasNext()) {
                jsonList.add((JSONObject)it.next());
            }
        }
        return jsonList;
    }

    public Map<String, List<String>> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(Map<String, List<String>> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    /*
     * Initiate mapping for json source field and ingest field for Excel InputStream heading
     * @param mappingFile
     */
    private void initiateFieldMappings(String mappingFile) throws Exception {
        FieldMappings mappings = new FieldMappings(new File(mappingFile));
        fieldMappings = mappings.getFieldMappings();
        constantFields = mappings.getConstantFields();
    }

    /*
     * Add converted dams4 fields/metadata with Excel InpuStream heading
     * @param data
     * @param srcPath
     * @param ingestField
     * @param value
     */
    private void addDataField(Map<String, String> data, String srcPath, String ingestField, String value) {
        addDataField(data, srcPath, ingestField, value, null);
    }

    /*
     * Add converted dams4 fields/metadata with Excel InpuStream heading with closeMatch
     * @param data
     * @param srcPath
     * @param ingestField
     * @param value
     * @param closeMatch
     */
    private void addDataField(Map<String, String> data, String srcPath, String ingestField,
            String value, String closeMatch) {
        String valConverted = convertValue(srcPath, ingestField, value);

        String headerKey = ingestField.toLowerCase();
        if (StringUtils.isNotBlank(closeMatch)) {
            headerKey += TabularRecord.DELIMITER + closeMatch;
        }

        String existing = data.get(headerKey);
        if (StringUtils.isNotBlank(existing)) {
            existing += TabularRecord.DELIMITER + valConverted;
        } else {
            existing = valConverted;
        }

        data.put(headerKey, existing);
    }

    /*
     * Convert field value according to the mapping for harvesting
     * @param field
     * @param value
     * @return
     */
    private String convertValue(String srcPath, String ingestField, String value) {
        String val = value;
        if (ingestField.equalsIgnoreCase(FieldMappings.TITLE)
                && !srcPath.equalsIgnoreCase(FieldMappings.SOURCE_IMAGE_FILE_TYPE)) {
            // Extract title from citation title
            val = extractTitle(value);
        } else if (ingestField.equalsIgnoreCase("date:creation")
                || ingestField.equalsIgnoreCase(ExcelSource.BEGIN_DATE)
                || ingestField.equalsIgnoreCase(ExcelSource.END_DATE)) {
            // Format Begin date and End date from date value: yyyy-MM-dd
            val = formatDate(val);
        } else if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_GROUP_ID)) {
            // "Cell Image Library Group ID: " GROUP_ID
            val = "Cell Image Library Group ID: " + val;
        } else if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_ATTRIBUTION_PUBMED)) {
            // Format Related Publications note
            val = formatRelatedPublications(val);
        } else if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_CITATION_DOI)) {
            // "Source Record in the Cell Image Library @ https://doi.org/"Citation.DOI
            val = "Source Record in the Cell Image Library @ https://doi.org/" + val.replace("doi:", "").trim();
        } else if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_CITATION_TITLE)
                && ingestField.equalsIgnoreCase(FieldMappings.NOTE_PREFERRED_CITATION)) {
            // Format Note:preferred citation from source citation title
            val = formatCitationTitle(val);
        } else if (ingestField.equalsIgnoreCase(FieldMappings.FILE_USE)) {
            // Extract File use from source file type
            val = getFileUseFromType(val) ;
        } else if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_IMAGE_FILE_TYPE)
                && ingestField.equalsIgnoreCase(FieldMappings.TITLE)) {
            val = val + " format";
        }

        return val;
    }

    /*
     * Extract title from json source citation title
     * title: "Trim CIL_CCDB.Citation.Title removing all text up to the first occurrence of "CIL"
     * and removing the second occurrence of ""CIL"" and all text after it
     * e.g. Sanford Palay (2011) CIL:10790, Rattus, brush border epithelial cell. CIL. Dataset"
     * @param value
     * @return
     */
    private String extractTitle(String val) {
        String title = val;
        int firstIdx = val.indexOf(CIL_TEXT);
        int secondIdx = val.indexOf(CIL_TEXT, firstIdx + 1);
        if (firstIdx >= 0 && secondIdx >= 0) {
            title = val.substring(firstIdx, secondIdx).trim();
        }
        return title;
    }

    /*
     * Format date value (MM/dd/yyyy) for Begin date and End date
     * Date format: yyyy-MM-dd
     * @param val
     * @return
     */
    private String formatDate(String val) {
        String[] dateParts= val.replace("00/", "").split("\\/");
        String yearPart = dateParts[dateParts.length - 1];
        return yearPart + (dateParts.length == 2 ? "-" + dateParts[0] : dateParts.length == 3 ? 
                "-" + dateParts[0] + "-" + dateParts[1] : "");
    }

    /*
     * Format Note:preferred citation from source citation title
     * "CIL_CCDB.CIL.Citation.Title Replace YYYY value in "(YYYY)" with current year.
     * Replace text "CIL. Dataset" with "In Cell Image Library. UC San Diego Library Digital Collections. Dataset."
     * E.g. Sanford Palay (20112018) CIL:10790, Rattus, brush border epithelial cell. CIL. Dataset In Cell Image Library.
     *  UC San Diego Library Digital Collections. Dataset."
     * @param val
     * @return
     */
    private String formatCitationTitle(String val) {
        String year = "" + Calendar.getInstance().get(Calendar.YEAR);
        int idxYearStart = val.indexOf("(");
        int idxYearEnd = val.indexOf(")", idxYearStart);
        if (idxYearEnd > idxYearStart) {
            String yearValue = val.substring(idxYearStart + 1, idxYearEnd);
            try {
                YEAR_FORMATTER.parse(yearValue);
                val = val.replace("(" + yearValue + ")", "(" + year + ")");
            } catch (ParseException e) {
            }
        }
        return val.replace("CIL. Dataset", "In Cell Image Library. UC San Diego Library Digital Collections. Dataset.");
    }

    /*
     * Process Person:researcher with special rules:
     * remove expression text
     * remove parentheses and contents
     * split combined names by semicolon
     * @param val
     * @return
     */
    private List<String> handleNameResearcher(List<String> names) {
        List<String> results = new ArrayList<>();

        int i = 0;
        for (String name : names) {
            boolean ignore = false;

            if ( i > 1) {
                // remove name followed by regular text pattern
                if (name.endsWith("®")) {
                    ignore = true;
                } else {
                    String[] words = name.split(" ");
                    try {
                        Integer.parseInt(words[0]);
                        ignore = true;
                    } catch (NumberFormatException e) { }
                }
            }

            if (!ignore) {
                // remove parentheses and contents
                name = removeParentheses(name);

                // split combined names delimited by semicolon
                String[] combinedNames = name.split("\\;");
                for (String n : combinedNames) {
                    results.add(n.trim());
                }
            }

            i++;
        }

        return results;
    }

    /*
     * Remove parentheses and contents inside parentheses
     * @param value
     * @return
     */
    private String removeParentheses(String value) {
        int idxOrgStart = value.indexOf("(");
        int idxOrgEnd = value.indexOf(")", idxOrgStart);
        while (idxOrgStart >= 0 && idxOrgEnd > idxOrgStart) {
            value = value.substring(0, idxOrgStart).trim() + " " + value.substring(idxOrgEnd + 1).trim();
            idxOrgStart = value.indexOf("(");
            idxOrgEnd = value.indexOf(")", idxOrgStart);
        }
        return value;
    }

    /*
     * Format note Related Publications
     * "IF value has "PMID: " before numerals, remove "PMID:", THEN
     * "PubMed ID: https://www.ncbi.nlm.nih.gov/pubmed/?term="ATTRIBUTION.PUBMED"
     * @param val
     * @return
     */
    private String formatRelatedPublications(String val) {
        int idx = val.indexOf("PMID:");
        if (idx >= 0) {
            String pmid = val.substring(idx + 6).trim();
            if (Character.isDigit(pmid.charAt(0))) {
            val = pmid;
            }
        }
        return "PubMed ID: https://www.ncbi.nlm.nih.gov/pubmed/?term=" + val;
    }

    /*
     * Extract File use from source file type
     * if value = Jpeg|OME_tif THEN image-source
     * if value = Gzip|Zip|Mrc THEN data-service
     * if value = Flv|mp4 THEN video-source - The old Flv format is removed from the mapping instruction in the new version
     * but it's not bad to keep and check it around.
     * @param fileType
     * @return
     */
    private String getFileUseFromType(String fileType) {
        String fileUse = "";
        if (fileType.equalsIgnoreCase("Jpeg") || fileType.equalsIgnoreCase("OME_tif")) {
            fileUse = "image-source";
        } else if (fileType.equalsIgnoreCase("Gzip") || fileType.equalsIgnoreCase("Zip") || fileType.equalsIgnoreCase("Mrc")) {
            fileUse = "data-service";
        } else if (fileType.equalsIgnoreCase("Flv") || fileType.equalsIgnoreCase("mp4")) {
            fileUse = "video-source";
        }
        return fileUse;
    }

    /*
     * Retrieve values by source field and format the raw data when needed
     * @param data
     * @param srcField
     * @return
     */
    private List<String> getFieldValues(JSONObject parentData, JSONObject data, String srcPath, String fieldName,
            String ingestField, String[] technicalDetailsNotes) {
        List<String> results = new ArrayList<>();
        if (ingestField.equalsIgnoreCase(FieldMappings.NOTE_TECHNICAL_DETAILS)
                && (srcPath.endsWith(SOURCE_ONTO_NAME_SUBFFIX) || srcPath.endsWith(SOURCE_FREE_TEXT_SUBFFIX))) {
            // extract note Technical details
            String value = "";
            String[] tokens = srcPath.split("\\.");
            String prefix = tokens[tokens.length - 2];
            String technicalDetailskey = srcPath.substring(0, srcPath.lastIndexOf("."));

            int idx = Arrays.asList(TECHNICAL_DETAILS_NOTE_KEYS).indexOf(technicalDetailskey);
            if (StringUtils.isNotBlank(technicalDetailsNotes[idx])) {
                // skip when the technical details note with the same prefix is processed for
                // onto_name and free_text
                return results;
            }

            Object noteData = parentData.get(prefix);
            JSONArray noteDataArr = ensureJsonArray(noteData);

            String notePrefix = convertKeyToPrefix(prefix);
            value = notePrefix + ": " + formatTechnicalDetailsNote(noteDataArr, srcPath, fieldName);

            // store value for technical details notes for now
            technicalDetailsNotes[idx] = value;
        } else if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_ATTRIBUTION_URLS_LABEL)
                || srcPath.equalsIgnoreCase(FieldMappings.SOURCE_ATTRIBUTION_URLS_HREF)) {
            // extract Related resource related type
            if (srcPath.equalsIgnoreCase(FieldMappings.SOURCE_ATTRIBUTION_URLS_HREF)) {
                List<String> href = getDataByFieldName(data, FieldMappings.SOURCE_ATTRIBUTION_URLS_HREF, fieldName);
                List<String> labels = getDataByFieldName(data, FieldMappings.SOURCE_ATTRIBUTION_URLS_LABEL, "Label");
                for(int i = 0; i < href.size(); i++) {
                    if (labels.size() > 0 && i < labels.size()) {
                        results.add(labels.get(i) + " @ " + href.get(i));
                    } else {
                        results.add("Related resource @ " + href.get(i));
                    }
                }
            }
        } else {
            results.addAll(getDataByFieldName(data, srcPath, fieldName));
        }

        return results;
    }

    /*
     * Wrap JSONObject to JSONArray
     * @param jsonData
     * @return
     */
    private JSONArray ensureJsonArray(Object jsonData) {
        JSONArray jsonDataArr = new JSONArray();
        if (jsonData instanceof JSONArray) {
            jsonDataArr.addAll((JSONArray)jsonData);
        } else {
            jsonDataArr.add(jsonData);
        }

        return jsonDataArr;
    }

    /*
     * Format technical details note: onto_name; free_text
     * @param data
     * @param srcField
     * @param values
     * @return
     */
    private String formatTechnicalDetailsNote(JSONArray data, String srcPath, String fieldName) {
        String value1 = "";
        String value2 = "";
        String srcField2 = "";
        Iterator<Object> it = data.iterator();
        while (it.hasNext()) {
            JSONObject jsonObj = (JSONObject)it.next();

            String valueStr = concatValue(getDataByFieldName(jsonObj, srcPath, fieldName), VALUE_DELIMITER);
            if (StringUtils.isNotBlank(valueStr)) {
                value1 += (value1.length() > 0 ? VALUE_DELIMITER : "") + valueStr;
            }

            if (srcPath.endsWith(SOURCE_ONTO_NAME_SUBFFIX)) {
                srcField2 = srcPath.replace(SOURCE_ONTO_NAME_SUBFFIX, SOURCE_FREE_TEXT_SUBFFIX);
            } else {
                srcField2 = srcPath.replace(SOURCE_FREE_TEXT_SUBFFIX, SOURCE_ONTO_NAME_SUBFFIX);
            }

            valueStr = concatValue(getDataByFieldName(jsonObj, srcField2, srcField2.substring(srcField2.lastIndexOf(".") + 1)), VALUE_DELIMITER);
            if (StringUtils.isNotBlank(valueStr)) {
                value2 += (value2.length() > 0 ? VALUE_DELIMITER : "") + valueStr;
            }
        }

        if (srcPath.endsWith(SOURCE_ONTO_NAME_SUBFFIX)) {
            return value1 + (value1.length() > 0 && value2.length() > 0 ? VALUE_DELIMITER : "") + value2;
        } else {
            return value2 + (value1.length() > 0 && value2.length() > 0 ? VALUE_DELIMITER : "") + value1;
        }
    }

    /*
     * Add the source json file as component
     * @param record
     * @param fileName
     */
    private void addSourceJsonComponent(TabularRecord record, String fileName) {
        TabularRecord component = new TabularRecord();
        Map<String, String> compData = new HashMap<>();
        component.setData(compData);
        record.addComponent(component);

        compData.put(TabularRecord.OBJECT_ID, record.getData().get(TabularRecord.OBJECT_ID));
        compData.put(TabularRecord.OBJECT_COMPONENT_TYPE, "Component");
        compData.put(FieldMappings.FILE_NAME.toLowerCase(), fileName);
        compData.put(FieldMappings.FILE_USE.toLowerCase(), "data-service");
        compData.put(FieldMappings.TITLE.toLowerCase(), "CIL source metadata (JSON)");
    }

    /*
     * Concatenate JSONArray values into string value
     * @param values
     * @param delimiter
     * @return
     */
    private String concatValue(List<String> values, String delimiter) {
        String value = "";
        for (String v : values) {
            value += (value.length() > 0 ? delimiter : "") + v;
        }

        return value;
    }

    /*
     * Get value from JSON with a field/key
     * @param data
     * @param srcPath
     * @param fieldName
     * @return
     */
    private List<String> getDataByFieldName(JSONObject data, String srcPath, String fieldName) {
        List<String> result = new ArrayList<>();
        Object v = data.get(fieldName);
        if (v != null && v.toString().length() > 0) {
            if (!(v instanceof JSONArray)) {
                result.add(v.toString());
            } else {
                Iterator<Object> it = ((JSONArray)v).iterator();
                while (it.hasNext()) {
                    result.add(it.next().toString());
                }
            }
        }

        return result;
    }

    /*
     * Convert json key to the prefix for note technical details
     * @param key
     * @return
     */
    private String convertKeyToPrefix(String key) {
        String value = key;
        switch(key.toUpperCase()) {
            case FieldMappings.SOURCE_KEY_RELATIONTOINTACTCELL:
                value = "Relation to intact cell";
                break;
            case "ITEMTYPE":
                value = "Item type";
                break;
            case "IMAGINGMODE":
                value = "Imaging mode";
                break;
            case "PARAMETERIMAGED":
                value = "Parameter imaged";
                break;
            case "SOURCEOFCONTRAST":
                value = "Source of contrast";
                break;
            case "VISUALIZATIONMETHODS":
                value = "Visualization methods";
                break;
            case "PROCESSINGHISTORY":
                value = "Processing history";
                break;
            case "DATAQUALIFICATION":
                value = "Data qualification";
                break;
            default:
                value = key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
                break;
        }

        return value;
    }

    /**
     * Convert to CSV format for Excel InputStream
     * @param jsonConvertSxlFile
     * @return
     * @throws Exception
     */
    public String toCSV(String jsonConvertSxlFile) throws Exception {
        try (InputStream jsonConvertXslInput = new FileInputStream(jsonConvertSxlFile);) {
            return toCSV(jsonConvertXslInput);
        }
    }


    /**
     * Convert to CSV format for Excel InputStream
     * @param jsonConvertSxlFile
     * @return
     * @throws Exception
     */
    public String toCSV(InputStream jsonConvertXslInput) throws Exception {
        Record rec = nextRecord();
        String recordId = rec.recordID();
        Document rdfDoc = rec.toRDFXML();
        Element root = rdfDoc.getRootElement();
        // loop through to include all objects
        while(rec != null) {
            if (!rec.recordID().equals(recordId)) {
                root.add(rec.toRDFXML().selectSingleNode("//dams:Object").detach());
            }
            rec = nextRecord();
        }

        String rdfFileName = "cil_metadata_processed-" + new SimpleDateFormat("yyyyMMddHHmm")
                .format(Calendar.getInstance().getTime());
        File rdfFile = writeRdfContent(rdfFileName, rdfDoc.asXML());
        RDFExcelConvertor converter = new RDFExcelConvertor(rdfFile.getAbsolutePath(), jsonConvertXslInput);
        return converter.convert2CSV();
    }

    /**
     * Write converted RDF/XML to file
     * @param recordId
     * @param rdf
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private File writeRdfContent(String recordId, String rdf) throws FileNotFoundException, IOException {
        byte[] buf = new byte[4096];
        File destFile = new File(Constants.TMP_FILE_DIR + recordId + "-rdf.xml");

        try (InputStream in = new ByteArrayInputStream(rdf.getBytes("UTF-8"));
                OutputStream out = new FileOutputStream(destFile)) {
            int bytesRead = 0;
            while((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }
        return destFile;
    }
}
