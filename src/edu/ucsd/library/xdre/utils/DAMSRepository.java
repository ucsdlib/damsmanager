package edu.ucsd.library.xdre.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Node;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.ucsd.library.xdre.imports.RDFDAMS4ImportTsHandler;

public class DAMSRepository {
	private static Logger log = Logger.getLogger(DAMSRepository.class);

	public static String MADS_AUTHORITATIVELABEL = "mads:authoritativeLabel";
	public static String RDF_ABOUT = "rdf:about";

	private static DAMSRepository damsRepository = null;

	private DAMSClient damsClient;
	private Map<String, Map<String, List<String>>> authoritiesModelMap = new HashMap<>();

	protected DAMSRepository(DAMSClient damsClient) {
		this.damsClient = damsClient;
	}

	/**
	 * Get the instance of the DamsRepository object
	 * @return
	 * @throws LoginException
	 * @throws IOException
	 */
	public static synchronized DAMSRepository getRepository() throws LoginException, IOException {
		if (damsRepository == null)
			damsRepository = new DAMSRepository(new DAMSClient(Constants.DAMS_STORAGE_URL));

		return damsRepository;
	}

	/**
	 * Lookup authority record from the repository
	 * @param model
	 * @param label
	 * @return
	 * @throws Exception
	 */
	public List<String> findAuthority(String model, String label) throws Exception {
		Map<String, List<String>> authorities = authoritiesModelMap.get(model);
		if (authorities == null || authorities.size() == 0) {
			authorities = refeshAuthoritiesCache(model);
		}

		String lookupKey = stripPunctuations(label);

		log.debug("Lookup " + model + " with label " + label + " (key: " + lookupKey + ")" + ": " + authorities.get(lookupKey));
		return authorities.get(lookupKey);
	}

	/**
	 * Refresh the authority record cache
	 * @param model the authority class
	 * @return
	 * @throws Exception
	 */
	public synchronized Map<String, List<String>> refeshAuthoritiesCache(String model) throws Exception {
		Map<String, List<String>> authorities = new HashMap<>();
		String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n PREFIX mads: <http://www.loc.gov/mads/rdf/v1#> " + 
				"SELECT ?sub ?label WHERE { ?sub rdf:type ?o . ?o rdf:label '\"" + model + "\"' . ?sub mads:authoritativeLabel ?label }";
		List<Map<String, String>> solutions = damsClient.sparqlLookup(sparql);
		for (Map<String, String> solution : solutions) {
			String labelKey = stripPunctuations(solution.get("label"));
			List<String> ids = authorities.get(labelKey);
			if (ids == null) {
				ids = new ArrayList<>();
				authorities.put(labelKey,ids);
			}

			ids.add(solution.get("sub"));
		}
		authoritiesModelMap.put(model, authorities);
		return authorities;
	}

	/**
	 * Create authority record
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public synchronized String createAuthorityRecord(Node node) throws Exception {
		Node aboutAttr = node.selectSingleNode("@" + RDF_ABOUT + "|/rdf:RDF/*/@" + RDF_ABOUT);
		String recordUrl = aboutAttr.getStringValue();

		if (!(recordUrl.startsWith("http") && recordUrl.contains("/ark:/20775/"))) {
			recordUrl = RDFDAMS4ImportTsHandler.toDamsUrl(damsClient.mintArk(Constants.DEFAULT_ARK_NAME));
			aboutAttr.setText(recordUrl);
		}

		if (!damsClient.updateObject(recordUrl, node.asXML(), Constants.IMPORT_MODE_ALL)) {
			log.error("Failed to create authority record:\n" + node.asXML());
			throw new Exception("Failed to create authority record: " + recordUrl);
		}

		// add the new record to cache
		Node recordNode = aboutAttr.getParent();
		String elemPath = recordNode.getPath();
		String modelName = elemPath.substring(elemPath.lastIndexOf("/") + 1);
		Map<String, List<String>> authorities = authoritiesModelMap.get(modelName);
		if (authorities == null) {
			authorities = refeshAuthoritiesCache(modelName);
		}

		Node labelNode = recordNode.selectSingleNode(MADS_AUTHORITATIVELABEL);
		if (labelNode != null) {
			authorities.put(stripPunctuations(labelNode.getText()), Arrays.asList(recordUrl));
		}

		log.info("Created authority record " + modelName + ": " + recordUrl);
		return recordUrl;
	}

	/**
	 * Update authority record
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public synchronized boolean updateAuthorityRecord(Node node) throws Exception {
		Node aboutAttr = node.selectSingleNode("@" + RDF_ABOUT + "|/rdf:RDF/*/@" + RDF_ABOUT);
		String recordUrl = aboutAttr.getStringValue();

		boolean successful = damsClient.updateObject(recordUrl, node.asXML(), Constants.IMPORT_MODE_ALL);

		// refresh the authority cache
		Node recordNode = aboutAttr.getParent();
		String elemPath = recordNode.getPath();
		String model = elemPath.substring(elemPath.lastIndexOf("/") + 1);
		refeshAuthoritiesCache(model);

		log.info("Updated authority record " + model + ": " + recordUrl);
		return successful;
	}

	public synchronized void clearCache() {
		authoritiesModelMap.clear();
	}

	public static boolean isAuthorityRecord(Node node) throws UnsupportedEncodingException, IOException {
		Node aboutNode = node.selectSingleNode("//@" + RDF_ABOUT);
		String uri = aboutNode.getStringValue();
		if (!uri.startsWith(Constants.DAMS_ARK_URL_BASE)) {
			// use valid uri for Jena  
			uri = Constants.DAMS_ARK_URL_BASE + "/" + Constants.ARK_ORG + "/" + uri;
			aboutNode.setText(uri);
		}

		return isAuthorityRecord(uri, new RDFStore().loadRDFXML(node.asXML()));
	}

	public static boolean isAuthorityRecord(String subject, Model model) {
		StmtIterator iter = model.listStatements(model.createResource(subject), RDF.type, (RDFNode)null);
		while (iter.hasNext()) {
			String rdfType = iter.next().getObject().toString();
			if (rdfType.endsWith("Object") || rdfType.endsWith("Collection") || rdfType.endsWith("Component"))
				return false;
		}
		return true;
	}

	/**
	 * Processed string basing on rules: case-insensitive, ignore leading, trailing, multiple spaces and punctuation
	 * @param value
	 * @return
	 */
	public static String stripPunctuations(String value) {
		return Normalizer.normalize(StringUtils.normalizeSpace(value.toLowerCase().replaceAll("[\\\\/\\|\\[\\]()\\{\\},\\.:;\\?\'\"~`!@#\\$%\\^&\\*<>\\+\\-_=]", " ")), Normalizer.Form.NFD);
	}
}
