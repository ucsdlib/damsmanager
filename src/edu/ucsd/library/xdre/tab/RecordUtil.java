package edu.ucsd.library.xdre.tab;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * Utilities for working with Record instances.
 * @author lsitu
 * @author escowles
 * @since 2014-09-18
**/
public class RecordUtil
{
    // private type values
    private static String copyrightPublic  = "Public domain";
    private static String copyrightRegents = "Copyright UC Regents";
    private static String copyrightPerson = "Copyrighted (Person)";
    private static String copyrightCorporate = "Copyrighted (Corporate)";
    private static String copyrightOther = "Copyrighted (Other)";
    private static String copyrightUnknown = "Unknown";

    private static String accessPublicLicense           = "Public - granted by rights holder";
    private static String accessPublicFairUse           = "Public - open fair use";
    private static String accessPublicMetadata          = "Public - metadata only fair use";
    private static String accessUCSDLicense             = "UCSD - granted by rights holder";
    private static String accessUCSDFairUse             = "UCSD - educational fair use";
    private static String accessCurator                 = "Curator";
    private static String accessClickthroughSensitivity = "Click through - cultural sensitivity";
    private static String accessRestrictedSensitivity   = "Restricted - cultural sensitivity";
    private static String accessRestrictedLicense       = "Restricted - by rights holder";

    private static String accessCcBy    = "Creative Commons - Attribution";
    private static String accessCcBySa  = "Creative Commons - Attribution-ShareAlike";
    private static String accessCcByNd  = "Creative Commons - Attribution-NoDerivs";
    private static String accessCcByNc  = "Creative Commons - Attribution-NonCommercial";
    private static String accessCcByNcSa= "Creative Commons - Attribution-NonCommercial-ShareAlike";
    private static String accessCcByNcNd= "Creative Commons - Attribution-NonCommercial-NoDerivs";

    private static String creativeCommons = "http://creativecommons.org/licenses/";

    private static String programRDC = "Research Data Curation Program";
    private static String programDLP = "Digital Library Development Program";
    private static String programSCA = "Special Collections and Archives";

    private static String address = "University of California, San Diego, La Jolla, 92093-0175";
    private static String programRDCnote = "Research Data Curation Program, " + address
        + " (http://libraries.ucsd.edu/services/data-curation/)";
    private static String programDLPnote = "Digital Library Development Program, " + address
        + " (http://libraries.ucsd.edu/about/digital-library/)";
    private static String programSCAnote = "Special Collections & Archives, " + address
        + " (http://libraries.ucsd.edu/collections/sca/)";

    private static String culturalSensitivityNote = "Culturally sensitive content: This is an "
        + "image of a person or persons now deceased. In some Aboriginal Communities, hearing "
        + "names or seeing images of deceased persons may cause sadness or distress, particularly "
        + "to the relatives of these people.";

    /**
     * Copyright values.
    **/
    public static String[] COPYRIGHT_VALUES = {
        copyrightPublic, copyrightRegents, copyrightPerson, copyrightCorporate,
        copyrightOther, copyrightUnknown
    };

    /**
     * Access values.
    **/
    public static String[] ACCESS_VALUES = {
        accessPublicLicense, accessPublicFairUse, accessPublicMetadata,
        accessUCSDLicense, accessUCSDFairUse, accessCurator, accessClickthroughSensitivity,
        accessRestrictedSensitivity, accessRestrictedLicense, accessCcBy, accessCcBySa,
        accessCcByNd, accessCcByNc, accessCcByNcSa, accessCcByNcNd
    };

    /**
     * Program values.
    **/
    public static String[] PROGRAM_VALUES = { programRDC, programDLP, programSCA };

    /**
     * Rights holder types.
    **/
    public static String[] RIGHTSHOLDER_TYPES = { "Personal", "Corporate", "Family" };
    
    // namespaces
    private static String damsURI="http://library.ucsd.edu/ontology/dams#";
    private static String madsURI="http://www.loc.gov/mads/rdf/v1#";
    private static String rdfURI="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static String rdfsURI="http://www.w3.org/2000/01/rdf-schema#";
    private static Namespace damsNS = new Namespace("dams", damsURI);
    private static Namespace madsNS = new Namespace("mads",madsURI);
    private static Namespace rdfNS = new Namespace("rdf", rdfURI);
    private static Namespace rdfsNS = new Namespace("rdfs", rdfsURI);
    private static QName rdfResource = new QName("resource", rdfNS);


    /**
     * Add rights metadata to a Document.
     * @param doc RDF/XML document containing an Object record.
     * @param unitURI Unit URI.
     * @param collectionURIs Zero or more collection URIs.
     * @param copyrightStatus One value from COPYRIGHT_VALUES.
     * @param copyrightJurisdiction Copyright jurisdiction country code.
     * @param copyrightOwner Name of the copyright owner.
     * @param program One value from PROGRAM_VALUES.
     * @param access  One value from ACCESS_VALUES.
     * @param endDate End of any license permission or restriction (in YYYY-MM-DD format).
    **/
    public static void addRights( Document doc, String unitURI, Map<String, String> collections,
        String copyrightStatus, String copyrightJurisdiction, String copyrightOwner, String rightsHolderType,
        String program, String access, String beginDate, String endDate )
    {
        Element o = (Element)doc.selectSingleNode("//dams:Object");

        // unit
        if ( !isBlank(unitURI) ) {
        	o.addElement("dams:unit",damsURI).addAttribute(rdfResource, unitURI);
        }

        // program
        if ( !isBlank(program) ) {
	        if ( program.equals(programRDC) )
	        {
	            addProgramNote(o, programRDCnote);
	        }
	        else if ( program.equals(programDLP) )
	        {
	            addProgramNote(o, programDLPnote);
	        }
	        else if ( program.equals(programSCA) )
	        {
	            addProgramNote(o, programSCAnote);
	        }
        }
        
        // collections
        if (collections != null && collections.size() > 0) {
	        for ( Iterator<String> it = collections.keySet().iterator(); it.hasNext(); )
	        {
	            String uri = it.next();
	            String collType = collections.get(uri);
	            String collPredicate = StringUtils.isNotBlank(collType) ? collType.substring(0, 1).toLowerCase() + collType.substring(1) : "collection";
	            o.addElement("dams:" + collPredicate, damsURI).addAttribute(rdfResource, uri);
	        }
        }

        // copyright
        if (!isBlank(copyrightStatus)) {
	        Element c = o.addElement("dams:copyright",damsURI).addElement("dams:Copyright",damsURI);
	        if ( !isBlank(copyrightJurisdiction) )
	        {
	            c.addElement("dams:copyrightJurisdiction",damsURI).setText( copyrightJurisdiction );
	        }
            if ( copyrightStatus.startsWith("Copyrighted (") )
            {
	            c.addElement("dams:copyrightStatus",damsURI).setText( "Copyrighted" );
            }
            else
            {
	            c.addElement("dams:copyrightStatus",damsURI).setText( copyrightStatus );
            }
	        if ( copyrightStatus.equals( copyrightRegents ) )
	        {
	            addRightsHolder( o, copyrightStatus, "UC Regents");
	        }
	        else if ( !isBlank(copyrightOwner) )
	        {
	            addRightsHolder( o, copyrightStatus, copyrightOwner );
	        }
        }

        // other rights
        if ( !isBlank( access ) )
        {
            if ( access.equals(accessPublicLicense) )
            {
                addLicense( o, "Public access granted by rights holder.", "display", null, beginDate, endDate, null );
            }
            else if ( access.equals(accessPublicFairUse) )
            {
                addOtherRights( o, "fair use (public)", "display", null );
            }
            else if ( access.equals(accessPublicMetadata) )
            {
                addOtherRights( o, "fair use (public)", "metadataDisplay", null );
            }
            else if ( access.equals(accessUCSDLicense) )
            {
                addLicense( o, "Access granted by rights holder.", "localDisplay", null,
                		beginDate, endDate, null );
            }
            else if ( access.equals(accessUCSDFairUse) )
            {
                addOtherRights( o, "fair use (UCSD)", "localDisplay", null );
            }
            else if ( access.equals(accessCurator) )
            {
                addOtherRights( o, null, null, "display" );
            }
            else if ( access.equals(accessClickthroughSensitivity) )
            {
            	// License
            	addLicense( o, null, "display", null, beginDate, endDate, null );
            	// OtherRights
                addOtherRights( o, "cultural sensitivity", "display", null );
                addCulturalSensitivityNote(o);
            }
            else if ( access.equals(accessRestrictedSensitivity) )
            {
                addOtherRights( o, "cultural sensitivity", "metadataDisplay", "display");
                addCulturalSensitivityNote(o);
            }
            else if ( access.equals(accessRestrictedLicense) )
            {
                addLicense( o, "Display currently prohibited.", null, "display", 
                		beginDate, endDate, null );
                addOtherRights( o, "fair use (public)", "metadataDisplay", null);
            }
            else if ( access.equals(accessCcBy) )
            {
                addLicense( o, getCreativeCommonsNote("Attribution"), 
                		"display", null, beginDate, endDate, creativeCommons + "by/4.0/" );
            }
            else if ( access.equals(accessCcBySa) )
            {
                addLicense( o, getCreativeCommonsNote("Attribution-ShareAlike"),
                		"display", null, beginDate, endDate, creativeCommons + "by-sa/4.0/" );
            }
            else if ( access.equals(accessCcByNd) )
            {
                addLicense( o, getCreativeCommonsNote("Attribution-NoDerivatives"),
                		"display", null, beginDate, endDate, creativeCommons + "by-nd/4.0/" );
            }
            else if ( access.equals(accessCcByNc) )
            {
                addLicense( o, getCreativeCommonsNote("Attribution-NonCommercial"),
                		"display", null, beginDate, endDate, creativeCommons + "by-nc/4.0/" );
            }
            else if ( access.equals(accessCcByNcSa) )
            {
                addLicense( o, getCreativeCommonsNote("Attribution-NonCommercial-ShareAlike"),
                		"display", null, beginDate, endDate, creativeCommons + "by-nc-sa/4.0/" );
            }
            else if ( access.equals(accessCcByNcNd) )
            {
                addLicense( o,  getCreativeCommonsNote("Attribution-NonCommercial-NoDerivatives"),
                		"display", null, beginDate, endDate, creativeCommons + "by-nc-nd/4.0/" );
            }
        }
    }

    private static void addProgramNote( Element o, String noteValue )
    {
        Element note = o.addElement("dams:note",damsURI).addElement("dams:Note",damsURI);
        note.addElement("dams:type",damsURI).setText("local attribution");
        note.addElement("dams:displayLabel",damsURI).setText("digital object made available by");
        note.addElement("rdf:value",rdfURI).setText(noteValue);
    }
    private static void addCulturalSensitivityNote( Element o )
    {
        Element note = o.addElement("dams:note",damsURI).addElement("dams:Note",damsURI);
        note.addElement("rdf:value",rdfURI).setText(culturalSensitivityNote);
    }
    private static void addOtherRights( Element o, String basis, String permission,
        String restriction )
    {
        Element other = o.addElement("dams:otherRights",damsURI)
            .addElement("dams:OtherRights",damsURI);
        if (!isBlank(basis)) 
        {
        	other.addElement("dams:otherRightsBasis",damsURI).setText(basis);
        }
        addRightsAction( other, permission, restriction, null, null );
    }
    private static void addLicense( Element o, String note, String permission, String restriction,
    		String beginDate, String endDate, String licenseURI )
    {
        Element license = o.addElement("dams:license",damsURI).addElement("dams:License",damsURI);
        if (!isBlank(note)) {
        	license.addElement("dams:licenseNote",damsURI).setText(note);
        }
        if ( !isBlank(licenseURI) )
        {
            license.addElement("dams:licenseURI",damsURI).setText(licenseURI);
        }
        addRightsAction( license, permission, restriction, beginDate, endDate );
    }
    private static void addRightsAction( Element e, String permission, String restriction,
    		String beginDate, String endDate )
    {
        if ( !isBlank(permission) )
        {
            Element perm = e.addElement("dams:permission",damsURI)
                .addElement("dams:Permission",damsURI);
            perm.addElement("dams:type",damsURI).setText(permission);
        	if ( !isBlank(beginDate) )
            {
        		perm.addElement("dams:beginDate",damsURI).setText(beginDate);
            }
            if ( !isBlank(endDate) )
            {
                perm.addElement("dams:endDate",damsURI).setText(endDate);
            }
        }
        if ( !isBlank(restriction) )
        {
            Element rest = e.addElement("dams:restriction",damsURI)
                .addElement("dams:Restriction",damsURI);
            rest.addElement("dams:type",damsURI).setText(restriction);
            if (isBlank(permission)) {
	        	if ( !isBlank(beginDate) )
	            {
	                rest.addElement("dams:beginDate",damsURI).setText(beginDate);
	            }
	        	if ( !isBlank(endDate) ) 
	        	{
	        		rest.addElement("dams:endDate",damsURI).setText(endDate);
	        	}
            }
        }
    }
    private static void addRightsHolder( Element o, String copyrightStatus, String rightsHolder )
    {
        String predicate = null;
        String nameClass = null;
        if ( copyrightStatus.equals(copyrightPerson) )
        {
            predicate = "dams:rightsHolderPersonal";
            nameClass = "mads:PersonalName";
        }
        else if ( copyrightStatus.equals(copyrightCorporate) || copyrightStatus.equals(copyrightRegents) )
        {
            predicate = "dams:rightsHolderCorporate";
            nameClass = "mads:CorporateName";
        }
        if ( copyrightStatus.equals(copyrightOther) )
        {
            predicate = "dams:rightsHolderName";
            nameClass = "mads:Name";
        }
        Element name = o.addElement(predicate,damsURI).addElement(nameClass,madsURI);
        name.addElement("mads:authoritativeLabel",madsURI).setText(rightsHolder);
        Element el = name.addElement("mads:elementList");
        el.addAttribute( new QName("parseType",rdfNS), "Collection" );
        el.addElement("mads:FullNameElement", madsURI).addElement("mads:elementValue", madsURI)
            .setText(rightsHolder);
    }

    private static String getCreativeCommonsNote( String attribution )
    {
    	return "Creative Commons " + attribution + " 4.0 International Public License";
    }

    private static boolean isBlank( String s )
    {
        return s == null || s.trim().equals("");
    }
}
