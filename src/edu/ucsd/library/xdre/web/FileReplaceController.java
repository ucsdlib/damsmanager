package edu.ucsd.library.xdre.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.collection.FileUploadHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DamsURI;

/**
 * Controller for replace file.
 * @author lsitu
 *
 */
public class FileReplaceController implements Controller{

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String message = "";
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();		

		String arkFile = request.getParameter("file");
		String filePath = request.getParameter("filesPath");

		FileUploadHandler handler = null;	
		try{
			if (StringUtils.isNotBlank(arkFile)) {
				damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);

				boolean fileExists = false;
				String fileUri = null;
				if (arkFile.indexOf("/ark:/") > 0) {
					// ark url: http://library.ucsd.edu/ark:/20775/oid/cid/fid, http://library.ucsd.edu/ark:/20775/oid/fid
					DamsURI damsURI = DamsURI.toParts(arkFile, null);
					fileExists = damsClient.exists(damsURI.getObject(), damsURI.getComponent(), damsURI.getFileName());
					fileUri = arkFile;
				} else {
					// ark file name or ark file url: oid_cid_fid, oid_fid, http://library.ucsd.edu/dc/object/oid/_cid_fid,
					// http://library.ucsd.edu/dc/object/oid/_fid. 
					String damsUrlBase = Constants.DAMS_ARK_URL_BASE + "/" + Constants.ARK_ORG;
					String[] parts = arkFile.split("/");
					String[] fileParts = parts[parts.length -1].split("_");

					if (fileParts.length == 2 || fileParts.length == 3) {
						String oid = "";
						String cid = "";
						String fid = "";
						if (fileParts.length == 3) {
							oid = StringUtils.isNotBlank(fileParts[0]) ? fileParts[0] : parts[parts.length - 2];
							cid = fileParts[1];
							fid = fileParts[2];
						} else {
							oid = StringUtils.isNotBlank(fileParts[0]) ? fileParts[0] : parts[parts.length - 2];
							fid = fileParts[1];
						}

						fileExists = damsClient.exists(oid, cid, fid);
						fileUri = damsUrlBase + "/" + oid + (StringUtils.isNotBlank(cid) ? "/" + cid : "") + "/" + fid;
					} else {
						message = "Invalid ark file name: " + arkFile;
					}
				}

				if (fileExists) {
					if (StringUtils.isNotBlank(filePath)) {
						File srcFile = new File(Constants.DAMS_STAGING + "/" + filePath.trim());
						if (srcFile.exists()) {
							Map<String, String> filesMap = new HashMap<>();
							filesMap.put(filePath.trim(), fileUri);
							handler = new FileUploadHandler(damsClient, filesMap);
							handler.setItems(Arrays.asList(filesMap.keySet().toArray(new String[filesMap.size()])));
							boolean successful = handler.execute();
							if (successful) {
								message = "Successfully replaced file " + arkFile + " with source file " + srcFile.getName() + ".";
								arkFile = "";
								filePath = "";
							} else {
								message = "Failed to replaced file " + arkFile + ".";
								String error = handler.getErrorReport();
								if (StringUtils.isNotBlank(error))
									message += " Error: " + error;
							}
						} else {
							message = "The selected source file doesn't exist: " + filePath;
						}
					}
				} else {
					message = "File doesn't exist: " + arkFile;
				}
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

		dataMap.put("file", arkFile);
		dataMap.put("filesPath", filePath);
		dataMap.put("message", message);
		return new ModelAndView("fileReplace", "model", dataMap);

	}
}
