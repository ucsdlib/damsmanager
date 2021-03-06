package edu.ucsd.library.xdre.tab;

import static edu.ucsd.library.xdre.tab.TabularRecord.OBJECT_ID;
import static edu.ucsd.library.xdre.tab.TabularRecord.DELIMITER;
import static edu.ucsd.library.xdre.tab.TabularRecord.OBJECT_COMPONENT_TYPE;
import static edu.ucsd.library.xdre.tab.TabularRecord.COMPONENT;
import static edu.ucsd.library.xdre.tab.TabularRecord.SUBCOMPONENT;
import static edu.ucsd.library.xdre.tab.TabularRecord.DELIMITER_LANG_ELEMENT;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * RecordSource implementation that uses Apache POI to read Excel (OLE or XML)
 * files.
 * @author lsitu
 * @author escowles
 * @since 2014-06-05
**/
public class ExcelSource implements RecordSource
{
    public static final String[] IGNORED_FIELDS_FOR_OBJECTS = {"CLR image file name", "Brief description", "subject type"};
    public static final String[] IGNORED_FIELDS_FOR_COLLECTIONS = {"Level","Title","Subtitle","Part name","Part number","Translation","Variant","File name","File use","File name 2","File use 2", "subject type"};
    public static final String[] RIGHTS_VALIDATION_FIELDS = {"copyrightJurisdiction", "copyrightStatus", "copyrightPurposeNote", "copyrightNote", "rightsHolderCorporate",
        "rightsHolderPersonal", "rightsHolderName", "otherRights:otherRightsBasis", "otherRights:permission/type", "otherRights:restriction/type", "otherRights:otherRightsNote",
        "license:permission/type", "license:restriction/type", "license:beginDate", "license:endDate", "license:licenseNote", "license:licenseURI"};

    private static final String[] DATETIME_FORMATS = {"yyyy", "yyyy-MM", "yyyy-MM-dd"};
    public static final String BEGIN_DATE = "Begin date";
    public static final String END_DATE = "End date";
    public static final String LICENSE_BEGIN_DATE = "license:beginDate";
    public static final String LICENSE_END_DATE = "license:endDate";

    protected static final String[] DATE_FIELDS = {BEGIN_DATE, END_DATE, LICENSE_BEGIN_DATE, LICENSE_END_DATE};
    protected static Map<String, List<String>> CONTROL_VALUES = new HashMap<>();

    protected int lastRow;
    protected int currRow;

    private Workbook book;
    private Sheet sheet;
    private List<String> headers;
    private Map<String, String> originalHeaders = new HashMap<>();
    private List<String> invalidHeaders = new ArrayList<>();
    private List<Map<String, String>> invalidValues = new ArrayList<>();
    private List<Map<String, String>> controlCharValues = new ArrayList<>();
    private List<String> controlFields = new ArrayList<>(); // control fields that need to validate or ignore during validation
    private boolean validateControlFieldsOnly = false;      // flag to control either validate the control fields or ignore them.
    Map<String,String> cache;

    private Map<String, List<String>> controlValues = new HashMap<>();

    // flag for watermarking
    protected boolean watermarking = false;

    // flag for ignoring copyright
    protected boolean ignoreCopyright = true;

    /**
     * Create an ExcelSource object from an Excel file on disk.
    **/
    public ExcelSource( File f ) throws IOException, InvalidFormatException
    {
        this( f, null );
    }

    /**
     * Create an ExcelSource object from an Excel file on disk with ignored fields.
    **/
    public ExcelSource( File f, List<String> ignoredFields ) throws IOException, InvalidFormatException
    {
        this( new FileInputStream(f), ignoredFields );
    }

    /**
     * Create an ExcelSource object from an Excel file on disk with ignored fields.
    **/
    public ExcelSource( File f, List<String> controlFields, boolean validateControlFieldsOnly )
            throws IOException, InvalidFormatException
    {
        this( new FileInputStream(f), controlFields, validateControlFieldsOnly );
    }

    /**
     * Create an ExcelSource object from an Excel file on disk with ignored fields and control values.
    **/
    public ExcelSource( File f, List<String> controlFields, boolean validateControlFieldsOnly,
            Map<String, List<String>> controlValues ) throws IOException, InvalidFormatException
    {
        this( new FileInputStream(f), controlFields, validateControlFieldsOnly, controlValues );
    }

    /**
     * Create an ExcelSource object from an InputStream
    **/
    public ExcelSource( InputStream in )
        throws IOException, InvalidFormatException
    {
        this(in, null);
    }

    /**
     * Create an ExcelSource object from an InputStream with ignored fields
    **/
    public ExcelSource( InputStream in,  List<String> ignoredFields )
        throws IOException, InvalidFormatException
    {
        this(in, ignoredFields, false);
    }
    /**
     * Create an ExcelSource object from an InputStream with ignored fields and allowedFields
    **/
    public ExcelSource( InputStream in, List<String> controlFields, boolean validateControlFieldsOnly )
        throws IOException, InvalidFormatException
    {
        this(in, controlFields, validateControlFieldsOnly, CONTROL_VALUES);
    }

    /**
     * Create an ExcelSource object from an InputStream with ignored fields and allowedFields
    **/
    public ExcelSource( InputStream in, List<String> controlFields, boolean validateControlFieldsOnly,
            Map<String, List<String>> controlValues )
        throws IOException, InvalidFormatException
    {
        this.controlValues = controlValues;
        this.validateControlFieldsOnly = validateControlFieldsOnly;
        if ( controlFields != null )
            this.controlFields.addAll(controlFields);

        this.book = WorkbookFactory.create(in);
        
        // always use the the first sheet.
        this.sheet = book.getSheetAt(0);
        
        this.lastRow = sheet.getLastRowNum();
        if ( this.lastRow == 0 )
        {
            this.lastRow = sheet.getPhysicalNumberOfRows() - 1;
        }

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
                
                // keep the original header for error report
                originalHeaders.put(lcHeader, header);
                if ( validateControlFieldsOnly )
                {
                    // report invalid subject import headers
                    if ( StringUtils.isNotBlank(header) && !controlFields.contains(headerToValidate(header)) )
                        invalidHeaders.add(header); 
                } else if ( controlValues != null && controlValues.size() > 0 
                        && StringUtils.isNotBlank(header) && !controlValues.containsKey(headerToValidate(lcHeader)) )
                {
                    invalidHeaders.add(header);
                }
            }
            currRow++;
            cache = parseRow( currRow );
        }
    }

    /**
     * Get the watermarking flag
     */
    public boolean isWatermarking() {
        return watermarking;
    }

    /**
     * Set the watermarking flag
     * @param watermarking
     */
    public void setWatermarking(boolean watermarking) {
        this.watermarking = watermarking;
    }

    @Override
    public Record nextRecord() throws Exception
    {
        if ( currRow < lastRow )
        {
            // parse an object record (if we don't already have a leftover)
            while ( currRow < lastRow && (cache == null || cache.size() == 0))
            {
                cache = parseRow( currRow++ );
            }

            if (cache.size() > 0) {
                TabularRecord rec = new TabularRecord();
                rec.setData( cache );
                rec.setWatermarking(watermarking);
                rec.setIgnoreCopyright(ignoreCopyright);

                String objID = cache.get(OBJECT_ID);
                String cmpID = null;

                // look for component/sub-component records
                while (currRow < lastRow && (cmpID == null || cmpID.equals(objID)))
                {
                    currRow++;
                    Map<String,String> cmpData = parseRow( currRow );
                    cmpID = cmpData.get(OBJECT_ID);
                    String objectComponentType = cmpData.get(OBJECT_COMPONENT_TYPE);
                    if ( objectComponentType != null && (objectComponentType.equalsIgnoreCase(COMPONENT) || objectComponentType.equalsIgnoreCase(SUBCOMPONENT))
                            && (cmpID == null || cmpID.trim().equals("") || cmpID.equals(objID)) )
                    {
                        TabularRecord component = new TabularRecord();
                        component.setData(cmpData);
                        component.setWatermarking(watermarking);
                        component.setIgnoreCopyright(ignoreCopyright);

                        if (objectComponentType.equalsIgnoreCase(COMPONENT)) {
                            // component record, add to list
                            rec.addComponent( component );
                        } else if (objectComponentType.equalsIgnoreCase(SUBCOMPONENT)) {
                            // sub-component record, add to child list
                            List<TabularRecord> components = rec.getComponents();
                            if ( components.size() == 0 )
                                throw new Exception ("Parent component is missing for sub-component in object " + objID + ".");
                            components.get( components.size() - 1 ).addComponent( component );
                        } else 
                            throw new Exception ("Unknown Level value for object/component/sub-component option in object " + objID + ".");

                        cmpID = null;
                        cache = null;
                    }
                    else
                    {
                        // this is the next object record, save for next time
                        cache = cmpData;
                        break;
                    }
                }
                return rec;
            }
        }
        else if ( cache != null && cache.size() > 0)
        {
            TabularRecord rec = new TabularRecord(cache);
            rec.setWatermarking(watermarking);
            rec.setIgnoreCopyright(ignoreCopyright);

            cache = null;
            return rec;
        }
        return null;
    }

    /**
     * Parse a row of data and return it as a Map
    **/
    protected Map<String,String> parseRow( int n )
    {
        Row row = sheet.getRow(n);
        Map<String,String> values = new HashMap<>();
        Map<String,String> invalids = new TreeMap<>();
        Map<String,String> replacedCharValues = new TreeMap<>();
        if (row != null) 
        {
            for ( int i = 0; i < headers.size(); i++ )
            {
                String header = headers.get(i);
                String headerToValidate = headerToValidate(header);

                // skip parsing the values in the ignored fields 
                if (!validateControlFieldsOnly && controlFields != null && controlFields.indexOf(headerToValidate) >= 0)
                    continue;

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

                        // check for invalid control values
                        String originalHeader = originalHeaders.get(header);
                        List<String> validValue = controlValues.get(headerToValidate);
                        if (validValue != null && validValue.size() > 0) 
                        {
                            if (originalHeader.equalsIgnoreCase(headerToValidate(BEGIN_DATE))
                                    || originalHeader.equalsIgnoreCase(headerToValidate(END_DATE))
                                    || originalHeader.equalsIgnoreCase(headerToValidate(LICENSE_BEGIN_DATE))
                                    || originalHeader.equalsIgnoreCase(headerToValidate(LICENSE_END_DATE)))
                            {
                                // validate date-time format
                                boolean validDate = false;
                                StringBuilder messageBuilder = new StringBuilder();

                                // match the datetime format for validation
                                String matchedFormat = matchDateTimeFormat(validValue, value);

                                // validate against the matched format if one exists. Otherwise validate against all supported datetime formats.
                                for (String dateFormatter : validValue)
                                {
                                    if (StringUtils.isBlank(matchedFormat) || dateFormatter.equals(matchedFormat)) {
                                        try {
                                            validateDateTime(dateFormatter, value);
                                            validDate = true;
                                            break;
                                        } catch (IllegalArgumentException ex) {
                                            String errorMessage = (messageBuilder.length() > 0 ? " | " : "")
                                                + buildErrorReportMessage(values.get(TabularRecord.OBJECT_ID), header, value, ex.getMessage());
                                            messageBuilder.append(errorMessage);
                                        }
                                    }
                                }

                                if (!validDate)
                                {
                                    invalids.put(originalHeader, messageBuilder.toString());
                                }
                            }
                            else
                            {
                                // validate cell with multiple values
                                String[] vals2Valid = value.split("\\" + DELIMITER);
                                for (String val : vals2Valid) 
                                {
                                    String normVal = val.trim();
                                    if (headerToValidate.equalsIgnoreCase("Language")) {
                                        // skip validation when ark provided with batch overlay
                                        if (arkPresented(normVal))
                                            continue;

                                        // strip ark value for validation with batch overlay
                                        normVal = stripArkValue(normVal);

                                        normVal = normalizeFieldValue(normVal, DELIMITER_LANG_ELEMENT);
                                    } else if (headerToValidate.equalsIgnoreCase(SubjectTabularRecord.SUBJECT_TYPE))
                                        // Subject Import: case insensitive subject header value
                                        normVal = val.toLowerCase();

                                    if (!validValue.contains(normVal)) {
                                        String existing = invalids.get(originalHeader);
                                        if ( existing == null )
                                            invalids.put(originalHeader, val);
                                        else
                                            invalids.put(originalHeader, existing + " " + DELIMITER + " " + val);
                                    }
                                }
                            }
                        }

                        // Replace control characters
                        String convertedValue = replaceControlChars( value );
                        if (StringUtils.isNotBlank(convertedValue))
                        {
                            replacedCharValues.put(originalHeader, convertedValue);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    String existing = values.get(headerToValidate);
                    if ( existing == null )
                    {
                        values.put(headerToValidate, value);
                    }
                    else
                    {
                        values.put(headerToValidate, existing + DELIMITER + value);
                    }
                }
            }
            
            if (invalids.size() > 0) 
            {
                invalids.put("row", "" + (n + 1));
                invalids.put(TabularRecord.OBJECT_ID, values.get(TabularRecord.OBJECT_ID));
                invalidValues.add(invalids);
            }

            if (replacedCharValues.size() > 0)
            {
                replacedCharValues.put("row", "" + (n + 1));
                replacedCharValues.put(TabularRecord.OBJECT_ID, values.get(TabularRecord.OBJECT_ID));
                controlCharValues.add(replacedCharValues);
            }
        }
        return values;
    }

    /**
     * Replace the control characters with the character name in blanket like [END OF TEXT]
     * @param value
     * @return String the value with control characters replaced or null
     */
    public static String replaceControlChars(String value) {
        boolean controlCharsFound = false;
        boolean escaped = false;

        StringBuilder sb = new StringBuilder();

        for( char ch : value.toCharArray() )
        {
            if( escaped )
            {
                boolean replaced = TabularRecord.handleEscapedCharacter(sb, ch, true);
                if ( replaced )
                    controlCharsFound = true;

                escaped = false;
            }
            else if ( ch == TabularRecord.ESCAPE_CHAR )
            {
                escaped = true;
            }
            else if ( Character.isISOControl(ch) && !TabularRecord.RETAIN_CONTROL_CHAR_LIST.contains(ch) )
            {
                // report control character and replace it with [character name]
                controlCharsFound = true;
                sb.append( "[" + Character.getName( ch ) + "]" );
            }
            else
            {
                sb.append( ch );
            }
        }

        if ( controlCharsFound )
        {
            return sb.toString();
        }

        return null;
    }

    private String matchDateTimeFormat(List<String> datetimeFormats, String datetimeValue) {
        String[] dateValueParts = datetimeValue.split("-");
        for (String dateFormat : datetimeFormats)
        {
            if (dateValueParts.length == dateFormat.split("-").length) {
                return dateFormat;
            }
        }
        return null;
    }

    /**
     * Get the list of invalid columns
     * @return
     */
    public List<String> getInvalidColumns()
    {
        return invalidHeaders;
    }

    /**
     * Get the list of invalid values
     * @return
     */
    public List<Map<String, String>> getInvalidValues()
    {
        return invalidValues;
    }

    /**
     * Get the list of values that contains control characters
     * @return
     */
    public List<Map<String, String>> getControlCharValues()
    {
        return controlCharValues;
    }

    /**
     * Get the Control Values
     * @return
     */
    public static Map<String, List<String>> getControlValues()
    {
        return CONTROL_VALUES;
    }

    /**
     * Initiate the control values with the standard input template for column names (sheet name: Item description), 
     * select header names (sheet name: Select-a-header values) and field values (sheet name: CV values) 
     * @param template
     * @throws Exception 
     */
    public synchronized static void initControlValues(File template) throws Exception 
    {
        if ( CONTROL_VALUES == null || CONTROL_VALUES.size() == 0 )
        {
            Workbook book = WorkbookFactory.create(template);

            // all control values
            Sheet cvsSheet = book.getSheet("CV values");

            // header/column names
            Sheet columnsSheet = book.getSheet("Item description");
            
            // Select-a-header values
            Sheet selectHeaderSheet = book.getSheet("Select-a-header values");

            // initiate the control values for column names
            initControlValues(columnsSheet, selectHeaderSheet, cvsSheet);
        }
    }

    /**
     * Initiate control values for column names and field values with data in the columns sheet and cvs sheet
     * @param columns
     * @param cvs
     * @throws Exception 
     */
    private static void initControlValues(Sheet columns, Sheet selectHeaderSheet, Sheet cvs) throws Exception 
    {
        CONTROL_VALUES = new HashMap<>();
        List<String> cvHeaders = new ArrayList<>();
        List<String> selectHeaders = new ArrayList<>();
        Map<String, List<String>> selectHeaderValues = new HashMap<>();
        
        // Initiate control values
        for ( Iterator<Row> it = cvs.rowIterator(); it.hasNext(); )
        {
            Row row = it.next();
            int lastCellNum = row.getLastCellNum();
            for ( int i = 0; i < lastCellNum; i++ )
            {
                Cell cell = row.getCell(i);
                if ( cell != null )
                {

                    String value = getCellValue(cell);
                    if ( StringUtils.isNotBlank(value) )
                    {
                        if (row.getRowNum() == 0) 
                        {
                            value = value.toLowerCase();
                            cvHeaders.add( value );
                            CONTROL_VALUES.put(value, new ArrayList<String>());
                        } 
                        else
                        {
                            value = new String(value.getBytes("UTF-8"));
                            String header = cvHeaders.get(cell.getColumnIndex());

                            // customize to ignore \ for Level column as requested in ticket DM-119
                            if (header.equalsIgnoreCase("Level") && value.startsWith("\\"))
                                value = value.substring(1, value.length());

                            if (header.equalsIgnoreCase("Language"))
                                value = normalizeFieldValue(value, DELIMITER_LANG_ELEMENT);

                            CONTROL_VALUES.get(header).add(value);
                        }
                    }
                }
            }
        }

        // ARK column
        if (!CONTROL_VALUES.containsKey("ark"))
            CONTROL_VALUES.put("ark", new ArrayList<String>());

        // select-a-header columns names
        for ( Iterator<Row> it = selectHeaderSheet.rowIterator(); it.hasNext(); )
        {
            Row row = it.next();
            int lastCellNum = row.getLastCellNum();

            for ( int i = 0; i < lastCellNum; i++ )
            {
                Cell cell = row.getCell(i);
                if ( cell != null )
                {
                    String value = getCellValue(cell);
                    if ( StringUtils.isNotBlank(value) )
                    {
                        if (row.getRowNum() == 0) 
                        {
                            value = value.toLowerCase();
                            // Select header column names
                            selectHeaders.add( value );
                            selectHeaderValues.put(value, new ArrayList<String>());
                        } 
                        else
                        {
                            // Select header values
                            value = new String(value.getBytes("UTF-8"));
                            String header = selectHeaders.get(cell.getColumnIndex());
                            
                            selectHeaderValues.get(header).add(value.toLowerCase());

                            if(row.getRowNum() == 1)
                                System.out.println("Select header value " + header + ": " + value);
                        }
                    }
                }
            }        
        }

        // add headers/column names
        for ( Iterator<Row> it = columns.rowIterator(); it.hasNext(); )
        {
            Row row = it.next();
            int lastCellNum = row.getLastCellNum();
            for ( int i = 0; i < lastCellNum; i++ )
            {
                Cell cell = row.getCell(i);
                if ( cell != null )
                {
                    String value = getCellValue(cell);
                    if ( StringUtils.isNotBlank(value) )
                    {
                        value = value.toLowerCase();
                        if (selectHeaderValues.containsKey(value)) 
                        {
                            List<String> headers = selectHeaderValues.get(value);
                            for (String header : headers) {
                                if ( !CONTROL_VALUES.containsKey(header) )
                                {
                                    CONTROL_VALUES.put(header, new ArrayList<String>());
                                }

                                // convert column values in "--Select a Subject:[type]--" for subject type validation
                                if ( value.equalsIgnoreCase("--Select a Subject:[type]--") )
                                {
                                    List<String> subjectTypes = CONTROL_VALUES.get(SubjectTabularRecord.SUBJECT_TYPE);
                                    if ( subjectTypes == null )
                                    {
                                        subjectTypes = new ArrayList<>();
                                        CONTROL_VALUES.put(SubjectTabularRecord.SUBJECT_TYPE, subjectTypes);
                                    }

                                    if ( !subjectTypes.contains(header) )
                                        subjectTypes.add(header);
                                }
                            }
                        } 
                        else if ( !CONTROL_VALUES.containsKey(value) )
                        {
                            CONTROL_VALUES.put(value, new ArrayList<String>());
                        }
                    }
                }
            }
        }

        // add supported ISO date formats for validation
        addDateValidationFromats(CONTROL_VALUES);
    }

    /*
     * Add supported ISO date formats for validation
     */
    protected static void addDateValidationFromats(Map<String, List<String>> controlValues) {
        // add supported ISO date formats for validation
        for (String dateField : DATE_FIELDS) {
            List<String> validDateFormats = controlValues.get(dateField.toLowerCase());
            if (validDateFormats != null && validDateFormats.isEmpty()) {
                for (String dateFormat : DATETIME_FORMATS) {
                    validDateFormats.add(dateFormat);
                }
            }
        }
    }

    /*
     * The header to validate: original header for excel import
     * @param header
     * @return
     */
    protected String headerToValidate(String header) {
        return header;
    }

    /*
     * Parse date-time value with the format provided.
     * @param dateTimeFormat
     * @param dateTimeValue
     */
    private void validateDateTime(String dateTimeFormat, String dateTimeValue) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(dateTimeFormat);
        fmt.parseDateTime(dateTimeValue);
    }

    /*
     * Build report message. Error message from parsing date-time value is in the format like: 
     * Invalid format: Invalid format: "2018/02/28" is malformed at "/02/28"
     * Cannot parse "2018-02-31": Value 30 for dayOfMonth must be in the range [1,29]
     * Cannot parse "2018-13-30": Value 13 for monthOfYear must be in the range [1,12]
     * @return Invalid [header] [dateTimeValue] in record [recordId]: [message].
     */
    private String buildErrorReportMessage(String recordId, String header, String dateTimeValue, String errorMessage)
    {
        String errorReport = errorMessage;
        String[] errorTokens = errorMessage.split(":");
        if (errorTokens.length == 2) {
            errorReport = errorTokens[1];
        }

        errorReport = "Invalid " + header +  " " + dateTimeValue +  " in record " + recordId + ": " + errorReport;
        return errorReport;
    }

    private static String getCellValue(Cell cell) {
        cell.setCellType(Cell.CELL_TYPE_STRING);
        String value = cell.toString();
        return value==null?value:value.trim();
    }

    private static String normalizeFieldValue(String value, String delimiter) 
    {
        String[] langElems = value.split("\\" + delimiter);
        if (langElems.length == 2)
            value = langElems[0].trim() + delimiter + langElems[1].trim();
        return value;
    }

    /*
     * Strip the ark value for validation, which will be presented for batch overlay.
     * @param value
     * @return
     */
    protected String stripArkValue(String value) {
        return value;
    }

    /*
     * Determine whether ark is allowed and presented for batch overlay
     * that don't need value validation
     * @param value
     * @return
     */
    protected boolean arkPresented(String value) {
        return false;
    }
}
