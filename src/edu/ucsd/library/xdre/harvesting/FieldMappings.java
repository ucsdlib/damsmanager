package edu.ucsd.library.xdre.harvesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * FieldMappings implementation that uses Apache POI to read Excel (OLE or XML)
 * files.
 * @author lsitu
**/
public class FieldMappings
{

    public static final String TITLE = "title";
    public static final String DATE_CREATION = "date:creation";
    public static final String BEGIN_DATE = "Begin date";
    public static final String END_DATE = "End date";
    public static final String DATE_ISSUED = "date:issued";
    public static final String TYPE_OF_RESOURCE = "Type of Resource";
    public static final String LANGUAGE = "Language";
    public static final String NOTE_TECHNICAL_DETAILS = "Note:technical details";
    public static final String FILE_NAME = "File name";
    public static final String FILE_USE = "File use";

    public static final String SOURCE_CIL_PREFFIX = "CIL_CCDB.CIL.";
    public static final String SOURCE_CORD_PREFFIX = "CIL_CCDB.CIL.CORE.";

    public static final String SOURCE_KEY_RELATIONTOINTACTCELL = "RELATIONTOINTACTCELL";
    public static final String SOURCE_KEY_PREPARATION = "PREPARATION";
    public static final String SOURCE_GROUP_ID = SOURCE_CORD_PREFFIX + "GROUP_ID";
    public static final String SOURCE_ATTRIBUTION_DTAE = SOURCE_CORD_PREFFIX + "ATTRIBUTION.DATE";
    public static final String SOURCE_ATTRIBUTION_PUBMED = SOURCE_CORD_PREFFIX + "ATTRIBUTION.PUBMED";
    public static final String SOURCE_CITATION_DOI = "CIL_CCDB.Citation.DOI";
    public static final String SOURCE_CITATION_TITLE = "CIL_CCDB.Citation.Title";
    public static final String SOURCE_IMAGE_FILES = SOURCE_CIL_PREFFIX + "Image_files";
    public static final String SOURCE_ALTERNATIVE_IMAGE_FILES = SOURCE_CIL_PREFFIX + "Alternative_image_files";
    public static final String SOURCE_IMAGE_FILE_PATH = SOURCE_CIL_PREFFIX + "Image_files.File_path";
    public static final String SOURCE_IMAGE_FILE_TYPE = SOURCE_CIL_PREFFIX + "Image_files.File_type";
    public static final String SOURCE_ALTERNATIVE_IMAGE_FILE_PATH = SOURCE_CIL_PREFFIX + "Alternative_Image_files.File_path";
    public static final String SOURCE_ALTERNATIVE_IMAGE_FILE_TYPE = SOURCE_CIL_PREFFIX + "Alternative_Image_files.File_type";

    public static final String SOURCE_PREPARATION_ONTO_NAME = SOURCE_CORD_PREFFIX + "PREPARATION.onto_name";
    public static final String SOURCE_RELATIONTOINTACTCELL_ONTO_NAME = SOURCE_CORD_PREFFIX + "RELATIONTOINTACTCELL.onto_name";
    public static final String SOURCE_ATTRIBUTION_URLS = SOURCE_CORD_PREFFIX + "ATTRIBUTION.URLs";
    public static final String SOURCE_ATTRIBUTION_URLS_LABEL = SOURCE_CORD_PREFFIX + "ATTRIBUTION.URLs.Label";
    public static final String SOURCE_ATTRIBUTION_URLS_HREF = SOURCE_CORD_PREFFIX + "ATTRIBUTION.URLs.Href";

    private static final String SOURCE_FIELD_HEADER = "Source / CCDB field";
    private static final String INGEST_FIELD_HEADER = "Ingest File Header";
    private static final String CONSTANT_VALUE_HEADER = "Processing and fixed values";

    protected int lastRow;
    protected int currRow;

    private Workbook book;
    private Sheet sheet;
    private List<String> headers;
    private Map<String, List<String>> fieldMappings = new HashMap<>();
    private Map<String, String> constantFields = new HashMap<>();

    /**
     * Create an ExcelSource object from an Excel file on disk.
     * @throws Exception 
    **/
    public FieldMappings( File f ) throws Exception
    {
        try(InputStream in = new FileInputStream(f)) {
            init(in);
        }
    }

    /**
     * Create an ExcelSource object from an InputStream
     * @throws Exception 
    **/
    public FieldMappings( InputStream in )
        throws Exception
    {
        init(in);
    }

    private void init(InputStream in) throws Exception {
        this.book = WorkbookFactory.create(in);

        // always use the the first sheet.
        this.sheet = book.getSheetAt(0);
        
        this.lastRow = sheet.getLastRowNum();
        if ( this.lastRow == 0 )
        {
            this.lastRow = sheet.getPhysicalNumberOfRows() - 1;
        }

        initFieldMappings();
    }

    /*
     * Initiate the mapping for json source and Excel InputStream
     * @throws Exception
     */
    private void initFieldMappings() throws Exception
    {
        // parse headers
        if ( lastRow > 0 )
        {
            headers = new ArrayList<>();
            Row firstRow = sheet.getRow(0);
            for ( int i = 0; i < firstRow.getLastCellNum(); i++ )
            {
                String header = firstRow.getCell(i).getStringCellValue();
                String lcHeader = header.trim().toLowerCase();
                headers.add( lcHeader );
            }
            currRow++;
        }

        while ( currRow < lastRow )
        {
            Map<String, String> data = parseRow( currRow++ );
            String sourceField = data.get(SOURCE_FIELD_HEADER.toLowerCase());
            String ingestField = data.get(INGEST_FIELD_HEADER.toLowerCase());
            String constantFieldValue = data.get(CONSTANT_VALUE_HEADER.toLowerCase());
            // field mapping
            if (StringUtils.isNotBlank(sourceField)) {
                String[] fields = sourceField.split("\\n");
                for (String field : Arrays.asList(fields)) {
                    List<String> existingMapping = fieldMappings.get(field);
                    if (existingMapping == null) {
                        existingMapping = new ArrayList<>();
                        fieldMappings.put(field, existingMapping);
                    }

                    existingMapping.add(ingestField);
                }
            }

            // constant field
            if (StringUtils.isBlank(sourceField) && StringUtils.isNotBlank(ingestField)) {
                if (ingestField.equalsIgnoreCase(DATE_ISSUED)) {
                    constantFieldValue = "" + Calendar.getInstance().get(Calendar.YEAR);
                    constantFields.put(ingestField, constantFieldValue);
                } else if (ingestField.equalsIgnoreCase(TYPE_OF_RESOURCE)
                        || ingestField.equalsIgnoreCase(LANGUAGE)) {
                    constantFields.put(ingestField, constantFieldValue);
                }
            }
        }
    }

    /**
     * Parse a row of data and return it as a Map
    **/
    protected Map<String,String> parseRow( int n )
    {
        Row row = sheet.getRow(n);
        Map<String,String> values = new HashMap<>();

        if (row != null) 
        {
            for ( int i = 0; i < headers.size(); i++ )
            {
                String header = headers.get(i);

                String value = null;
                if ( i < (row.getLastCellNum() + 1) ) 
                {
                    Cell cell = row.getCell(i);
                    if ( cell != null )
                    {
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        value = cell.toString();
                    }
                }
                if ( value != null && !value.trim().equals("") )
                {
                    try {
                        value = new String(value.trim().getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                values.put(header, value);
            }
        }
        return values;
    }

    /**
     * Get original case sensitive mapping for source field and ingest field
     * @return Map<String, String>
     */
    public Map<String, List<String>> getOriginalFieldMappings() {
        return fieldMappings;
    }

    /**
     * Get case insensitive mapping for source field and ingest field
     * @return Map<String, String>
     */
    public Map<String, List<String>> getFieldMappings() {
        Map<String, List<String>> caseInsensitiveMappings = new HashMap<>();
        for (String key : fieldMappings.keySet()) {
            caseInsensitiveMappings.put(key.toLowerCase(), fieldMappings.get(key));
        }
        return caseInsensitiveMappings;
    }

    /**
     * Get constant field values
     * @return
     */
    public Map<String, String> getConstantFields() {
        return constantFields;
    }

    /**
     * Set constant fields
     * @param constantFields
     */
    public void setConstantFields(Map<String, String> constantFields) {
        this.constantFields = constantFields;
    }
}
