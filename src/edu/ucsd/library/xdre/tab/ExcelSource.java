package edu.ucsd.library.xdre.tab;

import static edu.ucsd.library.xdre.tab.TabularRecord.OBJECT_ID;
import static edu.ucsd.library.xdre.tab.TabularRecord.DELIMITER;
import static edu.ucsd.library.xdre.tab.TabularRecord.OBJECT_COMPONENT_TYPE;
import static edu.ucsd.library.xdre.tab.TabularRecord.COMPONENT;
import static edu.ucsd.library.xdre.tab.TabularRecord.SUBCOMPONENT;

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

/**
 * RecordSource implementation that uses Apache POI to read Excel (OLE or XML)
 * files.
 * @author lsitu
 * @author escowles
 * @since 2014-06-05
**/
public class ExcelSource implements RecordSource
{
    private static Map<String, List<String>> CONTROL_VALUES = new HashMap<>();
    
    private Workbook book;
    private Sheet sheet;
    private int lastRow;
    private int currRow;
    private List<String> headers;
    private Map<String, String> originalHeaders = new HashMap<>();
    private List<String> invalidHeaders = new ArrayList<>();
    private List<Map<String, String>> invalidValues = new ArrayList<>();
    Map<String,String> cache;

    /**
     * Create an ExcelSource object from an Excel file on disk.
    **/
    public ExcelSource( File f ) throws IOException, InvalidFormatException
    {
        this( new FileInputStream(f) );
    }

    /**
     * Create an ExcelSource object from an InputStream
    **/
    public ExcelSource( InputStream in )
        throws IOException, InvalidFormatException
    {
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
            	String lcHeader = header.toLowerCase();
                headers.add( lcHeader );
                
                // keep the original header for error report
                originalHeaders.put(lcHeader, header);
                if ( CONTROL_VALUES != null && CONTROL_VALUES.size() > 0 
                		&& StringUtils.isNotBlank(lcHeader) && !CONTROL_VALUES.containsKey(lcHeader) )
                {
                	invalidHeaders.add(header);     		
                }
            }
            currRow++;
            cache = parseRow( currRow );
        }
    }

    @Override
    public TabularRecord nextRecord() throws Exception
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
            cache = null;
            return rec;
        }
        return null;
    }

    /**
     * Parse a row of data and return it as a Map
    **/
    private Map<String,String> parseRow( int n )
    {
        Row row = sheet.getRow(n);
        Map<String,String> values = new HashMap<>();
        Map<String,String> invalids = new TreeMap<>();
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

	                    // check for invalid control values
	                    List<String> validValue = CONTROL_VALUES.get(header);
	                    if (validValue != null && validValue.size() > 0 && !validValue.contains(value.toLowerCase())) 
	                    {
	    	                String existing = invalids.get(header);
	    	                if ( existing == null )
	    	                	invalids.put(originalHeaders.get(header), value);
	    	                else
	    	                    values.put(originalHeaders.get(header), existing + DELIMITER + value);
	                    }
	                } catch (UnsupportedEncodingException e) {
	                    e.printStackTrace();
	                }
	                String existing = values.get(header);
	                if ( existing == null )
	                {
	                    values.put(header, value);
	                }
	                else
	                {
	                    values.put(header, existing + DELIMITER + value);
	                }
	            }
	        }
	        
	        if (invalids.size() > 0) 
	        {
	        	invalids.put("row", "" + (n + 1));
	        	invalids.put(TabularRecord.OBJECT_ID, values.get(TabularRecord.OBJECT_ID));
	        	invalidValues.add(invalids);
	        }
        }
        return values;
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
     * Get the Control Values
     * @return
     */
    public Map<String, List<String>> getControlValues()
    {
    	return CONTROL_VALUES;
    }

    /**
     * Initiate control values for column names and field values
     * @param columns
     * @param cvs
     * @throws IOException 
     * @throws InvalidFormatException 
     */
    public synchronized static void initControlValues(File columns, File cvs) throws InvalidFormatException, IOException 
    {
    	if ( CONTROL_VALUES == null || CONTROL_VALUES.size() == 0 )
    	{
    		CONTROL_VALUES = new HashMap<>();
    		List<String> cvHeaders = new ArrayList<>();
    		
    		// Initiate control values
    		Workbook book = WorkbookFactory.create(cvs);
    		Sheet sheet = book.getSheetAt(0);
    		for ( Iterator<Row> it = sheet.rowIterator(); it.hasNext(); )
    		{
    			Row row = it.next();
				int lastCellNum = row.getLastCellNum();
				for ( int i = 0; i < lastCellNum; i++ )
				{
					Cell cell = row.getCell(i);
					if ( cell != null )
					{
						cell.setCellType(Cell.CELL_TYPE_STRING);
	                    String value = cell.toString();
	                    if ( value != null && !(value = value.trim()).equals("") )
	    	            {
	                    	value = value.toLowerCase();
							if (row.getRowNum() == 0) 
							{
								cvHeaders.add( value );
								CONTROL_VALUES.put(value, new ArrayList<String>());
							} 
							else
							{
								value = new String(value.getBytes("UTF-8"));
								String header = cvHeaders.get(cell.getColumnIndex());
								CONTROL_VALUES.get(header).add(value);
							}
	    	            }
					}
				}
    		}
    		
    		// ARK column
    		if (!CONTROL_VALUES.containsKey("ark"))
    			CONTROL_VALUES.put("ark", new ArrayList<String>());
    		
    		// add all valid column names
    		book = WorkbookFactory.create(columns);
    		sheet = book.getSheetAt(0);
    		for ( Iterator<Row> it = sheet.rowIterator(); it.hasNext(); )
    		{
    			Row row = it.next();
				int lastCellNum = row.getLastCellNum();
				for ( int i = 0; i < lastCellNum; i++ )
				{
					Cell cell = row.getCell(i);
					if ( cell != null )
					{
						cell.setCellType(Cell.CELL_TYPE_STRING);
	                    String value = cell.toString();
	                    if ( value != null && !(value = value.trim()).equals("") && !CONTROL_VALUES.containsKey(value.toLowerCase()) )
	    	            {
	                    	value = value.toLowerCase();
	                    	CONTROL_VALUES.put(value, new ArrayList<String>());
	    	            }
					}
				}
    		}
    	}
    }
}
