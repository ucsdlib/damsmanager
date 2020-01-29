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
    public static String copyrightPublic  = "Public domain";
    public static String copyrightRegents = "Copyright UC Regents";
    public static String copyrightPerson = "Copyrighted (Person)";
    public static String copyrightCorporate = "Copyrighted (Corporate)";
    public static String copyrightOther = "Copyrighted (Other)";
    private static String copyrightUnknown = "Unknown";
    private static String underCopyright = "Under copyright";

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

    private static String address = "UC San Diego, La Jolla, 92093-0175";
    private static String programRDCnote = "Research Data Curation Program, " + address
        + " (https://lib.ucsd.edu/rdcp)";
    private static String programDLPnote = "Digital Library Development Program, " + address
        + " (https://lib.ucsd.edu/digital-library)";
    private static String programSCAnote = "Special Collections & Archives, " + address
        + " (https://lib.ucsd.edu/sca)";

    private static String culturalSensitivityNote = "Culturally sensitive content: This is an "
        + "image of a person or persons now deceased. In some Aboriginal Communities, hearing "
        + "names or seeing images of deceased persons may cause sadness or distress, particularly "
        + "to the relatives of these people.";

    private static String culturalSensitivityRestrictedNote = "Image not available due to cultural "
            + "sensitivities of the community depicted.";

    private static String copyrightPurposeNote = "Use: This work is available from the UC San "
        + "Diego Library. This digital copy of the work is intended to support research, teaching, "
        + "and private study.";
    private static String copyrightNoteCopyrightedUS = "Constraint(s) on Use: This work is "
        + "protected by the U.S. Copyright Law (Title 17, U.S.C.). Use of this work beyond that "
        + "allowed by \"fair use\" or any license applied to this work requires written permission of the copyright holder(s). "
        + "Responsibility for obtaining permissions and any use and distribution of this work "
        + "rests exclusively with the user and not the UC San Diego Library. Inquiries can be made "
        + "to the UC San Diego Library program having custody of the work.";
    private static String copyrightNoteCopyrightedOther = "Constraint(s) on Use: This work is "
        + "protected by copyright law. Use of this work beyond that allowed by the applicable copyright statute "
        + "or any license applied to this work requires written permission of the copyright holder(s). "
        + "Responsibility for obtaining permissions and any use and distribution of this work "
        + "rests exclusively with the user and not the UC San Diego Library. Inquiries can be made "
        + "to the UC San Diego Library program having custody of the work.";
    private static String copyrightNotePublicDomain = "Constraint(s) on Use: This work may be used "
        + "without prior permission.";
    private static String copyrightNoteUnknownUS = "Constraint(s) on Use: This work may be "
        + "protected by the U.S. Copyright Law (Title 17, U.S.C.). Use of this work beyond that "
        + "allowed by \"fair use\" or any license applied to this work requires written permission of the copyright holders(s). "
        + "Responsibility for obtaining permissions and any use and distribution of this work "
        + "rests exclusively with the user and not the UC San Diego Library. Inquiries can be "
        + "made to the UC San Diego Library program having custody of the work.";
    private static String copyrightNoteUnknownOther = "Constraint(s) on Use: This work may be "
        + "protected by copyright law. Use of this work beyond that allowed by the applicable "
        + "copyright statute or any license applied to this work requires written permission of the copyright holders(s). "
        + "Responsibility for obtaining permissions and any use and distribution of this work "
        + "rests exclusively with the user and not the UC San Diego Library. Inquiries can be "
        + "made to the UC San Diego Library program having custody of the work.";

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
        String copyrightStatus, String copyrightJurisdiction, String[] copyrightOwners,
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
            if ( !isBlank(copyrightJurisdiction) ) {
                c.addElement("dams:copyrightJurisdiction",damsURI).setText( copyrightJurisdiction );
            }

            if ( copyrightStatus.startsWith("Copyrighted (") || copyrightStatus.equals(copyrightRegents) ) {
                c.addElement("dams:copyrightStatus",damsURI).setText( underCopyright );
            } else {
                c.addElement("dams:copyrightStatus",damsURI).setText( copyrightStatus );
            }

            if ( copyrightStatus.equals( copyrightRegents ) ) {
                String[] rightsHolders = {"UC Regents"};
                addRightsHolder( o, copyrightStatus, rightsHolders);
            } else if ( copyrightOwners != null && copyrightOwners.length > 0 ) {
                addRightsHolder( o, copyrightStatus, copyrightOwners );
            }

            // copyright boilerplate
            c.addElement("dams:copyrightPurposeNote").setText(copyrightPurposeNote);
            if ( copyrightStatus.equals(copyrightPublic) ) {
                c.addElement("dams:copyrightNote").setText(copyrightNotePublicDomain);
            } else if ( copyrightStatus.equals(copyrightUnknown) ) {
                if ( copyrightJurisdiction.equalsIgnoreCase("us") ) {
                    c.addElement("dams:copyrightNote").setText(copyrightNoteUnknownUS);
                } else {
                    c.addElement("dams:copyrightNote").setText(copyrightNoteUnknownOther);
                }
            } else {
                if ( copyrightJurisdiction.equalsIgnoreCase("us") ) {
                    c.addElement("dams:copyrightNote").setText(copyrightNoteCopyrightedUS);
                } else {
                    c.addElement("dams:copyrightNote").setText(copyrightNoteCopyrightedOther);
                }
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
                addLicense( o, "Display currently prohibited.", null, "display", beginDate, endDate, null );
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
                // License
                addLicense( o, "Display currently prohibited.", null, "display", beginDate, endDate, null );
                addOtherRights( o, "cultural sensitivity", "metadataDisplay", "display");
                addCulturalSensitivityRestrictedNote(o);
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
    private static void addCulturalSensitivityRestrictedNote( Element o )
    {
        Element note = o.addElement("dams:note",damsURI).addElement("dams:Note",damsURI);
        note.addElement("rdf:value",rdfURI).setText(culturalSensitivityRestrictedNote);
        note.addElement("dams:internalOnly",damsURI).setText("true");
    }
    public static void addOtherRights( Element o, String basis, String permission,
            String restriction )
    {
        addOtherRights( o, basis, null, permission, restriction );
    }
    public static void addOtherRights( Element o, String basis, String note, String permission,
        String restriction )
    {
        Element other = o.addElement("dams:otherRights",damsURI)
            .addElement("dams:OtherRights",damsURI);
        if (!isBlank(basis)) 
        {
            other.addElement("dams:otherRightsBasis",damsURI).setText(basis);
        }
        if (!isBlank(note)) {
            other.addElement("dams:otherRightsNote",damsURI).setText(note);
        }
        addRightsAction( other, permission, restriction, null, null );
    }
    public static void addLicense( Element o, String note, String permission, String restriction,
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
    public static void addRightsHolder( Element o, String copyrightStatus, String[] rightsHolders )
    {
        for (String rightsHolder : rightsHolders)
        {
            if (!isBlank (rightsHolder))
            {
                addRightsHolder( o, copyrightStatus, rightsHolder);
            }
        }
    }

    /**
     * Map copyrightStatus to rights holder predicate
     * @param copyrightStatus
     * @return
     */
    public static String getRightsHolderPredicate(String copyrightStatus) {
        if ( copyrightStatus.equals(copyrightPerson) )
        {
            return "rightsHolderPersonal";
        }
        else if ( copyrightStatus.equals(copyrightCorporate)
                || copyrightStatus.equals(copyrightRegents) )
        {
            return "rightsHolderCorporate";
        }

        return "rightsHolderName";
    }

    /**
     * Map copyrightStatus to rights holder name class
     * @param copyrightStatus
     * @return
     */
    public static String getRightsHolderClass(String copyrightStatus) {
        if ( copyrightStatus.equals(copyrightPerson) )
        {
            return "mads:PersonalName";
        }
        else if ( copyrightStatus.equals(copyrightCorporate)
                || copyrightStatus.equals(copyrightRegents) )
        {
            return "mads:CorporateName";
        }
        return "mads:Name";
    }

    /*
     * add rights holder
     * @param o
     * @param header
     * @param rightsHolder
     */
    public static void addRightsHolder( Element o, String copyrightStatus, String rightsHolder) {
        String predicate = "dams:" + getRightsHolderPredicate(copyrightStatus);
        String nameClass = getRightsHolderClass(copyrightStatus);

        Element name = o.addElement(predicate,damsURI).addElement(nameClass,madsURI);

        name.addElement("mads:authoritativeLabel",madsURI).setText(rightsHolder);
        Element el = name.addElement("mads:elementList");
        el.addAttribute( new QName("parseType",rdfNS), "Collection" );
        el.addElement("mads:FullNameElement", madsURI).addElement("mads:elementValue", madsURI)
            .setText(rightsHolder.trim());
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
