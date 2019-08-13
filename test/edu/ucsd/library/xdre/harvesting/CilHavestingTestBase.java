package edu.ucsd.library.xdre.harvesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.library.xdre.tab.TabularRecord;

/**
 * Test methods for CilHavesting class
 * @author lsitu
 *
 */
public abstract class CilHavestingTestBase {

    protected static final String OBJECT_UNIQUE_ID = "object Unique ID";
    protected static final String RELATED_RESOURCE_RELATED = "Related resource:related";
    protected static final String PERSON_RESEARCHER = "person:researcher";
    protected static final String SUBJECT_TOPIC = "subject:topic";
    protected static final String SUBJECT_ANATOMY = "subject:anatomy";
    protected static final String NOTE_METHODS = "Note:methods";
    protected static final String NOTE_DESCRIPTION = "Note:description";
    protected static final String SOURCE_IDENTIFIER = "identifier / json file name for object";
    protected static final String SOURCEOFCONTRAST_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "SOURCEOFCONTRAST.onto_name";
    protected static final String SOURCE_ATTRIBUTION_CONTRIBUTORS = FieldMappings.SOURCE_CORD_PREFFIX + "ATTRIBUTION.Contributors";
    protected static final String SOURCE_CELLULARCOMPONENT_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "CELLULARCOMPONENT.onto_name";
    protected static final String SOURCE_CELLULARCOMPONENT_ONTO_ID = FieldMappings.SOURCE_CORD_PREFFIX + "CELLULARCOMPONENT.onto_id";
    protected static final String SOURCE_CELLULARCOMPONENT_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "CELLULARCOMPONENT.free_text";
    protected static final String SOURCE_BIOLOGICALPROCESS_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "BIOLOGICALPROCESS.onto_name";
    protected static final String SOURCE_BIOLOGICALPROCESS_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "BIOLOGICALPROCESS.free_text";
    protected static final String SOURCE_TECHNICALDETAILS_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "TECHNICALDETAILS.free_text";
    protected static final String SOURCE_IMAGEDESCRIPTION_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "IMAGEDESCRIPTION.free_text";
    protected static final String SOURCE_CITATION_DOI = "CIL_CCDB.Citation.DOI";
    protected static final String SOURCE_ITEMTYPE_ONTO_NAME = FieldMappings.SOURCE_CORD_PREFFIX + "ITEMTYPE.onto_name";
    protected static final String SOURCE_TERMSANDCONDITIONS_FREE_TEXT = FieldMappings.SOURCE_CORD_PREFFIX + "TERMSANDCONDITIONS.free_text";

    protected File createJsonTestFile(String fileName) throws IOException {
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
        file.deleteOnExit();

        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        }
 
        return file;
    }

    protected File createJsonDataFile(String fileName) throws IOException {
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
                        "\"W. Stoeckenius\", \"Wolfgang Bettighofer\", \"2011 Olympus Competition®\", \"Buchanan, JoAnn (Stanford) (specimen prep); Richard Allen (University of Hawaii)\"" +
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
                "\"IMAGEDESCRIPTION\": {" +
                            "\"free_text\": \"This group of micrographs illustrate\"" +
                 "}," +
                "\"TERMSANDCONDITIONS\": {" +
                    "\"free_text\": \"attribution_nc_sa\"" +
                "}," +
                "\"ITEMTYPE\": [{" +
                    "\"onto_name\": \"recorded image\"," +
                    "\"onto_id\": \"FBbi:00000265\"" +
                "},{" +
                    "\"onto_name\": \"still image\"," +
                    "\"onto_id\": \"FBbi:00000265\"" +
                "},{" +
                    "\"free_text\": \"free text image\"" +
                "}]" +
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

    protected File createJsonComponentFile(String fileName) throws IOException {
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
        file.deleteOnExit();

        return file;
    }

    protected File createJsonNoTitleFile(String fileName) throws IOException {
        String json = "{\"CIL_CCDB\": {" +
            "\"CIL\": {" +
            "}," +
            "\"Citation\": {" +
                "\"DOI\": \"doi:10.7295/W9CIL37147\"," +
                "\"ARK\": \"ark:/b7295/w9cil37147\"," +
            "}" +
        "}}";

        File file = new File(fileName);
        file.deleteOnExit();

        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        }
 
        return file;
    }

    protected Map<String, List<String>> initiateFiledMappings() {
        Map<String, List<String>> fieldMappings = new HashMap<>();

        String[] titleFields = {FieldMappings.TITLE, FieldMappings.NOTE_PREFERRED_CITATION};
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

        String[] anatomyFields = {SUBJECT_ANATOMY};
        fieldMappings.put(SOURCE_CELLULARCOMPONENT_ONTO_NAME.toLowerCase(), Arrays.asList(anatomyFields));
        fieldMappings.put(SOURCE_CELLULARCOMPONENT_FREE_TEXT.toLowerCase(), Arrays.asList(anatomyFields));

        String[] descriptionFields = {NOTE_DESCRIPTION};
        fieldMappings.put(SOURCE_IMAGEDESCRIPTION_FREE_TEXT.toLowerCase(), Arrays.asList(descriptionFields));

        String[] noteMethodFields = {NOTE_METHODS};
        fieldMappings.put(SOURCE_TECHNICALDETAILS_FREE_TEXT.toLowerCase(), Arrays.asList(noteMethodFields));

        String[] identifierFields = {OBJECT_UNIQUE_ID, FieldMappings.IDENTIFIER_SAMPLENUMBER};
        fieldMappings.put(SOURCE_IDENTIFIER.toLowerCase(), Arrays.asList(identifierFields));

        String[] copyrightNoteFields = {TabularRecord.COPYRIGHT_NOTE};
        fieldMappings.put(SOURCE_TERMSANDCONDITIONS_FREE_TEXT.toLowerCase(), Arrays.asList(copyrightNoteFields));

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

    protected File getResourceFile(String fileName) throws IOException {
        File resourceFile = new File(fileName);
        resourceFile.deleteOnExit();

        byte[] buf = new byte[4096];
        try(InputStream in = getClass().getResourceAsStream("/resources/" + fileName);
                FileOutputStream out = new FileOutputStream(resourceFile)) {

            int bytesRead = 0;
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }
        return resourceFile;
    }

    protected Map<String, String> initiateConstantsFileds() {
        Map<String, String> constantsFields = new HashMap<String, String>();
        constantsFields.put(FieldMappings.DATE_ISSUED, "" + Calendar.getInstance().get(Calendar.YEAR));
        constantsFields.put(FieldMappings.TYPE_OF_RESOURCE.toLowerCase(), "data|still image");
        constantsFields.put(FieldMappings.LANGUAGE.toLowerCase(), "zxx  - No linguistic content; Not applicable");
        return constantsFields;
    }

    protected TabularRecord getComponentByFileName(TabularRecord record, String fileName) {
        for (TabularRecord comp : record.getComponents()) {
            if (comp.getData().get(FieldMappings.FILE_NAME.toLowerCase()).equalsIgnoreCase(fileName)) {
                return comp;
            }
        }
        return null;
    }

    protected String getFileContent(File file) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Reader in = new FileReader(file); BufferedReader bf = new BufferedReader(in);) {
            String line = null;
            while ((line = bf.readLine()) != null) {
                contentBuilder.append(line + "\n");
            }
        }

        return contentBuilder.toString();
    }
}
