package edu.ucsd.library.xdre.tab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * Test methods for TabularEditRecord class
 * @author lsitu
 *
 */
public class BatchEditExcelSourceTest extends TabularRecordTestBasic {

    private File xlsInputTemplate = null;
    private File xlsInputTestFile = null;
    private BatchEditExcelSource excelSource = null;

    @Before
    public void init() throws Exception {
        Constants.DAMS_STORAGE_URL = "http://localhost:8080/dams/api";
        Constants.DAMS_ARK_URL_BASE = "http://library.ucsd.edu/ark:";
        Constants.ARK_ORG = "20775";
        Constants.BATCH_ADDITIONAL_FIELDS = "Note:local attribution,collection(s),unit";

        xlsInputTemplate = getResourceFile("xls_standard_input_template.xlsx");
        xlsInputTestFile = getResourceFile("xls_batch_edit_input_test.xlsx");
        BatchEditExcelSource.initControlValues(xlsInputTemplate);

        excelSource = new BatchEditExcelSource(xlsInputTestFile, Arrays.asList(ExcelSource.IGNORED_FIELDS_FOR_OBJECTS));
    }

    @Test
    public void testReportInvalidHeader() throws Exception {
        assertEquals("Invalid header isn't reported!", 1, excelSource.getInvalidColumns().size());

        List<String> invalidHeaders = excelSource.getInvalidColumns();
        assertEquals("Invalid header anyHeader doesn't match!", "anyHeader", invalidHeaders.get(0));
    }

    @Test
    public void testReportInvalidValue() throws Exception {
        assertEquals("Invalid values aren't reported!", 1, excelSource.getInvalidValues().size());

        Map<String, String> invalidFields = excelSource.getInvalidValues().get(0);
        assertTrue("Invalid license:beginDate doesn't match!", invalidFields.containsKey("license:beginDate"));
        assertTrue("Invalid license:endDate doesn't match!", invalidFields.containsKey("license:endDate"));
    }

    @Test
    public void testNextRecord() throws Exception {
        TabularRecord editRecord = (TabularRecord)excelSource.nextRecord();

        assertNotNull(editRecord);
        // record id
        assertEquals("Record ID doesn't match!", "zzxxxxxxxx", editRecord.recordID());

        Map<String, String> data = editRecord.getData();

        // title
        assertEquals("Title doesn't match!", "TEST Person:Advisor", data.get("title"));

        //Type of Resource
        assertEquals("Type of Resource doesn't match!", "still image", data.get("type of resource"));

        //Language
        assertEquals("Language doesn't match!", "zxx  - No linguistic content; Not applicable",
                data.get("language"));

        // Note:description
        assertEquals("Note:description doesn't match!", "TEST", data.get("note:description"));

        //Note:statement of responsibility
        assertEquals("Note:statement of responsibility doesn't match!", "statement of responsibility",
                data.get("note:statement of responsibility"));

        // Person:Advisor
        assertEquals("Person:Advisor doesn't match!", "Reser, Greg", data.get("person:advisor"));

        // Date:creation
        assertEquals("Date:creation doesn't match!", "2019-04-04", data.get("date:creation"));

        // Begin date
        assertEquals("Begin date doesn't match!", "2019", data.get("begin date"));

        // End date
        assertEquals("End date doesn't match!", "2019", data.get("end date"));

        // Subject:topic, Subject:topic FAST
        assertEquals("Subject:topic | Subject:topic FAST doesn't match!", "Subject:topic|Subject:topic FAST", data.get("subject:topic"));

        // Subject:genre FAST
        assertEquals("Subject:genre FAST doesn't match!", "Subject:genre FAST", data.get("subject:genre"));

        // Subject:anatomy
        assertEquals("Subject:anatomy doesn't match!", "Subject:anatomy", data.get("subject:anatomy"));

        // Related resource:related
        assertEquals("Related resource:related doesn't match!", "Related resource:related@http://example.com/uri",
                data.get("related resource:related"));

        // copyrightJurisdiction
        assertEquals("copyrightJurisdiction doesn't match!", "CopyrightJurisdiction", data.get("copyrightjurisdiction"));

        // copyrightStatus
        assertEquals("copyrightStatus doesn't match!", "CopyrightStatus", data.get("copyrightstatus"));

        // copyrightPurposeNote
        assertEquals("copyrightPurposeNote doesn't match!", "CopyrightPurposeNote", data.get("copyrightpurposenote"));

        // copyrightNote
        assertEquals("copyrightNote doesn't match!", "CopyrightNote", data.get("copyrightnote"));

        // rightsHolderPersonal
        assertEquals("rightsHolderPersonal doesn't match!", "RightsHolderPersonal", data.get("rightsholderpersonal"));

        // rightsHolderCorporate
        assertEquals("rightsHolderCorporate doesn't match!", "RightsHolderCorporate", data.get("rightsholdercorporate"));

        // otherRights:otherRightsBasis
        assertEquals("otherRights:otherRightsBasis doesn't match!", "OtherRights:otherRightsBasis", data.get("otherrights:otherrightsbasis"));

        // otherRights:permission/type
        assertEquals("otherRights:permission/type doesn't match!", "OtherRights:permission/type", data.get("otherrights:permission/type"));

        // otherRights:restriction/type
        assertEquals("otherRights:restriction/type doesn't match!", "OtherRights:restriction/type", data.get("otherrights:restriction/type"));

        // otherRights:permission/type
        assertEquals("otherRights:permission/type doesn't match!", "OtherRights:permission/type", data.get("otherrights:permission/type"));

        // otherRights:otherRightsNote
        assertEquals("otherRights:otherRightsNote doesn't match!", "OtherRights:otherRightsNote", data.get("otherrights:otherrightsnote"));

        // license:permission/type
        assertEquals("license:permission/type doesn't match!", "License:permission/type", data.get("license:permission/type"));

        // license:restriction/type
        assertEquals("license:restriction/type doesn't match!", "License:restriction/type", data.get("license:restriction/type"));

        // license:beginDate
        assertEquals("license:beginDate doesn't match!", "License:beginDate", data.get("license:begindate"));

        // license:endDate
        assertEquals("license:endDate doesn't match!", "License:endDate", data.get("license:enddate"));

        // license:licenseNote
        assertEquals("license:licenseNote doesn't match!", "License:licenseNote", data.get("license:licensenote"));

        // license:licenseURI
        assertEquals("license:licenseURI doesn't match!", "License:licenseURI", data.get("license:licenseuri"));

        // collection(s)
        assertEquals("collection(s) doesn't match!", "Collection(s)", data.get("collection(s)"));

        // unit
        assertEquals("Unit doesn't match!", "Unit", data.get("unit"));

        // Component
        List<TabularRecord> comps = editRecord.getComponents();
        assertEquals("component size doesn't match!", 1, comps.size());
        Map<String, String> compData = comps.get(0).getData();
        assertEquals("component title doesn't match!", "Test Component 1", compData.get("title"));

        // Sub-Component
        List<TabularRecord> subComps = comps.get(0).getComponents();
        assertEquals("sub-component size doesn't match!", 1, subComps.size());
        Map<String, String> subCompData = subComps.get(0).getData();
        assertEquals("sub-component title doesn't match!", "Test Sub-Component 1", subCompData.get("title"));
    }
}
