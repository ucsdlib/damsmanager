package edu.ucsd.library.xdre.tab;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class RDFExcelConvertor {
	private static final String[] requiredFields = {"Object Unique ID","Level","File name","File use","Type of Resource","Language","Title","Subtitle","Translation","Variant"};
	private String rdfSource = null;
	private String xsl = null;
	private InputStream xslInput = null;
	public RDFExcelConvertor(String rdfSource, String xsl) throws FileNotFoundException{
		this.rdfSource = rdfSource;
		this.xsl = xsl;
		this.xslInput = new FileInputStream(xsl);
	}

	public RDFExcelConvertor(String rdfSource, InputStream xslInput) throws FileNotFoundException{
		this.rdfSource = rdfSource;
		this.xslInput = xslInput;
	}

	/**
	 * Convert RDF to CSV
	 * @param xsl
	 * @return
	 * @throws FileNotFoundException
	 * @throws TransformerException
	 * @throws UnsupportedEncodingException
	 */
	public String convert2CSV () throws FileNotFoundException, TransformerException, UnsupportedEncodingException {
		Map<String, Integer> fieldCounts = new TreeMap<>();
		Map<String, List<Map<String,String>>> objectRecords = new HashMap<>();
		String jsonString = xslConvert(xslInput, rdfSource);

		ByteArrayInputStream input = null;
		try {
			input = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
			JSONObject jsonObj = (JSONObject) JSONValue.parse(new BufferedReader(new InputStreamReader(input)));
			for (Iterator<String> it = jsonObj.keySet().iterator(); it.hasNext();){
				String objID = it.next();
				Object data = jsonObj.get(objID);
				if (data instanceof JSONArray){
					List<Map<String,String>> objectRecord = new ArrayList<>();
					objectRecords.put(objID, objectRecord);
					parseRecord (objectRecord, fieldCounts, (JSONArray)data);
				}
			}
		}finally{
			if (StringUtils.isNotBlank(xsl)) {
				close (xslInput);
			}
			close (input);
		}

		// output string builder
		StringBuilder csvBuilder = new StringBuilder();
		// header output
		StringBuilder line = new StringBuilder();;
		for (String fieldName : requiredFields) {
			Integer count = fieldCounts.get(fieldName);
			appendFlatColumnNames(line, (count==null?1:count), fieldName);
		}

		List<String> requiredFieldsList = Arrays.asList(requiredFields);
		for (String fieldName : fieldCounts.keySet()) {
			if (requiredFieldsList.indexOf(fieldName) < 0) {
				Integer count = fieldCounts.get(fieldName);
				appendFlatColumnNames(line, (count==null?1:count), fieldName);
			}
		}
		csvBuilder.append(line.toString() + "\n");

		for (String key : objectRecords.keySet()) {
			List<Map<String, String>> rows = objectRecords.get(key);
			for(Map<String, String> row : rows ) {
				line = new StringBuilder();
				for (String fieldName : requiredFields) {
					Integer fieldCount = fieldCounts.get(fieldName);
					String fieldValues = row.get(fieldName);

					appendValues(line, (fieldCount==null?1:fieldCount), fieldValues);
				}
				
				// append all other fields
				for (String fieldName : fieldCounts.keySet()) {
					if (requiredFieldsList.indexOf(fieldName) < 0) {
						Integer fieldCount = fieldCounts.get(fieldName);
						String fieldValues = row.get(fieldName);

						appendValues(line, fieldCount, fieldValues);
					}
				}

				csvBuilder.append(line.toString() + "\n");
			}
		}
		return csvBuilder.toString();
	}

	private void parseRecord (List<Map<String, String>> objectRecord, Map<String, Integer> fieldCounts, JSONArray data) {
		Map<String, Integer> recFieldCounts = new HashMap<>();
		Map<String, String> dataRow = new TreeMap<>();
		objectRecord.add(dataRow);

		for (Object obj : data) {
			JSONObject dataObj = (JSONObject)obj;
			String columnName = (String)dataObj.keySet().iterator().next();

			if (columnName.startsWith("Component")) {

				JSONArray compObj = (JSONArray)dataObj.get(columnName);
				parseRecord (objectRecord, fieldCounts, compObj);
			} else {
				String columnValue = (String)dataObj.get(columnName);

				Integer count = recFieldCounts.get(columnName);
				if (count == null)
					count = new Integer(1);
				else
					count = count + 1;
				recFieldCounts.put(columnName, count);

				String fieldValues = dataRow.get(columnName);
				if (fieldValues == null) 
					fieldValues = columnValue;
				else
					fieldValues += "|" + columnValue;

				dataRow.put(columnName, fieldValues);
			}
		}

		// update the counts of each field
		updateFieldCounts(fieldCounts, recFieldCounts);
	}

	private void updateFieldCounts(Map<String, Integer> fieldCounts, Map<String, Integer> tmpCounts) {
		// update the counts of each field
		for (String fieldName : tmpCounts.keySet()) {
			Integer count = fieldCounts.get(fieldName);
			if (count == null || tmpCounts.get(fieldName) > fieldCounts.get(fieldName))
				fieldCounts.put(fieldName, new Integer(tmpCounts.get(fieldName).intValue()));
		}
	}

	private void appendValues(StringBuilder line, int fieldCount, String fieldValues) {
		if (StringUtils.isNotBlank(fieldValues)) {
			String[] values = fieldValues.split("\\|");

			Arrays.sort(values);
			for (String value : values) {
				if (line.length() > 1 || (line.length() == 1 && line.charAt(0) != ','))
					line.append(",");

				if (value.indexOf(",") >= 0 || value.indexOf("\"") >= 0 
						|| value.indexOf(System.getProperty("line.separator")) >= 0)
					line.append("\"" + value.replace("\"", "\"\"") + "\"");
				else
					line.append(value);
			}

			// append commas for extra fields
			if (fieldCount > values.length) {
				for (int i= values.length; i< fieldCount; i++) {
					line.append(",");
				}
			}	
		} else {
			for(int i=0; i<fieldCount; i++)
				line.append(",");
		}
	}

	private void appendFlatColumnNames(StringBuilder line, int fieldCount, String columnName) {
		for (int i=0; i< fieldCount; i++) {
			if (line.length() > 1)
				line.append(",");
	
			line.append(columnName);
		}
	}

	/**Convert source to other formats with xsl transform
	 * 
	 * @param xsl
	 * @param source
	 * @return
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public static String xslConvert(String xsl, String source) throws TransformerException, IOException {
		try (InputStream xslIn = new FileInputStream(xsl);) {
			return xslConvert(xslIn, source);
		}
	}


	/**Convert source to other formats with xsl transform
	 * 
	 * @param xslIn
	 * @param source
	 * @return
	 * @throws FileNotFoundException
	 * @throws TransformerException
	 */
	public static String xslConvert(InputStream xslIn, String source) throws FileNotFoundException, TransformerException {
		InputStream srcInput = null;
		OutputStream out = null;
		try{
			out = new ByteArrayOutputStream();
			Source xslSrc = new StreamSource(xslIn);
			TransformerFactory transFact = TransformerFactory.newInstance( );
			Templates xslTemp = transFact.newTemplates(xslSrc);
			Transformer trans = xslTemp.newTransformer();
			srcInput = new FileInputStream(source);
			Source xmlSource = new StreamSource(srcInput);
			StreamResult result = new StreamResult(out);
			trans.setOutputProperty("encoding", "UTF8");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.transform(xmlSource, result);
		}finally{
			close(srcInput);
			close(out);
		}
		return out.toString();
	}

	public static void close(Closeable closable){
		if(closable != null){
			try {
				closable.close();
			} catch (IOException e) {}
		}
		
	}
}
