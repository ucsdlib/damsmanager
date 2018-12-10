package edu.ucsd.library.xdre.harvesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
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
public class CilHavestingTest {

    private static final String RELATED_RESOURCE_RELATED = "Related resource:related";
    private static final String PERSON_RESEARCHER = "person:researcher";
    private static final String SUBJECT_TOPIC = "subject:topic";
    private static final String NOTE_METHODS = "Note:methods";
    private static final String SOURCEOFCONTRAST_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "SOURCEOFCONTRAST.onto_name";
    private static final String SOURCE_ATTRIBUTION_CONTRIBUTORS = FieldMappings.SOURCE_CORD_PREFFIX + "ATTRIBUTION.Contributors";
    private static final String SOURCE_BIOLOGICALPROCESS_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "BIOLOGICALPROCESS.onto_name";
    private static final String SOURCE_BIOLOGICALPROCESS_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "BIOLOGICALPROCESS.free_text";
    private static final String SOURCE_TECHNICALDETAILS_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "TECHNICALDETAILS.free_text";
    private static final String SOURCE_CITATION_DOI = "CIL_CCDB.Citation.DOI";
    private static final String SOURCE_ITEMTYPE_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "ITEMTYPE.onto_name";

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
    public void testExtractData() throws Exception {
        String[] files = {createJsonDataFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));
        TabularRecord rec = (TabularRecord) cilHarvesting.nextRecord();

        // constants metadata
        assertEquals("" + Calendar.getInstance().get(Calendar.YEAR), rec.getData().get(FieldMappings.DATE_ISSUED.toLowerCase()));
        assertEquals("data|still image", rec.getData().get(FieldMappings.TYPE_OF_RESOURCE.toLowerCase()));
        assertEquals("zxx  - No linguistic content; Not applicable", rec.getData().get(FieldMappings.LANGUAGE.toLowerCase()));

        // date created
        assertEquals("02/02/1958", rec.getData().get(FieldMappings.DATE_CREATION.toLowerCase()));
        assertEquals("1958-02-02", rec.getData().get(FieldMappings.BEGIN_DATE.toLowerCase()));
        // technical details
        String expectedResult = "Preparation: PREPARATION for sectioned tissue|PREPARATION a free test\r\n"
                + "Relation to intact cell: sectioned tissue|a free test|Source of contrast: differences in adsorption or binding of stain"
                + "|Item type: recorded image";
        assertEquals(expectedResult, rec.getData().get(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase()));
        assertEquals(expectedResult, rec.getData().get(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase()));
        // related resource
        expectedResult = "Source Record in the Cell Image Library @ https://doi.org/doi:10.7295/W9CIL37147";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        expectedResult = "George E. Palade EM Slide Collection @ http://cushing.med.yale.edu/gsdl/cgi-bin/library?c=palade&a=d&d=DpaladeFxB";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        // person:researcher
        assertEquals("W. Stoeckenius", rec.getData().get(PERSON_RESEARCHER.toLowerCase()));
        // subject:topic
        assertEquals("response to chemical stimulus|free text for response to chemical stimulus", rec.getData().get(SUBJECT_TOPIC.toLowerCase()));
        // note:methods
        assertEquals("Gustafsdottir et al. (doi:10.1371/journal.pone.0080999)", rec.getData().get(NOTE_METHODS.toLowerCase()));
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
        assertEquals("02/02/1958", doc.valueOf("//dams:Object/dams:date/dams:Date[dams:type='creation']//rdf:value"));
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
        assertTrue(csvValue.contains("test123,  \\Component,test123.json,data-service"));
    }

    @Test
    public void testCsvExportWithComponent() throws Exception {
        String[] files = {createJsonComponentFile("test123.json").getAbsolutePath()};
        CilHarvesting cilHarvesting = new CilHarvesting(fieldMappings, constantFields, Arrays.asList(files));

        String csvValue = cilHarvesting.toCSV(getResourceFile("dams42json.xsl").getAbsolutePath());
        assertTrue(csvValue.contains("Unique ID,Level,File name,File use"));
        assertTrue(csvValue.contains("test123,Object,,"));
        assertTrue(csvValue.contains("test123,  \\Component,BBBC022_v1_images_20585w1.zip,data-service"));
        assertTrue(csvValue.contains("test123,  \\Component,BBBC022_v1_images_20585w2.zip,data-service"));
        assertTrue(csvValue.contains("test123,  \\Component,37147.tif,image-source"));
        assertTrue(csvValue.contains("test123,  \\Component,37147.jpg,image-source"));
        assertTrue(csvValue.contains("test123,  \\Component,37147.zip,data-service"));
        assertTrue(csvValue.contains("test123,  \\Component,test123.json,data-service"));
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
        assertTrue(csvValue.contains("test123a,  \\Component,test123a.json,data-service"));
        assertTrue(csvValue.contains("test123b,Object,,"));
        assertTrue(csvValue.contains("test123b,  \\Component,test123b.json,data-service"));
        assertTrue(csvValue.contains("test123c,Object,,"));
        assertTrue(csvValue.contains("test123c,  \\Component,BBBC022_v1_images_20585w1.zip,data-service"));
        assertTrue(csvValue.contains("test123c,  \\Component,BBBC022_v1_images_20585w2.zip,data-service"));
        assertTrue(csvValue.contains("test123c,  \\Component,37147.tif,image-source"));
        assertTrue(csvValue.contains("test123c,  \\Component,37147.jpg,image-source"));
        assertTrue(csvValue.contains("test123c,  \\Component,37147.zip,data-service"));
        assertTrue(csvValue.contains("test123c,  \\Component,test123c.json,data-service"));
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
        assertEquals("02/02/1958", rec.getData().get(FieldMappings.DATE_CREATION.toLowerCase()));
        assertEquals("1958-02-02", rec.getData().get(FieldMappings.BEGIN_DATE.toLowerCase()));
        // technical details
        String expectedResult = "Preparation: PREPARATION for sectioned tissue|PREPARATION a free test\r\n"
                + "Relation to intact cell: sectioned tissue|a free test|Source of contrast: differences in adsorption or binding of stain"
                + "|Item type: recorded image";
        assertEquals(expectedResult, rec.getData().get(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase()));
        assertEquals(expectedResult, rec.getData().get(FieldMappings.NOTE_TECHNICAL_DETAILS.toLowerCase()));
        // related resource
        expectedResult = "Source Record in the Cell Image Library @ https://doi.org/doi:10.7295/W9CIL37147";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        expectedResult = "George E. Palade EM Slide Collection @ http://cushing.med.yale.edu/gsdl/cgi-bin/library?c=palade&a=d&d=DpaladeFxB";
        assertTrue(rec.getData().get(RELATED_RESOURCE_RELATED.toLowerCase()).contains(expectedResult));
        // person:researcher
        assertEquals("W. Stoeckenius", rec.getData().get(PERSON_RESEARCHER.toLowerCase()));
        // subject:topic
        assertEquals("response to chemical stimulus|free text for response to chemical stimulus", rec.getData().get(SUBJECT_TOPIC.toLowerCase()));
        // note:methods
        assertEquals("Gustafsdottir et al. (doi:10.1371/journal.pone.0080999)", rec.getData().get(NOTE_METHODS.toLowerCase()));
    }

    private File createJsonTestFile(String fileName) throws IOException {
        String json = "{\"CIL_CCDB\": {" +
            "\"CIL\": {" +
            "}," +
            "\"Citation\": {" +
                "\"DOI\": \"doi:10.7295/W9CIL37147\"," +
                "\"ARK\": \"ark:/b7295/w9cil37147\"," +
                "\"Title\": \"W. Stoeckenius (2011) CIL:37147. CIL. Dataset\"" +
            "}" +
        "}}";

        File file = new File(fileName);
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        }
 
        return file;
    }

    private File createJsonDataFile(String fileName) throws IOException {
        String json = "{\"CIL_CCDB\": {" +
        "\"CIL\": {" +
            "\"CORE\": {" +
                "\"TECHNICALDETAILS\": {" +
                        "\"free_text\": \"Gustafsdottir et al. (doi:10.1371/journal.pone.0080999)\"" +
                "}," +
                "\"CELLULARCOMPONENT\": [" +
                    "{" +
                        "\"onto_name\": \"membrane\"," +
                        "\"onto_id\": \"GO:0016020\"" +
                    "}," +
                    "{" +
                        "\"free_text\": \"artificial phospholipid membrane\"" +
                    "}" +
                "]," +
                "\"ATTRIBUTION\": {" +
                    "\"DATE\": [" +
                        "\"02/02/1958\"" +
                    "]," +
                    "\"URLs\": [" +
                        "{" +
                            "\"Label\": \"George E. Palade EM Slide Collection\"," +
                            "\"Href\": \"http://cushing.med.yale.edu/gsdl/cgi-bin/library?c=palade&a=d&d=DpaladeFxB\"" +
                        "}" +
                    "]," +
                    "\"Contributors\": [" +
                        "\"W. Stoeckenius\"" +
                    "]" +
                "}," +
                "\"SOURCEOFCONTRAST\": {" +
                    "\"onto_name\": \"differences in adsorption or binding of stain\"," +
                    "\"onto_id\": \"FBbi:00000598\"" +
                "}," +
                "\"PREPARATION\": {" +
                    "\"onto_name\": \"PREPARATION for sectioned tissue\"," +
                    "\"free_text\": \"PREPARATION a free test\"" +
                "}," +
                "\"RELATIONTOINTACTCELL\": {" +
                    "\"onto_name\": \"sectioned tissue\"," +
                    "\"free_text\": \"a free test\"," +
                    "\"onto_id\": \"FBbi:00000026\"" +
                "}," +
                "\"BIOLOGICALPROCESS\": {" +
                        "\"onto_name\": \"response to chemical stimulus\"," +
                        "\"free_text\": \"free text for response to chemical stimulus\"," +
                        "\"onto_id\": \"GO:0042221\"" +
                "}," +
                "\"TERMSANDCONDITIONS\": {" +
                    "\"free_text\": \"attribution_nc_sa\"" +
                "}," +
                "\"ITEMTYPE\": {" +
                    "\"onto_name\": \"recorded image\"," +
                    "\"onto_id\": \"FBbi:00000265\"" +
                "}" +
            "}," +
        "}," +
        "\"Citation\": {" +
            "\"DOI\": \"doi:10.7295/W9CIL37147\"," +
            "\"ARK\": \"ark:/b7295/w9cil37147\"," +
            "\"Title\": \"W. Stoeckenius (2011) CIL:37147. CIL. Dataset\"" +
        "}" +
        "}}";

        File file = new File(fileName);
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        }
 
        file.deleteOnExit();
        return file;
    }

    private File createJsonComponentFile(String fileName) throws IOException {
        String json = "{\"CIL_CCDB\": {" +
        "\"CIL\": {" +
            "\"Image_files\": [" +
                "{" +
                    "\"Mime_type\": \"image/tif\"," +
                    "\"File_type\": \"OME_tif\"," +
                    "\"File_path\": \"37147.tif\"," +
                    "\"Size\": 63800000" +
                "}," +
                "{" +
                    "\"Mime_type\": \"image/jpeg; charset=utf-8\"," +
                    "\"File_type\": \"Jpeg\"," +
                    "\"File_path\": \"37147.jpg\"," +
                    "\"Size\": 4866564" +
                "}," +
                "{" +
                    "\"Mime_type\": \"application/zip\"," +
                    "\"File_type\": \"Zip\"," +
                    "\"File_path\": \"37147.zip\"," +
                    "\"Size\": 63726850" +
                "}" +
            "]," +
            "\"Alternative_image_files\": [" +
                "{" +
                    "\"Mime_type\": \"application/zip\"," +
                    "\"File_type\": \"Zip\"," +
                    "\"File_path\": \"BBBC022_v1_images_20585w1.zip\"," +
                    "\"URL_postfix\": \"/broad_data/plate_20585/BBBC022_v1_images_20585w1.zip\"," +
                    "\"Size\": 1383949189" +
                "}," +
                "{" +
                    "\"Mime_type\": \"application/zip\"," +
                    "\"File_type\": \"Zip\"," +
                    "\"File_path\": \"BBBC022_v1_images_20585w2.zip\"," +
                    "\"URL_postfix\": \"/broad_data/plate_20585/BBBC022_v1_images_20585w2.zip\"," +
                    "\"Size\": 1620971171" +
                "}" +
            "]" +
        "}," +
        "\"Citation\": {" +
            "\"DOI\": \"doi:10.7295/W9CIL37147\"," +
            "\"ARK\": \"ark:/b7295/w9cil37147\"," +
            "\"Title\": \"W. Stoeckenius (2011) CIL:37147. CIL. Dataset\"" +
        "}" +
        "}}";

        File file = new File(fileName);
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        }

        return file;
    }

    private Map<String, List<String>> initiateFiledMappings() {
        Map<String, List<String>> fieldMappings = new HashMap<>();

        String[] titleFields = {FieldMappings.TITLE};
        fieldMappings.put(FieldMappings.SOURCE_CITATION_TITLE.toLowerCase(), Arrays.asList(titleFields));

        String[] dateFields = {FieldMappings.DATE_CREATION, FieldMappings.BEGIN_DATE, FieldMappings.END_DATE};
        fieldMappings.put(FieldMappings.SOURCE_ATTRIBUTION_DTAE.toLowerCase(), Arrays.asList(dateFields));

        String[] tenicalDetailsFields = {FieldMappings.NOTE_TECHNICAL_DETAILS};
        fieldMappings.put(FieldMappings.SOURCE_PREPARATION_ONTO_NAME.toLowerCase(), Arrays.asList(tenicalDetailsFields));
        fieldMappings.put(FieldMappings.SOURCE_RELATIONTOINTACTCELL_ONTO_NAME.toLowerCase(), Arrays.asList(tenicalDetailsFields));
        fieldMappings.put(SOURCEOFCONTRAST_ONTO_NAME.toLowerCase(), Arrays.asList(tenicalDetailsFields));
        fieldMappings.put(SOURCE_ITEMTYPE_ONTO_NAME.toLowerCase(), Arrays.asList(tenicalDetailsFields));

        String[] relatedResourceFields = {RELATED_RESOURCE_RELATED};
        fieldMappings.put(SOURCE_CITATION_DOI.toLowerCase(), Arrays.asList(relatedResourceFields));
        fieldMappings.put(FieldMappings.SOURCE_ATTRIBUTION_URLS_LABEL.toLowerCase(), Arrays.asList(relatedResourceFields));
        fieldMappings.put(FieldMappings.SOURCE_ATTRIBUTION_URLS_HREF.toLowerCase(), Arrays.asList(relatedResourceFields));

        String[] researcherFields = {PERSON_RESEARCHER};
        fieldMappings.put(SOURCE_ATTRIBUTION_CONTRIBUTORS.toLowerCase(), Arrays.asList(researcherFields));

        String[] topicFields = {SUBJECT_TOPIC};
        fieldMappings.put(SOURCE_BIOLOGICALPROCESS_ONTO_NAME.toLowerCase(), Arrays.asList(topicFields));
        fieldMappings.put(SOURCE_BIOLOGICALPROCESS_FREE_TEXT.toLowerCase(), Arrays.asList(topicFields));

        String[] noteMethodFields = {NOTE_METHODS};
        fieldMappings.put(SOURCE_TECHNICALDETAILS_FREE_TEXT.toLowerCase(), Arrays.asList(noteMethodFields));

        String[] fileNameFields = {FieldMappings.FILE_NAME};
        fieldMappings.put(FieldMappings.SOURCE_IMAGE_FILE_PATH.toLowerCase(), Arrays.asList(fileNameFields));

        String[] fileUseFields = {FieldMappings.FILE_USE, FieldMappings.TITLE};
        fieldMappings.put(FieldMappings.SOURCE_IMAGE_FILE_TYPE.toLowerCase(), Arrays.asList(fileUseFields));

        String[] altFilePathFields = {FieldMappings.FILE_NAME, FieldMappings.TITLE};
        fieldMappings.put(FieldMappings.SOURCE_ALTERNATIVE_IMAGE_FILE_PATH.toLowerCase(), Arrays.asList(altFilePathFields));

        String[] altFileUseFields = {FieldMappings.FILE_USE};
        fieldMappings.put(FieldMappings.SOURCE_ALTERNATIVE_IMAGE_FILE_TYPE.toLowerCase(), Arrays.asList(altFileUseFields));

        return fieldMappings;
    }

    private File getResourceFile(String fileName) throws IOException {
        File xslFile = new File(fileName);
        xslFile.deleteOnExit();

        byte[] buf = new byte[4096];
        try(InputStream in = getClass().getResourceAsStream("/resources/" + fileName);
                FileOutputStream out = new FileOutputStream(xslFile)) {

            int bytesRead = 0;
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }
        return xslFile;
    }

    private Map<String, String> initiateConstantsFileds() {
        Map<String, String> constantsFields = new HashMap<String, String>();
        constantsFields.put(FieldMappings.DATE_ISSUED, "" + Calendar.getInstance().get(Calendar.YEAR));
        constantsFields.put(FieldMappings.TYPE_OF_RESOURCE.toLowerCase(), "data|still image");
        constantsFields.put(FieldMappings.LANGUAGE.toLowerCase(), "zxx  - No linguistic content; Not applicable");
        return constantsFields;
    }

    private TabularRecord getComponentByFileName(TabularRecord record, String fileName) {
        for (TabularRecord comp : record.getComponents()) {
            if (comp.getData().get(FieldMappings.FILE_NAME.toLowerCase()).equalsIgnoreCase(fileName)) {
                return comp;
            }
        }
        return null;
    }
}
