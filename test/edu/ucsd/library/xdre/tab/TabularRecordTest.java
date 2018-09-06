package edu.ucsd.library.xdre.tab;

import static edu.ucsd.library.xdre.tab.TabularRecord.DELIMITER;
import static org.junit.Assert.assertEquals;

import java.util.List;

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
}
