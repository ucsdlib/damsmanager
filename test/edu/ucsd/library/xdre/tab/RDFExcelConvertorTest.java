package edu.ucsd.library.xdre.tab;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.TransformerException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

/**
 * Test methods for RDFExcelConvertorTest class
 * @author lsitu
 *
 */
public class RDFExcelConvertorTest {

    @Test
    public void testJSONParseBackslashException() throws UnsupportedEncodingException, IOException {
        String jsonString = "{\"record_id\":[{\"title\":\"Test title backslash \\\"},{\"typeOfResource\":\"Still Image\"}]}";
        String error = "Error parse json: Unexpected character (t) at position 51.\n"
                + "Source: {\"record_id\":[{\"title\":\"Test title backslash \\\"},{\"typeOfResource\":\"Still Image\"}]}";
        try {
            RDFExcelConvertor.parseJson(jsonString);
            fail();
        } catch(TransformerException ex) {
            assertEquals(error, ex.getMessage());
        }
    }

    @Test
    public void testJsonParseWithBackslashEscaped() throws UnsupportedEncodingException, IOException, TransformerException {
        String jsonString = "{\"record_id\":[{\"title\":\"Test title backslash \\\\\"},{\"typeOfResource\":\"Still Image\"}]}";

        JSONObject json = RDFExcelConvertor.parseJson(jsonString);
        String expectedTitle = ((JSONObject)((JSONArray)json.get("record_id")).get(0)).get("title").toString();
        assertEquals("Test title backslash \\", expectedTitle);
    }
}
