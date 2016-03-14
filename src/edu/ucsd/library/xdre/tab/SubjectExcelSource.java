package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * class SubjectRecordSource implementation that uses Apache POI to read Excel files.
 * @author lsitu
 * @since 2016-03-10
**/
public class SubjectExcelSource extends ExcelSource
{

	public SubjectExcelSource(File f, List<String> validFields) throws IOException,
			InvalidFormatException {
		super(f, validFields, true);
	}

    @Override
    public Record nextRecord() throws Exception
    {
    	Record rec = null;
    	if (cache != null)
    	{
    		rec = new SubjectTabularRecord( cache );
    	}
    	else if ( currRow < lastRow )
        {
        	cache = parseRow( ++currRow );
            rec = new SubjectTabularRecord( cache );
        }
    	cache = null;
        return rec;
    }
}
