package edu.ucsd.library.xdre.tab;

import static edu.ucsd.library.xdre.tab.TabularRecordBasic.addElement;
import static edu.ucsd.library.xdre.tab.TabularRecordBasic.addTextElement;
import static edu.ucsd.library.xdre.tab.TabularRecordBasic.damsNS;
import static edu.ucsd.library.xdre.tab.TabularRecordBasic.rdfNS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.junit.Before;
import org.junit.Test;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * Test methods for TabularEditRecord class
 * @author lsitu
 *
 */
public class TabularEditRecordTest extends TabularRecordTestBasic {

    @Before
    public void init() throws LoginException, IOException {
        Constants.DAMS_STORAGE_URL = "http://localhost:8080/dams/api";
        Constants.DAMS_ARK_URL_BASE = "http://library.ucsd.edu/ark:";
        Constants.ARK_ORG = "20775";
        Constants.BATCH_ADDITIONAL_FIELDS = "Note:local attribution,collection(s)";
    }

    @Test
    public void testOverlayTitle() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        String overlayTitle = getOverlayValue(title);
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", overlayTitle);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document docEdited = testObject.toRDFXML();

        // validate title is edited
        List<Node> titleNodes = docEdited.selectNodes("//mads:Title");
        assertEquals("Title size doesn't match!", 1, titleNodes.size());

        String actualResult = titleNodes.get(0).selectSingleNode("mads:authoritativeLabel").getText();
        assertEquals("Title doesn't match!", overlayTitle, actualResult);
    }

    @Test
    public void testOverlayLanguage() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        String overlayTitle = getOverlayValue(title);
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for note
        String language = "xxx - Language xxx";
        data.put("language", language);
        String languageOverlay = getOverlayValue(language);
        overlayData.put("language", languageOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document docEdited = testObject.toRDFXML();

        // validate title is edited
        List<Node> languageNodes = docEdited.selectNodes("//mads:Language");
        assertEquals("Language size doesn't match!", 1, languageNodes.size());

        String actualResult = languageNodes.get(0).selectSingleNode("mads:authoritativeLabel").getText();
        assertTrue("Language doesn't match!", languageOverlay.endsWith(actualResult));

        actualResult = languageNodes.get(0).selectSingleNode("mads:code").getText();
        assertEquals("Language code doesn't match!", "xxx", actualResult);
    }

    @Test
    public void testOverlayNote() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for note
        String note = "Note:note";
        data.put("note:note", note);
        String noteOverlay = getOverlayValue(note);
        overlayData.put("note:note", noteOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:Note[dams:type='note']");
        assertEquals("The size of note doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("rdf:value").getText();
        assertEquals("Note value doesn't match!", noteOverlay, actualResult);
    }

    @Test
    public void testOverlayIdentifier() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for identifier
        String identifier = "Identifier:Identifier";
        data.put("identifier:identifier", identifier);
        String identifierOverlay = getOverlayValue(identifier);
        overlayData.put("identifier:identifier", identifierOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:Note[dams:type='identifier']");
        assertEquals("The size of the identifier note doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("rdf:value").getText();
        assertEquals("Identifier value doesn't match!", identifierOverlay, actualResult);
    }

    @Test
    public void testOverlayRelationship() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator
        String name = "Person:Creator";
        data.put("person:creator", name);
        String nameOverlay = getOverlayValue(name);;
        overlayData.put("person:creator", getOverlayValue(name));

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        Node node = doc.selectSingleNode("//dams:relationship/dams:Relationship");

        String actualNameResult = node.selectSingleNode("//mads:PersonalName/mads:authoritativeLabel").getText();
        assertEquals("Name value doesn't match!", nameOverlay, actualNameResult);
        String actualRoleResult = node.selectSingleNode("dams:role/mads:Authority/mads:authoritativeLabel").getText();
        assertEquals("Role value doesn't match!", "creator", actualRoleResult);
    }

    @Test
    public void testOverlayMadsSubject() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for mads subject
        String subject = "Subject:topic";
        data.put("subject:topic", subject);
        String overlayTopic = getOverlayValue(subject);
        overlayData.put("subject:topic", overlayTopic);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:topic/mads:Topic");
        assertEquals("The size of Topic doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("mads:authoritativeLabel").getText();
        assertEquals("Topic value doesn't match!", overlayTopic, actualResult);
    }

    @Test
    public void testOverlayDamsSubject() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data with dams subject
        String subject = "Subject:anatomy";
        data.put("subject:anatomy", subject);
        String overlaySubject = getOverlayValue(subject);
        overlayData.put("subject:anatomy", overlaySubject);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();
        List<Node> nodes = doc.selectNodes("//dams:anatomy/dams:Anatomy");
        assertEquals("The size of subject anatomy doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("mads:authoritativeLabel").getText();
        assertEquals("Subject anatomy doesn't match!", overlaySubject, actualResult);
    }

    @Test
    public void testOverlayRelatedResource() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for related resource
        String relatedResource = "Related resource:related@uri";
        data.put("related resource:related", relatedResource);
        String relatedResourceOverlay = getOverlayValue(relatedResource);
        overlayData.put("related resource:related", relatedResourceOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:RelatedResource");
        assertEquals("The size of Related Resource doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("dams:description").getText();
        assertEquals("Related Resource description doesn't match!", "Related resource:related", actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:uri/@rdf:resource").getStringValue();
        assertTrue("Related Resource uri doesn't match!", relatedResourceOverlay.endsWith(actualResult));

        actualResult = nodes.get(0).selectSingleNode("dams:type").getText();
        assertEquals("Related Resource type doesn't match!", "related", actualResult);
    }


    @Test
    public void testOverlayCartographics() throws Exception {
        // Initiate tabular data for edit
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for cartographics
        String line  = "Geographic:line";
        String point  = "Geographic:point";
        String polygon  = "Geographic:polygon";
        String projection  = "Geographic:projection";
        String reference   = "Geographic:reference system";
        String scale = "Geographic:scale";
        data.put("geographic:line", line);
        data.put("geographic:point", point);
        data.put("geographic:polygon", polygon);
        data.put("geographic:projection", projection);
        data.put("geographic:reference system", reference);
        data.put("geographic:scale", scale);

        String lineOverlay  = getOverlayValue(line);
        String pointOverlay  = getOverlayValue(point);
        String polygonOverlay = getOverlayValue(polygon);
        String projectionOverlay  = getOverlayValue(projection);
        String referenceOverlay  = getOverlayValue(reference);
        String scaleOverlay  = getOverlayValue(scale);
        overlayData.put("geographic:line", lineOverlay);
        overlayData.put("geographic:point", pointOverlay);
        overlayData.put("geographic:polygon", polygonOverlay);
        overlayData.put("geographic:projection", projectionOverlay);
        overlayData.put("geographic:reference system", referenceOverlay);
        overlayData.put("geographic:scale", scaleOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:Cartographics");
        assertEquals("The size of Cartographics doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("dams:line").getText();
        assertEquals("Cartographics line doesn't match!", lineOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:point").getText();
        assertEquals("Cartographics point doesn't match!", pointOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:polygon").getText();
        assertEquals("Cartographics polygon doesn't match!", polygonOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:projection").getText();
        assertEquals("Cartographics projection doesn't match!", projectionOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:referenceSystem").getText();
        assertEquals("Cartographics reference system doesn't match!", referenceOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:scale").getText();
        assertEquals("Cartographics scale doesn't match!", scaleOverlay, actualResult);
    }

    @Test
    public void testOverlayCopyright() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        String copyrightJurisdiction = "CopyrightJurisdiction";
        String copyrightStatus = "CopyrightStatus";
        String copyrightPurposeNote = "CopyrightPurposeNote";
        String copyrightNote = "CopyrightNote";
        addTextElement(obj, "copyrightJurisdiction", damsNS, copyrightJurisdiction);
        addTextElement(obj, "copyrightStatus", damsNS, copyrightStatus);
        addTextElement(obj, "copyrightPurposeNote", damsNS, copyrightPurposeNote);
        addTextElement(obj, "copyrightNote", damsNS, copyrightNote);

        String copyrightJurisdictionOverlay = getOverlayValue(copyrightJurisdiction);
        String copyrightStatusOverlay = getOverlayValue(copyrightStatus);
        String copyrightPurposeNoteOverlay = getOverlayValue(copyrightPurposeNote);
        String copyrightNoteOverlay = getOverlayValue(copyrightNote);
        Map<String, String> overlayData = createDataWithTitle(objArk, title);
        overlayData.put("copyrightjurisdiction", copyrightJurisdictionOverlay);
        overlayData.put("copyrightstatus", copyrightStatusOverlay);
        overlayData.put("copyrightpurposenote", copyrightPurposeNoteOverlay);
        overlayData.put("copyrightnote", copyrightNoteOverlay);

        // Create record with data overlay: copyrightJurisdiction, copyrightStatus, copyrightPurposeNote, copyrightNote
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), overlayData);
        Document doc = testObject.toRDFXML();
        List<Node> nodes = doc.selectNodes("//dams:Copyright");
        assertEquals("The size of Copyright doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("dams:copyrightJurisdiction").getText();
        assertEquals("CopyrightJurisdiction doesn't match!", copyrightJurisdictionOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:copyrightStatus").getText();
        assertEquals("copyrightStatus doesn't match!", copyrightStatusOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:copyrightPurposeNote").getText();
        assertEquals("copyrightPurposeNote doesn't match!", copyrightPurposeNoteOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:copyrightNote").getText();
        assertEquals("copyrightNote doesn't match!", copyrightNoteOverlay, actualResult);
    }

    @Test
    public void testOverlayRightsHolderPersonal() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        String rightsHolderPersonal = "RightsHolderPersonal";
        addTextElement(obj, "rightsHolderPersonal", damsNS, rightsHolderPersonal);

        String rightsHolderPersonalOverlay = getOverlayValue(rightsHolderPersonal);
        Map<String, String> overlayData = createDataWithTitle(objArk, title);
        overlayData.put("rightsholderpersonal", rightsHolderPersonalOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:rightsHolderPersonal/mads:PersonalName");
        assertEquals("The size of rightsHolderPersonal doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("mads:authoritativeLabel").getText();
        assertEquals("RightsHolder PersonalName doesn't match!", rightsHolderPersonalOverlay, actualResult);
    }

    @Test
    public void testOverlayRightsHolderCorporate() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        String rightsHolderCorporate = "RightsHolderCorporate";
        addTextElement(obj, "rightsHolderCorporate", damsNS, rightsHolderCorporate);

        String rightsHolderCorporateOverlay = getOverlayValue(rightsHolderCorporate);
        Map<String, String> overlayData = createDataWithTitle(objArk, title);
        overlayData.put("rightsholdercorporate", rightsHolderCorporateOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:rightsHolderCorporate/mads:CorporateName");
        assertEquals("The size of rightsHolderPersonal doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("mads:authoritativeLabel").getText();
        assertEquals("RightsHolder CorporateName doesn't match!", rightsHolderCorporateOverlay, actualResult);
    }

    @Test
    public void testOverlayOtherRights() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);

        String otherRightsBasis = "OtherRights:otherRightsBasis";
        String permissionType = "otherRights:permission/type";
        String restrictionType = "otherRights:restriction/type";
        String otherRightsNote = "otherRights:otherRightsNote";
        Element e = addElement(obj, "otherRights", damsNS, "OtherRights", damsNS);
        addTextElement(e, "otherRightsBasis", damsNS, otherRightsBasis);
        addTextElement(e, "otherRightsNote", damsNS, otherRightsNote);

        Element el = addElement(e, "permission", damsNS, "Permission", damsNS);
        addTextElement(el, "type", damsNS, permissionType);
        el = addElement(e, "restriction", damsNS, "Restriction", damsNS);
        addTextElement(el, "type", damsNS, restrictionType);

        String otherRightsBasisOverlay = getOverlayValue(otherRightsBasis);
        String permissionTypeOverlay = getOverlayValue(permissionType);
        String restrictionTypeOverlay = getOverlayValue(restrictionType);
        String otherRightsNoteOverlay = getOverlayValue(otherRightsNote);
        Map<String, String> overlayData = createDataWithTitle(objArk, title);
        overlayData.put("otherrights:otherrightsbasis", otherRightsBasisOverlay);
        overlayData.put("otherrights:permission/type", permissionTypeOverlay);
        overlayData.put("otherrights:restriction/type", restrictionTypeOverlay);
        overlayData.put("otherrights:otherrightsnote", otherRightsNoteOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:OtherRights");
        assertEquals("The size of OtherRights doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("dams:otherRightsBasis").getText();
        assertEquals("OtherRights otherRightsBasis doesn't match!", otherRightsBasisOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:otherRightsNote").getText();
        assertEquals("OtherRights otherRightsNote doesn't match!", otherRightsNoteOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:permission//dams:type").getText();
        assertEquals("OtherRights permission type doesn't match!", permissionTypeOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:restriction//dams:type").getText();
        assertEquals("OtherRights restriction type doesn't match!", restrictionTypeOverlay, actualResult);
    }

    @Test
    public void testOverlayLicense() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        // "license:permission/type", "license:restriction/type", "license:beginDate", "license:endDate", "license:licenseNote", "license:licenseURI"
        String permissionType = "license:permission/type";
        String restrictionType = "license:restriction/type";
        String licenseNote = "license:licenseNote";
        String licenseURI = "license:licenseURI";
        String beginDate = "license:beginDate";
        String endDate = "license:endDate";
        Element e = addElement(obj, "license", damsNS, "License", damsNS);
        addTextElement(e, "licenseNote", damsNS, licenseNote);
        addTextElement(e, "licenseURI", damsNS, licenseURI);
        addTextElement(e, "beginDate", damsNS, beginDate);
        addTextElement(e, "endDate", damsNS, endDate);

        Element el = addElement(e, "permission", damsNS, "Permission", damsNS);
        addTextElement(el, "type", damsNS, permissionType);
        addTextElement(el, "type", damsNS, restrictionType);

        String permissionTypeOverlay = getOverlayValue(permissionType);
        String restrictionTypeOverlay = getOverlayValue(restrictionType);
        String licenseNoteOverlay = getOverlayValue(licenseNote);
        String licenseURIOverlay = getOverlayValue(licenseURI);
        String beginDateOverlay = getOverlayValue(beginDate);
        String endDateOverlay = getOverlayValue(endDate);
        Map<String, String> overlayData = createDataWithTitle(objArk, title);
        overlayData.put("license:permission/type", permissionTypeOverlay);
        overlayData.put("license:restriction/type", restrictionTypeOverlay);
        overlayData.put("license:licensenote", licenseNoteOverlay);
        overlayData.put("license:licenseuri", licenseURIOverlay);
        overlayData.put("license:begindate", beginDateOverlay);
        overlayData.put("license:enddate", endDateOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:License");
        assertEquals("The size of License doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("dams:permission//dams:type").getText();
        assertEquals("License permission type doesn't match!", permissionTypeOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:restriction//dams:type").getText();
        assertEquals("License restriction type doesn't match!", restrictionTypeOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:licenseNote").getText();
        assertEquals("License licenseNote doesn't match!", licenseNoteOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("dams:licenseURI").getText();
        assertEquals("License licenseURI doesn't match!", licenseURIOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("//dams:Permission/dams:beginDate").getText();
        assertEquals("License beginDate doesn't match!", beginDateOverlay, actualResult);

        actualResult = nodes.get(0).selectSingleNode("//dams:Permission/dams:endDate").getText();
        assertEquals("License endDate doesn't match!", endDateOverlay, actualResult);
    }

    @Test
    public void testOverlayCollection() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        String collectionUrl = TabularEditRecord.getArkUrl("xx000000xx");;
        Element e = addElement(obj, "assembledCollection", damsNS);
        addAttribute(e, "resource", rdfNS, collectionUrl);

        String collectionOverlay = getOverlayValue(collectionUrl);
        Map<String, String> overlayData = createDataWithTitle(objArk, title);
        overlayData.put("collection(s)", collectionOverlay);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:collection");
        assertEquals("The size of collection doesn't match!", 1, nodes.size());
    }

    @Test
    public void testOverlayLanguageWithArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String languageArk = "@bdxxxxxxxx";
        data.put("language", languageArk);
        overlayData.put("language", languageArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:language");
        assertEquals("The size of Language doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Language resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayLanguageWithValueArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String languageArk = "@bdxxxxxxxx";
        data.put("language", languageArk);
        overlayData.put("language", "zxx - xxx xx" + languageArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:language");
        assertEquals("The size of Language doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Language resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayRelationshipWithArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator ark
        String nameArk = "@bdxxxxxxxx";
        data.put("person:creator", nameArk);
        overlayData.put("person:creator", nameArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        Node node = doc.selectSingleNode("//dams:relationship/dams:Relationship");

        String actualNameResult = node.selectSingleNode("//dams:personalName/@rdf:resource").getStringValue();
        assertEquals("Name resource url doesn't match!", TabularEditRecord.getLinkedArkUrl(nameArk), actualNameResult);
        String actualRoleResult = node.selectSingleNode("dams:role/mads:Authority/mads:authoritativeLabel").getText();
        assertEquals("Role value doesn't match!", "creator", actualRoleResult);
    }

    @Test
    public void testOverlayRelationshipWithValueArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String name = "Person:Creator";
        data.put("person:creator", name);
        String overlayName = "Person:Creator@bdxxxxxxxx";
        overlayData.put("person:creator", overlayName);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        Node node = doc.selectSingleNode("//dams:relationship/dams:Relationship");

        String actualNameResult = node.selectSingleNode("//dams:personalName/@rdf:resource").getStringValue();
        assertEquals("Name resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualNameResult);
        String actualRoleResult = node.selectSingleNode("dams:role/mads:Authority/mads:authoritativeLabel").getText();
        assertEquals("Role value doesn't match!", "creator", actualRoleResult);
    }

    @Test
    public void testOverlaySubjectWithArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String topicArk = "@bdxxxxxxxx";
        data.put("subject:topic", topicArk);
        overlayData.put("subject:topic", topicArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:topic");
        assertEquals("The size of Topic doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Topic resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlaySubjectWithValueArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String topicArk = "@bdxxxxxxxx";
        data.put("subject:topic", topicArk);
        String overlayTopic = "Test Topic" + topicArk;
        overlayData.put("subject:topic", overlayTopic);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:topic");
        assertEquals("The size of Topic doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Topic resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayNoteWithArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String noteArk = "@bdxxxxxxxx";
        data.put("note:local attribution", noteArk);
        overlayData.put("note:local attribution", noteArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:note");
        assertEquals("The size of Local attribution note doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Note resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayNoteWithSpaceArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String noteArk = " @ bdxxxxxxxx";
        data.put("note:local attribution", noteArk);
        overlayData.put("note:local attribution", noteArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:note");
        assertEquals("The size of Local attribution note doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Note resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayNoteWithValueArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String noteArk = "@bdxxxxxxxx";
        data.put("note:local attribution", noteArk);
        String overlayNote = "Local attribution note" + noteArk;
        overlayData.put("note:local attribution", overlayNote);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:note");
        assertEquals("The size of Local attribution note doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Note resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayRelatedResourceWithArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String relatedResourceArk = "@bdxxxxxxxx";
        data.put("related resource:related", relatedResourceArk);
        overlayData.put("related resource:related", relatedResourceArk);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:relatedResource");
        assertEquals("The size of related resource doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Related resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayRelatedResourceWithValueArk() throws Exception {
        String title = "Test object";
        Map<String, String> data = createDataWithTitle("zzxxxxxxxx", title);
        Map<String, String> overlayData = createDataWithTitle("zzxxxxxxxx", title);

        // Initiate tabular data for creator with ark reference
        String relatedResourceArk = "@bdxxxxxxxx";
        data.put("related resource:related", relatedResourceArk);
        String overlayNote = "Related resource" + relatedResourceArk;
        overlayData.put("related resource:related", overlayNote);

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(data, overlayData);
        Document doc = testObject.toRDFXML();

        List<Node> nodes = doc.selectNodes("//dams:relatedResource");
        assertEquals("The size of related resource doesn't match!", 1, nodes.size());

        String actualResult = nodes.get(0).selectSingleNode("@rdf:resource").getStringValue();
        assertEquals("Related resource url doesn't match!", TabularEditRecord.getArkUrl("bdxxxxxxxx"), actualResult);
    }

    @Test
    public void testOverlayComponent() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        String componentUrl = TabularEditRecord.getArkUrl("zzxxxxxxxx/1");
        Element e = addElement( obj, "hasComponent", damsNS, "Component", damsNS);;
        addAttribute(e, "about", rdfNS, componentUrl);

        Map<String, String> objectData = createDataWithTitle(objArk, title, "object");
        String compTitle = getOverlayValue(title + " Component1");
        Map<String, String> compData = createDataWithTitle(objArk + "/1", compTitle, "component");

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), objectData, compData);
        Document doc = testObject.toRDFXML();
        List<Node> nodes = doc.selectNodes("//dams:hasComponent/dams:Component");
        assertEquals("The size of component doesn't match!", 1, nodes.size());
        String actualCompTitle = nodes.get(0).selectSingleNode("dams:title//mads:authoritativeLabel").getText();
        assertEquals("Component title doesn't match!", compTitle, actualCompTitle);
    }

    @Test
    public void testOverlaySubcomponent() throws Exception {
        String title = "Test object";
        String objArk = "zzxxxxxxxx";
        String objUrl = TabularEditRecord.getArkUrl(objArk);
        Element obj = createDocumentRoot(objUrl);
        String componentUrl = TabularEditRecord.getArkUrl("zzxxxxxxxx/1");
        Element e = addElement( obj, "hasComponent", damsNS, "Component", damsNS);
        addAttribute(e, "about", rdfNS, componentUrl);
        Element el = addElement( e, "hasComponent", damsNS, "Component", damsNS);
        String subcomponentUrl = TabularEditRecord.getArkUrl("zzxxxxxxxx/2");
        addAttribute(el, "about", rdfNS, subcomponentUrl);

        Map<String, String> objectData = createDataWithTitle(objArk, title, "object");
        String compTitle = getOverlayValue(title + " Component1");
        Map<String, String> compData = createDataWithTitle(objArk + "/1", compTitle, "component");
        String subcompTitle = getOverlayValue(title + " Subomponent1");
        Map<String, String> subcompData = createDataWithTitle(objArk + "/2", subcompTitle, "sub-component");

        // Create record with data overlay
        TabularEditRecord testObject = createdRecordWithOverlay(obj.getDocument(), objectData, compData, subcompData);
        Document doc = testObject.toRDFXML();

        // verify component title
        List<Node> nodes = doc.selectNodes("//dams:Object/dams:hasComponent/dams:Component");
        assertEquals("The size of component doesn't match!", 1, nodes.size());
        String actualCompTitle = nodes.get(0).selectSingleNode("dams:title//mads:authoritativeLabel").getText();
        assertEquals("Component title doesn't match!", compTitle, actualCompTitle);

        // verify sub-component title
        nodes = doc.selectNodes("//dams:Object/dams:hasComponent/dams:Component/dams:hasComponent/dams:Component");
        assertEquals("The size of sub-component doesn't match!", 1, nodes.size());
        String actualSubcompTitle = nodes.get(0).selectSingleNode("dams:title//mads:authoritativeLabel").getText();
        assertEquals("Sub-component title doesn't match!", subcompTitle, actualSubcompTitle);
    }
}
