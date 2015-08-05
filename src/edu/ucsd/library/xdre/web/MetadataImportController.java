package edu.ucsd.library.xdre.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.xml.sax.SAXException;

import edu.ucsd.library.xdre.collection.MetadataImportHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DamsURI;

/**
 * Controller to perform RDF metadata import.
 * @author lsitu
 *
 */
public class MetadataImportController implements Controller{
	private static Logger log = Logger.getLogger( MetadataImportController.class );

    @Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String message = "";
		DAMSClient damsClient = null;	

		request.setCharacterEncoding("UTF-8");
		String ark = request.getParameter("ark");
		String dataFormat = request.getParameter("dataFormat");
		String data = request.getParameter("data");
		String importMode = request.getParameter("importMode");
		MetadataImportHandler handler = null;	

		boolean result = false;
		HttpSession session = request.getSession();
		try{
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setUser(request.getRemoteUser());

			String oData = (String)session.getAttribute("data");

			String objLink = "";
			if ( hasChange (data, oData) ) {
				// Parse the RDF for errors
				SAXReader saxReader = new SAXReader();
				Document doc = saxReader.read(new ByteArrayInputStream(data.getBytes("UTF-8")));
				
				// remove the collection extent note
				List<Node> extentNotes = doc.selectNodes("//*[contains(name(), 'Collection')]/dams:note/dams:Note[dams:type='extent' and contains(rdf:value, 'digital object.')]");
				for (Node extentNote : extentNotes) {
					extentNote.getParent().detach();
					data = doc.asXML();
				}

				handler = new MetadataImportHandler(damsClient, null, data.trim(), dataFormat, importMode);
				result = handler.execute();
				if(result) {
					List<String> soluctions = new ArrayList<>();
					List<Node> nodes = doc.selectNodes("//@rdf:about");
					for (Node node : nodes) {
						String subId = node.getStringValue();
						String className = node.getParent().getName();

						if (!className.endsWith("Object")) {
							// Collection or Authority update, need to update SOLR for the linked records.
							queryObjects ( soluctions, subId, 1, damsClient );
							objLink = "Total " + soluctions.size() + " records affected by ark " + subId.substring(subId.lastIndexOf("/") + 1) + ".";
						} else {
							String damsUrl = "http://" + Constants.CLUSTER_HOST_NAME + (Constants.CLUSTER_HOST_NAME.startsWith("localhost")?"":".ucsd.edu/dc") + "/object/"
									+ ark.substring(ark.lastIndexOf("/") + 1, ark.length());
							objLink = "View item <a href=\"" + damsUrl +  "\" target=\"dc\">" + ark + "</a>.";
						}
					}

					message = "Update successfully. " + objLink;
					session.removeAttribute("data");
				} else {
					StringBuilder err = new StringBuilder();
					for (String error : handler.getErrors()) {
						err.append((err.length() > 0 ? "; \n": "") + error);
					}
					message = err.toString();
				}
			} else {
				message = "Update rejected: No changes found.";
			}
		}catch (Exception e){
			e.printStackTrace();
			message = "Error: " + e.getMessage();
			
		}finally{
			if(damsClient != null)
				damsClient.close();
			if(handler != null) {
				handler.release();
				handler = null;
			}
		}

		request.getSession().setAttribute("message", message);
		if(result) {
			response.sendRedirect(request.getContextPath() + "/rdfImport.do");
		} else {
			request.getRequestDispatcher("rdfEdit.do").forward(request, response);
		}
		return null;
	}

	private void queryObjects( List<String> soluctions, String oid, int level, DAMSClient damsClient ) throws Exception {
		String sparql = "SELECT ?sub WHERE {";
		String bnId = "";
		int le = level;
		for ( int i = 0; i < le; i++ ) {
			if ( i == 0 && le <= 1 )
				sparql += " ?sub ?pre" + i + " <" + oid + "> ";
			else if ( i == 0 ) {
				bnId = "_bn" + i;
				sparql += " ?sub ?pre" + i + " ?" + bnId;
			} else if ( i == le - 1 ) {
				sparql += " . ?" + bnId + " ?pre" + i + " <" + oid + "> ";
			} else {
				String tmpBnId = "_bn" + i;
				sparql += " . ?" + bnId + " ?pre" + i + " ?" + tmpBnId;
				bnId= tmpBnId;
			}	
		}
		sparql += "}";

		soluctions.add(oid);
		List<Map<String, String>> results = damsClient.sparqlLookup(sparql);
		boolean hasBn = false;
		for ( Map<String, String> result : results ) {
			String sub = result.get("sub");
			if (!sub.startsWith("http")) {
				hasBn = true;
			} else {
				// affected record, could be a component URI.
				DamsURI damsURI = DamsURI.toParts(sub, null);
				sub = damsURI.getObject();
				if ( !soluctions.contains(sub) ) {
					soluctions.add(sub);
					try {
						Document doc = damsClient.getRecord(sub);
						if (doc.selectSingleNode("//dams:Object") == null ) {
							// collection CLR, authority record, need to query linked records
							queryObjects ( soluctions, sub, 1, damsClient );
						}
					}catch (Exception e) {
						log.error("Failed to retrieve record " + sub, e);
					}
				}
			}
		}
		
		if ( hasBn ) {
			// has blankNode subject, query one level down.
			queryObjects ( soluctions, oid, ++le, damsClient );
		}
	}
	
	private boolean hasChange (String data, String oData) throws SAXException, IOException {
		XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(oData, data));
		return diff.getAllDifferences().size() > 0;
	}
}
