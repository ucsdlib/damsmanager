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
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * RecordSource implementation that uses Apache POI to read Excel (OLE or XML)
 * files.
 * @author escowles
 * @since 2014-06-05
**/
public class ExcelSource implements RecordSource
{
    private Workbook book;
    private Sheet sheet;
    private int lastRow;
    private int currRow;
    private List<String> headers;
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
        
        // use the the first sheet when there's only one sheet.
        int numberOfSheets = book.getNumberOfSheets();
        if (numberOfSheets == 1)
        {
        	this.sheet = book.getSheetAt(0);
        }
        else
        {
            this.sheet = book.getSheetAt(1);
        }
        
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
                headers.add( firstRow.getCell(i).getStringCellValue().toLowerCase() );
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
            if ( cache == null )
            {
                cache = parseRow( currRow );
            }

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
        else if ( cache != null )
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
        return values;
    }
}
