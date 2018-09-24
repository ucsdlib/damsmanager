package edu.ucsd.library.xdre.tab;

import static edu.ucsd.library.xdre.tab.TabularRecord.DELIMITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Test;

/**
 * Test methods for TabularRecord class
 * @author lsitu
 *
 */
public class TabularRecordTest {

    @Test
    public void testSplitValueWithControlCharacters() {
        String val = "Test control character\3.\\3";

        List<String> result = TabularRecord.split(val);
        assertEquals("Value doesn't match!", "Test control character.", result.get(0));
    }

    @Test
    public void testSplitValueWithRetainedCRLF() {
        char lf = (char)10;
        char cr = (char)13;
        String val = "Test CR/LF control character." + cr + lf + "Another paragraph.";

        List<String> result = TabularRecord.split(val);
        assertEquals("line feed (lf)", Character.getName(lf).toLowerCase());
        assertEquals("carriage return (cr)", Character.getName(cr).toLowerCase());
        assertEquals("Value with chars CR/LF doesn't match!", "Test CR/LF control character." + cr + lf +
                "Another paragraph.", result.get(0));
    }

    @Test
    public void testSplitValueWithWithSpecialCharacters() {
        String val = "Test Çatalhöyük \u00c7 \\t \\r \\n \".";

        List<String> result = TabularRecord.split(val);
        assertEquals("Value with special characters doesn't match!", "Test Çatalhöyük \u00c7 \\t \\r \\n \".", result.get(0));
    }

    @Test
    public void testSplitValueWithPipeEscaped() {
        String val = "Test value 1 \\| Test value 2.";

        List<String> result = TabularRecord.split(val);
        assertEquals("Values size doesn't match!", 1, result.size());
        assertEquals("Character Pipe (|) isn't escaped correctly!", "Test value 1 | Test value 2.", result.get(0));
    }

    @Test
    public void testSplitMultipleValuesWithControlCharacters() {
        String val = "Test value 1\3. " + DELIMITER  + " Test value 2.\\3";

        List<String> result = TabularRecord.split(val);
        assertEquals("Values size doesn't match!", 2, result.size());
        assertEquals("Value 1 doesn't match!", "Test value 1.", result.get(0));
        assertEquals("Value 2 doesn't match!", "Test value 2.", result.get(1));
    }

    @Test
    public void testMutipleValuesforIdentifierNote() throws Exception {
        String idValues = "id 1" + DELIMITER + "id2" + DELIMITER + "id3";

        // Initiate tabular data with multiple Identifier:identifier values delimited by | character
        Map<String, String> data = new HashMap<>();
        data.put("object unique id", "object1");
        data.put("level", "object");
        data.put("title", "Test object");
        data.put("identifier:identifier", idValues);

        // Build the dams4 RDF/XML
        TabularRecord testObject = new TabularRecord(data);
        Document doc = testObject.toRDFXML();

        List<String> ids = Arrays.asList(idValues.split("\\|"));

        List<Node> nodes = doc.selectNodes("//dams:Note[dams:type='identifier']");
        assertEquals("The size of the identifier note doesn't match!", 3, nodes.size());
        for (Node node : nodes) {
            String id = node.selectSingleNode("rdf:value").getText();
            assertTrue("Identifier value doesn't match!", ids.contains(id));
        }
    }

    public void testSplitMultipleValuesWithNoControlCharacters() {
        String val = "Test value 1." + DELIMITER  + "Test value 2." + DELIMITER  + "Test value 3.";

        List<String> result = TabularRecord.split(val);
        assertEquals("Values size doesn't match!", 3, result.size());
        assertEquals("Value 1 doesn't match!", "Test value 1.", result.get(0));
        assertEquals("Value 2 doesn't match!", "Test value 2.", result.get(1));
        assertEquals("Value 3 doesn't match!", "Test value 3.", result.get(2));
    }

    @Test
    public void testSplitMultipleValuesWithLineFeedAndAnchors() {
        char lf = (char)10;
        String val = "Test value 1 "+ lf + " <a href=\"http:\\example.com\"></a>" + DELIMITER  
                + "Test value 2 \\"+ lf + " <a href=\"http:\\example.com\"></a>" + DELIMITER
                + "Test value 3 "+ lf + " <a href=\"http:\\example.com\"></a>";

        List<String> result = TabularRecord.split(val);
        assertEquals("Values size doesn't match!", 3, result.size());
        assertEquals("Value 1 doesn't match!", "Test value 1 "+ lf + " <a href=\"http:\\example.com\"></a>", result.get(0));
        assertEquals("Value 2 doesn't match!", "Test value 2 \\"+ lf + " <a href=\"http:\\example.com\"></a>", result.get(1));
        assertEquals("Value 3 doesn't match!", "Test value 3 "+ lf + " <a href=\"http:\\example.com\"></a>", result.get(2));
    }
}
