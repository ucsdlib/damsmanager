package edu.ucsd.library.xdre.statistics.analyzer;

import static org.junit.Assert.assertEquals;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.utils.UnitTestBasic;

/**
 * Tests methods for Statistics class
 * @author lsitu
 *
 */
public class StatisticsTest extends UnitTestBasic {

    private static Document solrDoc = null;

    @Test
    public void testCompoundTitle() throws Exception {
        solrDoc = new SAXReader().read(getResourceFile("solrDoc.xml"));
        String compTitle = Statistics.getCompoundTitle(solrDoc, "2");
        assertEquals("FTIR plot: Data", compTitle);

        compTitle = Statistics.getCompoundTitle(solrDoc, "4");
        assertEquals("XRF plot: Data", compTitle);
    }
}
