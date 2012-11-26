package edu.ucsd.library.xdre.web;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DomUtil;
import edu.ucsd.library.xdre.utils.ProcessHandler;

public class MetadataImportController implements Controller{
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		boolean success = true;
		String message = "";
		String data = request.getParameter("data");
		String dataType = request.getParameter("type");
		String collectionId = request.getParameter("collectionId");
		String ds = request.getParameter("ds");
		//Operation: new --> populate and tag with the metadatapopulated tag
		//           renew -> remove all tags before populate the data;
		//           repopulate -> delete the subject before populate it, marked as updated
		//           add satements --> add statements, marked as updated
		String operation = request.getParameter("op");
		if(ds == null || (ds=ds.trim()).length() == 0)
			ds = Constants.DEFAULT_TRIPLESTORE;
		if(dataType == null || !(dataType=dataType.trim()).equalsIgnoreCase("RDF")){
			//success = false;
			message += "Unsupported data type: " + dataType + "\n";
		}
		if(operation.equalsIgnoreCase("reNew") && (collectionId == null || collectionId.length() != 10))
			message += "Errot: Invalid collection ID: " + collectionId;
		if((data == null || (data=data.trim()).length() == 0) && !"tsRenew".equalsIgnoreCase(operation)){
			//success = false;
			message += "Errot: Data required."+ "\n";;
		}
		if(operation == null)
			operation = "tsNew";
		if(message == null || message.length() == 0){
			ProcessHandler handler = null;
			try{
				if(operation.equalsIgnoreCase("tsNew") 
						|| operation.equalsIgnoreCase("tsRepopulation")
						|| operation.equalsIgnoreCase("tsRepopulateOnly")
						|| operation.equalsIgnoreCase("samePredicatesReplacement")){
					//XXX
					//handler = new RDFLoadingHandler(tsUtils, data, getOperationId(operation));
				} if(operation.equalsIgnoreCase("tsRenew")){
					//XXX
					//handler = new RDFLoadingHandler(tsUtils, collectionId, data, getOperationId(operation));
				}else {
					success = false;
					message = "Errot: Unsupported operation: " + operation;
				}
				success = handler.execute();
				message += handler.getExeInfo();
			}catch (Exception e){
				success = false;
				message += e.getMessage() + "\n";
			}
		}else
			success = false;

		Element resultElem = DomUtil.createElement(null, "result", null, null);
		DomUtil.createElement(resultElem, "status", null, String.valueOf(success));
		DomUtil.createElement(resultElem, "message", null, message);
		response.setContentType("text/xml");
		PrintWriter output= response.getWriter();
		output.write(resultElem.asXML());
		output.flush();
		output.close();		
		return null;
	}

	public static int getOperationId(String operation) throws Exception{
		if("TSNEW".equalsIgnoreCase(operation))
			return Constants.METADAT_NEW;
		else if("TSRENEW".equalsIgnoreCase(operation))
			return Constants.METADAT_RENEW;
		else if("TSREPOPULATION".equalsIgnoreCase(operation))
			return Constants.METADAT_REPOPULATE_ALL;
		else if("TSREPOPULATEONLY".equalsIgnoreCase(operation))
			return Constants.METADAT_REPOPULATE_ONLY;
		else if("SAMEPREDICATESREPLACEMENT".equalsIgnoreCase(operation))
			return Constants.METADAT_SAME_PREDICATE_REPLACEMENT;
		else throw new Exception("Unsupported opperation: " + operation);
	}
}
