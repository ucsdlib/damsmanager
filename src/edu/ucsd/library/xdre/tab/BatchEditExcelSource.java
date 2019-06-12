package edu.ucsd.library.xdre.tab;

import static edu.ucsd.library.xdre.tab.TabularRecord.COMPONENT;
import static edu.ucsd.library.xdre.tab.TabularRecord.OBJECT_COMPONENT_TYPE;
import static edu.ucsd.library.xdre.tab.TabularRecord.OBJECT_ID;
import static edu.ucsd.library.xdre.tab.TabularRecord.SUBCOMPONENT;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * RecordSource implementation for batch editing that uses Apache POI to read Excel (OLE or XML)
 * files for BatchEdit
 * @author lsitu
 */
public class BatchEditExcelSource extends ExcelSource {

    public BatchEditExcelSource(File f, List<String> controlFields) throws IOException,
            InvalidFormatException {
        super(f, controlFields, false);
    }

    /**
     * See ExcelSource nextRecord()
     */
    @Override
    public Record nextRecord() throws Exception {
        if ( currRow < lastRow )
        {
            // parse an object record (if we don't already have a leftover)
            while ( currRow < lastRow && (cache == null || cache.size() == 0))
            {
                cache = parseRow( currRow++ );
            }

            if (cache.size() > 0) {
                TabularRecord rec = new TabularEditRecord();
                rec.setData( cache );
                String objID = cache.get(OBJECT_ID);
                String cmpID = null;

                // look for component/sub-component records
                while (currRow < lastRow)
                {
                    currRow++;
                    Map<String,String> cmpData = parseRow( currRow );
                    cmpID = cmpData.get(OBJECT_ID);
                    String objectComponentType = cmpData.get(OBJECT_COMPONENT_TYPE);

                    if ( objectComponentType != null
                            && (objectComponentType.equalsIgnoreCase(COMPONENT)
                                    || objectComponentType.equalsIgnoreCase(SUBCOMPONENT))
                            && (cmpID.equals(objID) || cmpID.startsWith(objID + "/")) )
                    {
                        TabularRecord component = new TabularEditRecord();
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
                            throw new Exception ("Unknown Level for object/component/sub-component in object " + objID + ".");

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
            TabularRecord rec = new TabularEditRecord(cache);
            cache = null;
            return rec;
        }
        return null;
    }

    /*
     * The header to validate: Remove the FAST heading suffix
     * @param header
     * @return String
     */
    @Override
    protected String headerToValidate(String header) {
        return header.toLowerCase().replace(" fast", "");
    }
}
