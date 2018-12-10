package edu.ucsd.library.xdre.harvesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Test methods for FieldMapping class
 * @author lsitu
 *
 */
public class FieldMappingsTest {

    @Test
    public void testParseFieldMapping() throws Exception {
        File mappingFile = getMappingFile();
        assertTrue(mappingFile.exists() && mappingFile.length() > 0);
        FieldMappings fieldMappings = new FieldMappings(mappingFile);

        // constant fields
        Map<String, String> constantFields = fieldMappings.getConstantFields();
        assertTrue(constantFields.keySet().contains("Type of Resource"));
        assertEquals("data|still image", constantFields.get("Type of Resource"));
        assertEquals("zxx  - No linguistic content; Not applicable", constantFields.get("Language"));
        Calendar cal = Calendar.getInstance();
        assertEquals("" + cal.get(Calendar.YEAR), constantFields.get("date:issued"));

        // Excel headings mapping 
        Map<String, List<String>> mappings = fieldMappings.getOriginalFieldMappings();
        assertTrue(mappings.keySet().contains("identifier / json file name for object"));
        assertEquals("object Unique ID", mappings.get("identifier / json file name for object").get(0));
        assertEquals("Title", mappings.get("CIL_CCDB.Citation.Title").get(0));
        assertEquals(3, mappings.get("CIL_CCDB.CIL.CORE.ATTRIBUTION.DATE").size());
        assertEquals("date:creation", mappings.get("CIL_CCDB.CIL.CORE.ATTRIBUTION.DATE").get(0));
        assertEquals("Begin date", mappings.get("CIL_CCDB.CIL.CORE.ATTRIBUTION.DATE").get(1));
        assertEquals("End date", mappings.get("CIL_CCDB.CIL.CORE.ATTRIBUTION.DATE").get(2));

        assertEquals("person:researcher", mappings.get("CIL_CCDB.CIL.CORE.ATTRIBUTION.Contributors").get(0));
        assertEquals("subject:topic", mappings.get("CIL_CCDB.CIL.CORE.BIOLOGICALPROCESS.free_text").get(0));
    }

    private File getMappingFile() throws IOException {
        File mappingFile = new File("CIL Processing and Mapping Instructions.xlsx");
        //mappingFile.deleteOnExit();
        byte[] buf = new byte[4096];
        try(InputStream in = getClass().getResourceAsStream("/resources/CIL Processing and Mapping Instructions.xlsx");
                FileOutputStream out = new FileOutputStream(mappingFile)) {

            int bytesRead = 0;
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }
        return mappingFile;
    }
}
