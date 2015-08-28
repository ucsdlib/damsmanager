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
		String data = request.getParameter("data").trim();
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
							// Collection or Authority update, need to update SOLR for the linked records. Recursive lookup for collections.
							queryObjects ( soluctions, subId, 1, damsClient, doc );
							List<String> solrFaileds = new ArrayList<>();
							for (String soluction : soluctions) {
								boolean successful = false;
								try {
									successful = damsClient.solrUpdate(soluction);
									if (!successful) {
										solrFaileds.add(soluction);
									}
								} catch (Exception e) {
									e.printStackTrace();
									solrFaileds.add(soluction);
								}
							}
							objLink = "Total " + (soluctions.size() - 1) + " records affected by ark " + subId.substring(subId.lastIndexOf("/") + 1) + ".";
							if (solrFaileds.size() > 0) {
								StringBuilder builder = new StringBuilder();
								for (String id : solrFaileds) {
									builder.append((builder.length() > 0 ? ", " : "") + id);
								}
								
								objLink += " But failed to add the following " + solrFaileds.size() + " records to the queue for SOLR update: \n" + builder.toString();
							}
						} else {
							String damsUrl = "http://" + Constants.CLUSTER_HOST_NAME + (Constants.CLUSTER_HOST_NAME.startsWith("localhost")?"":".ucsd.edu/dc") + "/object/"
									+ ark.substring(ark.lastIndexOf("/") + 1, ark.length());
							objLink = "View item <a href=\"" + damsUrl +  "\" target=\"dc\">" + ark + "</a>.";
						}
					}

					message = "Update record successfully. " + objLink;
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

	private void queryObjects( List<String> soluctions, String oid, int level, DAMSClient damsClient, Document parent ) throws Exception {
		int le = level;
		String[] sparqls =  getQueries(oid, level, parent);
		for (String sparql : sparqls) {
			if (!soluctions.contains(oid))
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
							// recursive lookup for authority records and collections hierarchy only
							if (doc.selectSingleNode("/rdf:RDF/*[contains(name(), 'Object') or contains(name(), 'Collection')]" ) == null
									|| (parent.selectSingleNode("/rdf:RDF/*[contains(name(), 'Collection')]") != null 
											&& doc.selectSingleNode("/rdf:RDF/*[contains(name(), 'Collection')]") != null)) {
	
								// update the authoritativeLabel for the ComplexSubject
								boolean isComplexSubject = doc.selectSingleNode("/rdf:RDF/mads:ComplexSubject") != null;
								if (isComplexSubject) {
									Document fullDoc = damsClient.getFullRecord(sub);
	
									List<Node> subElems = fullDoc.selectNodes("/rdf:RDF/mads:ComplexSubject/mads:componentList/*/mads:authoritativeLabel");
									if (subElems.size() > 0) {
										String authoritativeLabel = "";
										for ( Node subElem : subElems) {
											authoritativeLabel += (authoritativeLabel.length() > 0 ? "--" : "") + subElem.getText();
										}
										
										doc.selectSingleNode("/rdf:RDF/mads:ComplexSubject/mads:authoritativeLabel").setText(authoritativeLabel);
										MetadataImportHandler handler = new MetadataImportHandler(damsClient, null, doc.asXML(), null, Constants.IMPORT_MODE_ALL);
										
										try {
											if(handler.execute()) {
												// need to query linked records for the authority record
												queryObjects ( soluctions, sub, 1, damsClient, doc );
											} else
												throw new Exception ("Failed to update linked record " + sub);
										} catch (Exception e) {
											throw new Exception ("Failed to update linked record " + sub, e);
										}finally{
											if(handler != null) {
												handler.release();
												handler = null;
											}
										}
									}
								} else {
									queryObjects( soluctions, sub, 1, damsClient, doc );
								}
							}
							
						}catch (Exception e) {
							throw new Exception ("Failed to retrieve linked records for " + sub, e);
						}
					}
				}
			}
			
			if ( hasBn ) {
				// for blankNode ComplexSubject (mads elements), query five level down, otherwise query one level down
				Node node = parent.selectSingleNode("/rdf:RDF/*[string-length(@rdf:about) > 0]") ;
				if (node != null && node.getPath().indexOf("/mads:") >= 0 && le <= 1) {
					for (int l = 2; l <= 6; l++) 
						queryObjects ( soluctions, oid, l, damsClient, parent);
				} else
					queryObjects ( soluctions, oid, ++le, damsClient, parent);
					
			}
		}
	}

	private String[] getQueries (String oid, int level, Document doc) {
		String sparqlPrex = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX mads: <http://www.loc.gov/mads/rdf/v1#> " +
				"PREFIX dams: <http://library.ucsd.edu/ontology/dams#>";
		String bnId = "";
		int le = level;

		if (level <= 1 ) {
			String[] sparqls = {"SELECT ?sub WHERE { ?sub ?pre <" + oid + "> }"};
			return sparqls;
		} else if (level == 2 ){
			// to retrieve ComplexSubjects linked records
			String sparqlCS = sparqlPrex + " SELECT ?sub WHERE { ?sub mads:componentList ?bn . ?bn rdf:first <" + oid + "> }";
			// to retrieve Relationship linked records: names and roles
			String sparqlRS = sparqlPrex + " SELECT ?sub WHERE { ?sub dams:relationship ?bn . ?bn  ?pre <" + oid + "> } ";

			Node node = doc.selectSingleNode("/rdf:RDF/*[contains(name(), 'Name') or contains(name(), 'Authority')]");
			if ( node != null ) {
				String[] sparqls = {sparqlCS, sparqlRS};
				return sparqls;
			} else {
				String[] sparqls = {sparqlCS};
				return sparqls;
			}
		} else {
			String sparqlCS = sparqlPrex +  " SELECT ?sub WHERE { ";
			for ( int i = 0; i < le; i++ ) {
				// lookup linked records with blankNode: ComplexSubjects 
				if ( i == 0 ) {
					bnId = "_bn" + i;
					sparqlCS += " ?sub mads:componentList ?bn . ";
					sparqlCS += " ?bn rdf:rest ?" + bnId;
				} else if ( i == le - 1 ) {
					sparqlCS += " . ?" + bnId + " rdf:first <" + oid + "> ";
				} else {
					String tmpBnId = "_bn" + i;
					sparqlCS += " . ?" + bnId + " rdf:rest ?" + tmpBnId;
					bnId= tmpBnId;
				}
			}
			sparqlCS += " }";
			String[] sparqls = {sparqlCS};
			return sparqls;
		}
	}

	private boolean hasChange (String data, String oData) throws SAXException, IOException {
		XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(oData, data));
		return diff.getAllDifferences().size() > 0;
	}
}
