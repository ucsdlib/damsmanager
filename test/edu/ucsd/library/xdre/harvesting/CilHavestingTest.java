package edu.ucsd.library.xdre.harvesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Before;
import org.junit.Test;

import edu.ucsd.library.xdre.tab.TabularRecord;

/**
 * Test methods for CilHavesting class
 * @author lsitu
 *
 */
public class CilHavestingTest extends CilHavestingTestBase {

    private Map<String, String> constantFields = null;
    private Map<String, List<String>> fieldMappings = null;

    @Before
    public void init() {
        constantFields = initiateConstantsFileds();
        fieldMappings = initiateFiledMappings();
    }

    @Test
    public void testExtractTitle() throws Exception {
        String[] files = {createJsonTestFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertEquals("CIL:37147.", rec.getData().get(FieldMappings.TITLE));
    }

    @Test
    public void testExtractSubjectCloseMatch() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertTrue("Subject:anatomy does't match.", rec.getData().get("subject:anatomy").contains("membrane"));

        Map<String, String> subjectHeadings = cilHarvesting.getSubjectHeadings();
        assertTrue("Subject:anatomy CloseMatch does't match.", subjectHeadings.containsKey("subject:anatomy|GO:0016020"));
        assertEquals("Subject:anatomy doesn't match.", "membrane", subjectHeadings.get("subject:anatomy|GO:0016020"));
        assertTrue("CSV output for subject:anatomy CloseMatch does't match.",
                cilHarvesting.getSubjectHeadingsCsv().contains("Subject:anatomy,,GO:0016020,membrane\n"));
    }

    @Test
    public void testExtractCopyrightNode() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertEquals("CopyrightNote does't match.", "attribution_nc_sa", rec.getData().get(TabularRecord.COPYRIGHT_NOTE.toLowerCase()));

        Document doc = rec.toRDFXML();
        assertEquals("attribution_nc_sa", doc.valueOf("//dams:Object/dams:copyright//dams:copyrightNote"));
    }

    @Test
    public void testExtractData() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();

        // constants metadata
        assertEquals("" + Calendar.getInstance().get(Calendar.YEAR), rec.getData().get(FieldMappings.DATE_ISSUED.toLowerCase()));
        assertEquals("data|still image", rec.getData().get(FieldMappings.TYPE_OF_RESOURCE.toLowerCase()));
        assertEquals("zxx  - No linguistic content; Not applicable", rec.getData().get(FieldMappings.LANGUAGE.toLowerCase()));

        // date created
        assertEquals("1958-02-02", rec.getData().get(FieldMappings.DATE_CREATION.toLowerCase()));
        assertEquals("1958-02-02", rec.getData().get(FieldMappings.BEGIN_DATE.toLowerCase()));

        // technical details
        String expectedResult = "PREPARATION a free test\r\nRelation to intact cell: sectioned tissue";
        String[] results = rec.getData().get(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase()).split("\\|");
        assertEquals(5, results.length);
        assertTrue(Arrays.asList(results).contains(expectedResult));
        assertTrue(Arrays.asList(results).contains("Preparation: PREPARATION for sectioned tissue"));
        assertTrue(Arrays.asList(results).contains("a free test"));
        assertTrue(Arrays.asList(results).contains("Source of contrast: differences in adsorption or binding of stain"));
        assertTrue(Arrays.asList(results).contains("Item type: recorded image"));

        // related resource
        expectedResult = "Source Record in the Cell Image Library @ https://doi.org/doi:10.7295/W9CIL37147";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        expectedResult = "George E. Palade EM Slide Collection @ http://cushing.med.yale.edu/gsdl/cgi-bin/library?c=palade&a=d&d=DpaladeFxB";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        // person:researcher
        assertEquals("W. Stoeckenius", rec.getData().get(PERSON_RESEARCHER.toLowerCase()));
        // subject:topic
        assertTrue(rec.getData().get(SUBJECT_TOPIC.toLowerCase()).contains("response to chemical stimulus"));
        // note:methods
        assertEquals("Gustafsdottir et al. (doi:10.1371/journal.pone.0080999)", rec.getData().get(NOTE_METHODS.toLowerCase()));

        // note:descriptions
        assertEquals("This group of micrographs illustrate", rec.getData().get(NOTE_DESCRIPTION.toLowerCase()));

        // Identifier:Identifier:samplenumber
        assertEquals("test123", rec.getData().get(OBJECT_UNIQUE_ID.toLowerCase()));
        assertEquals("test123", rec.getData().get(FieldMappings.IDENTIFIER_SAMPLENUMBER.toLowerCase()));
    }

    @Test
    public void testJSONNoTitle() throws Exception {
        String[] files = {createJsonNoTitleFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();

        assertEquals("test123", rec.getData().get(FieldMappings.TITLE));
        assertEquals("test123", rec.getData().get(FieldMappings.NOTE_PREFERRED_CITATION.toLowerCase()));
    }

    @Test
    public void testComponentData() throws Exception {
        String[] files = {createJsonComponentFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertNotNull(rec);
        assertTrue(rec.getComponents().size() == 6);

        // image component
        Map<String, String> compData = getComponentByFileName(rec, "37147.tif").getData();
        assertEquals("OME_tif format", compData.get(FieldMappings.TITLE.toLowerCase()));
        assertEquals("image-source", compData.get(FieldMappings.FILE_USE.toLowerCase()));

        // alternative image component
        compData = getComponentByFileName(rec, "BBBC022_v1_images_20585w2.zip").getData();
        assertEquals("BBBC022_v1_images_20585w2.zip", compData.get(FieldMappings.TITLE.toLowerCase()));
        assertEquals("data-service", compData.get(FieldMappings.FILE_USE.toLowerCase()));
    }

    @Test
    public void testJSONComponent() throws Exception {
        String[] files = {createJsonComponentFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertNotNull(rec);
        int compSize = rec.getComponents().size();
        assertTrue(compSize == 6);

        Map<String, String> compData = rec.getComponents().get(compSize - 1).getData();
        assertEquals("CIL source metadata (JSON)", compData.get(FieldMappings.TITLE.toLowerCase()));
        assertEquals("test123.json", compData.get(FieldMappings.FILE_NAME.toLowerCase()));
        assertEquals("data-service", compData.get(FieldMappings.FILE_USE.toLowerCase()));
    }

    @Test
    public void testRDF() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertNotNull(rec);

        Document doc = rec.toRDFXML();
        assertEquals("" + Calendar.getInstance().get(Calendar.YEAR), doc.valueOf("//dams:Object/dams:date/dams:Date[dams:type='issued']//rdf:value"));
        assertEquals("1958-02-02", doc.valueOf("//dams:Object/dams:date/dams:Date[dams:type='creation']//rdf:value"));
        assertEquals("CIL:37147.", doc.valueOf("//dams:Object/dams:title//mads:authoritativeLabel"));
    }

    @Test
    public void testComponentRDF() throws Exception {
        String[] files = {createJsonComponentFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();
        assertNotNull(rec);

        Document doc = rec.toRDFXML();
        assertEquals("CIL:37147.", doc.valueOf("//dams:Object/dams:title//mads:authoritativeLabel"));

        assertEquals(6, doc.selectNodes("//dams:Object//dams:Component").size());

        // tif image file component
        Node compFile = doc.selectSingleNode("//dams:Object//dams:Component//dams:File[dams:sourceFileName='37147.tif']");
        assertEquals("OME_tif format", compFile.valueOf("../../dams:title//mads:authoritativeLabel"));
        assertEquals("image-source", compFile.valueOf("dams:use"));
        // Jpeg image file component
        compFile = doc.selectSingleNode("//dams:Object//dams:Component//dams:File[dams:sourceFileName='37147.jpg']");
        assertEquals("Jpeg format", compFile.valueOf("../../dams:title//mads:authoritativeLabel"));
        assertEquals("image-source", compFile.valueOf("dams:use"));

        // Zip file component
        compFile = doc.selectSingleNode("//dams:Object//dams:Component//dams:File[dams:sourceFileName='37147.zip']");
        assertEquals("Zip format", compFile.valueOf("../../dams:title//mads:authoritativeLabel"));
        assertEquals("data-service", compFile.valueOf("dams:use"));

        // json source file component
        compFile = doc.selectSingleNode("//dams:Object//dams:Component//dams:File[dams:sourceFileName='test123.json']");
        assertEquals("CIL source metadata (JSON)", compFile.valueOf("../../dams:title//mads:authoritativeLabel"));
        assertEquals("data-service", compFile.valueOf("dams:use"));
    }


    @Test
    public void testCsvExportData() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));

        String csvValue = cilHarvesting.toCSV(getResourceFile("dams42json.xsl").getAbsolutePath());
        assertTrue(csvValue.contains("Unique ID,Level,"));
        assertTrue(csvValue.contains("test123,Object,"));
        assertTrue(csvValue.contains(",Gustafsdottir et al. (doi:10.1371/journal.pone.0080999),"));
        assertTrue(csvValue.contains("test123,Component,test123.json,data-service"));
    }

    @Test
    public void testCsvExportWithComponent() throws Exception {
        String[] files = {createJsonComponentFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));

        String csvValue = cilHarvesting.toCSV(getResourceFile("dams42json.xsl").getAbsolutePath());
        assertTrue(csvValue.contains("Unique ID,Level,File name,File use"));
        assertTrue(csvValue.contains("test123,Object,,"));
        assertTrue(csvValue.contains("test123,Component,BBBC022_v1_images_20585w1.zip,data-service"));
        assertTrue(csvValue.contains("test123,Component,BBBC022_v1_images_20585w2.zip,data-service"));
        assertTrue(csvValue.contains("test123,Component,37147.tif,image-source"));
        assertTrue(csvValue.contains("test123,Component,37147.jpg,image-source"));
        assertTrue(csvValue.contains("test123,Component,37147.zip,data-service"));
        assertTrue(csvValue.contains("test123,Component,test123.json,data-service"));
    }

    @Test
    public void testCsvExportMultiple() throws Exception {
        String[] files = {
                createJsonTestFile("test123a.json").getAbsolutePath(),
                createJsonDataFile("test123b.json").getAbsolutePath(),
                createJsonComponentFile("test123c.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));

        String csvValue = cilHarvesting.toCSV(getResourceFile("dams42json.xsl").getAbsolutePath());
        assertTrue(csvValue.contains("Unique ID,Level,File name,File use"));
        assertTrue(csvValue.contains("test123a,Object,,"));
        assertTrue(csvValue.contains("test123a,Component,test123a.json,data-service"));
        assertTrue(csvValue.contains("test123b,Object,,"));
        assertTrue(csvValue.contains("test123b,Component,test123b.json,data-service"));
        assertTrue(csvValue.contains("test123c,Object,,"));
        assertTrue(csvValue.contains("test123c,Component,BBBC022_v1_images_20585w1.zip,data-service"));
        assertTrue(csvValue.contains("test123c,Component,BBBC022_v1_images_20585w2.zip,data-service"));
        assertTrue(csvValue.contains("test123c,Component,37147.tif,image-source"));
        assertTrue(csvValue.contains("test123c,Component,37147.jpg,image-source"));
        assertTrue(csvValue.contains("test123c,Component,37147.zip,data-service"));
        assertTrue(csvValue.contains("test123c,Component,test123c.json,data-service"));
    }


    @Test
    public void testExtractDataIntegration() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        FieldMappings fieldMapping = new FieldMappings(getResourceFile("CIL Processing and Mapping Instructions.xlsx"));
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMapping.getFieldMappings(),
                fieldMapping.getConstantFields(), Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();

        // constants metadata
        assertEquals("" + Calendar.getInstance().get(Calendar.YEAR), rec.getData().get(FieldMappings.DATE_ISSUED.toLowerCase()));
        assertEquals("data|still image", rec.getData().get(FieldMappings.TYPE_OF_RESOURCE.toLowerCase()));
        assertEquals("zxx  - No linguistic content; Not applicable", rec.getData().get(FieldMappings.LANGUAGE.toLowerCase()));

        // date created
        assertEquals("1958-02-02", rec.getData().get(FieldMappings.DATE_CREATION.toLowerCase()));
        assertEquals("1958-02-02", rec.getData().get(FieldMappings.BEGIN_DATE.toLowerCase()));

        // technical details
        String expectedResult = "PREPARATION a free test\r\nRelation to intact cell: sectioned tissue";
        String[] results = rec.getData().get(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase()).split("\\|");
        assertEquals(5, results.length);
        assertTrue(Arrays.asList(results).contains(expectedResult));
        assertTrue(Arrays.asList(results).contains("Preparation: PREPARATION for sectioned tissue"));
        assertTrue(Arrays.asList(results).contains("a free test"));
        assertTrue(Arrays.asList(results).contains("Source of contrast: differences in adsorption or binding of stain"));
        assertTrue(Arrays.asList(results).contains("Item type: recorded image"));

        // related resource
        expectedResult = "Source Record in the Cell Image Library @ https://doi.org/doi:10.7295/W9CIL37147";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        expectedResult = "George E. Palade EM Slide Collection @ http://cushing.med.yale.edu/gsdl/cgi-bin/library?c=palade&a=d&d=DpaladeFxB";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        // person:researcher
        assertEquals("W. Stoeckenius", rec.getData().get(PERSON_RESEARCHER.toLowerCase()));
        // subject:topic
        assertTrue(rec.getData().get(SUBJECT_TOPIC.toLowerCase()).contains("response to chemical stimulus"));
        // note:methods
        assertEquals("Gustafsdottir et al. (doi:10.1371/journal.pone.0080999)", rec.getData().get(NOTE_METHODS.toLowerCase()));

        // note:descriptions
        assertEquals("This group of micrographs illustrate", rec.getData().get(NOTE_DESCRIPTION.toLowerCase()));

        // Identifier:Identifier:samplenumber
        assertEquals("test123", rec.getData().get(OBJECT_UNIQUE_ID.toLowerCase()));
        //assertEquals("test123", rec.getData().get(IDENTIFIER_SAMPLENUMBER.toLowerCase()));
    }
}
