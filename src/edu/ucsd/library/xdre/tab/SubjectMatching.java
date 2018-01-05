package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.ucsd.library.xdre.imports.RDFDAMS4ImportTsHandler;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DAMSRepository;

/**
 * SubjectMatching: pre-test for matching subjects
 * 
 * @author lsitu
 * @since 2018-01-04
**/
public class SubjectMatching {
	private static Logger log = Logger.getLogger(RDFDAMS4ImportTsHandler.class);
	private static String[] MADS_SUBJECTS = {"PersonalName", "ConferenceName", "CorporateName", "FamilyName", "Topic",
		"GenreForm", "Geographic", "Occupation", "Temporal"};

	private DAMSClient damsClient = null;
	private List<File> rdfFiles = null;
	private List<String> models = null;

	/**
	 * Constructor for SubjectMatching
	 * @param fileNames
	 * @param filesPaths
	 * @throws Exception
	 */
	public SubjectMatching (DAMSClient damsClient, List<File> rdfFiles, List<String> subjectTypes) throws Exception
	{
		this.damsClient = damsClient;
		this.rdfFiles = rdfFiles;
		this.models = convertToModelName(subjectTypes);
	}

	/**
	 * Convert the subject types to model class name
	 * @param subjectTypes
	 * @return
	 */
	private List<String> convertToModelName(List<String> subjectTypes) {
		List<String> subjectModels = new ArrayList<>();

		for (String subjectType : subjectTypes) {
			String subjectName = subjectType.substring(subjectType.lastIndexOf(":") + 1).replace(" name", "Name");
			subjectName = subjectName.substring(0, 1).toUpperCase() + subjectName.substring(1, subjectName.length());
			if (subjectName.equals("Genre"))
				subjectName = "GenreForm";

			if (madsSubject(subjectName))
				subjectName = "mads:" + subjectName;
			else
				subjectName = "dams:" + subjectName;

			subjectModels.add(subjectName);
		}
		return subjectModels;
	}

	/**
	 * Determine whether a subject is a MADS subject
	 * @param subjectName
	 * @return
	 */
	private boolean madsSubject(String subjectName) {
		return Arrays.asList(MADS_SUBJECTS).contains(subjectName);
	}

	/**
	 * Method to retrieve match subjects
	 * @return CSV format string
	 * @throws IOException 
	 * @throws LoginException 
	 */
	public String getMatchSubjects () throws LoginException, IOException
	{
		StringBuilder csvBuilder = new StringBuilder();
		String header = escapeRow("subject type", "subject term", "result", "ARK", "exactMatch", "closeMatch");
		csvBuilder.append(header);

		DAMSRepository damsRepository = DAMSRepository.getRepository();
		for (File rdfFile : rdfFiles) {
			SAXReader saxReader = new SAXReader();
			try {
				boolean result = false;
				Document srcDoc = saxReader.read(rdfFile);
				Node labelNode = srcDoc.selectSingleNode("//mads:authoritativeLabel");
				String subjectTerm  = labelNode.getText();
				String modelName= getSubjectTypeLabel(labelNode.getParent().getName());
				
				for (String model : models) {
					List<String> matchedSubjects = damsRepository.findAuthority(model, subjectTerm);
					if (matchedSubjects != null) {
						if(matchedSubjects.size() > 0)
							result = true;

						for (String subject : matchedSubjects) {
							String ark = subject;
							Document doc = damsClient.getRecord(ark);

							// subject type
							modelName = getSubjectTypeLabel(doc.selectSingleNode("/rdf:RDF/*").getName());

							String exactMatch = concatValues(doc.selectNodes("/rdf:RDF/*/mads:hasExactExternalAuthority/@rdf:resource"));
							String closeMatch = concatValues(doc.selectNodes("/rdf:RDF/*/mads:hasCloseExternalAuthority/@rdf:resource"));

							String row = escapeRow(modelName, subjectTerm, "match", ark, exactMatch, closeMatch);
							csvBuilder.append(row);
						}
					}
				}
				
				if (!result) {
					csvBuilder.append(escapeRow(modelName, subjectTerm, result?"match":"no match", "", "", ""));
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				log.error(ex.getMessage());
				
				String row = escapeRow(rdfFile.getName(), "", "Error: " + ex.getMessage(), "", "", "");
				csvBuilder.append(row);
			}
		}
		return csvBuilder.toString();
	}

	private String concatValues(List<Node> nodes) {
		String concatValue = "";
		for (Node node : nodes) {
			concatValue += (concatValue.length() > 0 ? " | " : "") + node.getText();
		}
		return concatValue;
	}
	/**
	 * Convert model name to subject types
	 * @param modelName
	 * @return
	 */
	private String getSubjectTypeLabel(String modelName) {
		String modelLabel = modelName.substring(0, 1).toLowerCase() + modelName.substring(1, modelName.length());
		modelLabel = modelLabel.replace("Name", " name");
		switch(modelLabel) {
		case "genreForm":
			modelLabel = "Subject:genre";
			break;
		default:
			modelLabel = "Subject:" + modelLabel;
			break;
		}
		return modelLabel;
	}

	private String escapeRow(String subjectType, String subjectTerm, String result, String ark, String exactMatch, String closeMatch) {
		StringBuilder builder = new StringBuilder();
		builder.append(escapeCsv(subjectType) + ",");
		builder.append(escapeCsv(subjectTerm) + ",");
		builder.append(escapeCsv(result) + ",");
		builder.append(escapeCsv(ark) + ",");
		builder.append(escapeCsv(exactMatch) + ",");
		builder.append(escapeCsv(closeMatch) + "\n");
		return builder.toString();
	}

	private static String escapeCsv(String value) {
		if (value == null)
			return "";
		if (value.indexOf(",") >= 0 || value.indexOf("\"") >= 0 
				|| value.indexOf(System.getProperty("line.separator")) >= 0)
			return "\"" + value.replace("\"", "\"\"") + "\"";
		else
			return value;
	}
}
