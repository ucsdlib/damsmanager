package edu.ucsd.library.xdre.tab;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test methods for ExcelSource class
 * @author lsitu
 *
 */
public class ExcelSourceTest {

    @Test
    public void testReplaceControlChars() {
        String val = "Test control character\3.\\3";

        String result = ExcelSource.replaceControlChars(val);
        assertEquals("Value doesn't match!", "Test control character[END OF TEXT].[END OF TEXT]", result);
    }

    @Test
    public void testReplaceControlCharsWithNoControlCharacters() {
        String val = "Test Çatalhöyük \u00c7 \\t \\r \\n control character.";

        String result = ExcelSource.replaceControlChars(val);
        assertEquals("Value is not null!", null, result);
    }
}
