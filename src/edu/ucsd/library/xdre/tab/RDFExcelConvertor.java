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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RDFExcelConvertor {
	private static final String[] requiredFields = {"Object Unique ID","Level","File name","File use","Type of Resource","Language","Title"};
	private static final String[] groupFields = {"Date:collected","Date:creation","Date:issued","Begin date","End date"};

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
	 * @return
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public String convert2CSV () throws TransformerException, IOException {
		return convert2CSV (false);
	}

	/**
	 * Convert RDF to CSV
	 * @param mutiValueField: if true values delimited by character pipe (|).
	 * @return
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public String convert2CSV (boolean mutiValuesField)
			throws TransformerException, IOException {
		Map<String, Integer> fieldCounts = new TreeMap<>();
		Map<String, List<Map<String,String>>> objectRecords = new HashMap<>();

		// convert object data to Excel header format
		convertRecords(objectRecords, fieldCounts);

		// output string builder
		StringBuilder csvBuilder = new StringBuilder();
		// header output
		StringBuilder line = new StringBuilder();;
		for (String fieldName : requiredFields) {
			Integer count = getFieldCounts(fieldName, fieldCounts, mutiValuesField, 1);
			appendFlatColumnNames(line, count, fieldName);
		}

		// group headers
		for (String fieldName : groupFields) {
			Integer count = getFieldCounts(fieldName, fieldCounts, mutiValuesField, 0);
			appendFlatColumnNames(line, count, fieldName);
		}

		List<String> requiredFieldsList = Arrays.asList(requiredFields);
		List<String> groupFieldsList = Arrays.asList(groupFields);
		// other headers
		for (String fieldName : fieldCounts.keySet()) {
			if (requiredFieldsList.indexOf(fieldName) < 0 && groupFieldsList.indexOf(fieldName) < 0) {
				Integer count = getFieldCounts(fieldName, fieldCounts, mutiValuesField, 1);
				appendFlatColumnNames(line, count, fieldName);
			}
		}
		csvBuilder.append(line.toString() + "\n");

		for (String key : objectRecords.keySet()) {
			List<Map<String, String>> rows = objectRecords.get(key);
			for(Map<String, String> row : rows ) {
				line = new StringBuilder();
				for (String fieldName : requiredFields) {
					Integer fieldCount = getFieldCounts(fieldName, fieldCounts, mutiValuesField, 1);
					String fieldValues = row.get(fieldName);

					appendValues(line, fieldCount, fieldValues, mutiValuesField);
				}

				for (String fieldName : groupFields) {
					Integer fieldCount = getFieldCounts(fieldName, fieldCounts, mutiValuesField, 0);
					String fieldValues = row.get(fieldName);

					appendValues(line, fieldCount, fieldValues, mutiValuesField);
				}

				// append all other fields
				for (String fieldName : fieldCounts.keySet()) {
					if (requiredFieldsList.indexOf(fieldName) < 0 && groupFieldsList.indexOf(fieldName) < 0) {
						Integer fieldCount = getFieldCounts(fieldName, fieldCounts, mutiValuesField, 1);
						String fieldValues = row.get(fieldName);

						appendValues(line, fieldCount, fieldValues, mutiValuesField);
					}
				}

				csvBuilder.append(line.toString() + "\n");
			}
		}
		return csvBuilder.toString();
	}

	/*
	 * Calculate the field counts of a field:
	 * @param fieldName
	 * @param fieldCounts
	 * @param mutiValuesField
	 * @return Integer:
	 *  when fieldCount == null || fieldCount == 0: defaultCount
	 *  when mutiValuesField: 1
	 */
	private int getFieldCounts(String fieldName, Map<String, Integer> fieldCounts,
			boolean mutiValuesField, int defaultCount) {
		Integer fieldCount = fieldCounts.get(fieldName);
		return fieldCount == null || fieldCount == 0 ? defaultCount : mutiValuesField ? 1 : fieldCount;
	}

	/**
	 * Convert RDF to Excel format
	 * @param xsl
	 * @return
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public Workbook convert2Excel() throws TransformerException, IOException {
		Map<String, Integer> fieldCounts = new TreeMap<>();
		Map<String, List<Map<String,String>>> objectRecords = new HashMap<>();

		// convert object data to Excel header.
		convertRecords(objectRecords, fieldCounts);

		int rowIndex = 0;
		int cellIndex = 0;
		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet();
		XSSFRow xssfRow = sheet.createRow(rowIndex++);

		// header output
		for (String fieldName : requiredFields) {
			Integer count = fieldCounts.get(fieldName) == null ? 1 : fieldCounts.get(fieldName);
 			for (int i = 0; i < count; i++) {
				addCell(xssfRow, cellIndex++, fieldName);
			}
		}

		// date group headers
		for (String fieldName : groupFields) {
			Integer count = fieldCounts.get(fieldName) == null ? 0 : fieldCounts.get(fieldName);
			for (int i = 0; i < count; i++) {
				addCell(xssfRow, cellIndex++, fieldName);
			}
		}

		List<String> requiredFieldsList = Arrays.asList(requiredFields);
		List<String> groupFieldsList = Arrays.asList(groupFields);
		// other headers
		for (String fieldName : fieldCounts.keySet()) {
			if (requiredFieldsList.indexOf(fieldName) < 0 && groupFieldsList.indexOf(fieldName) < 0) {
				Integer count = fieldCounts.get(fieldName) == null ? 1 : fieldCounts.get(fieldName);
				for (int i = 0; i < count; i++) {
					addCell(xssfRow, cellIndex++, fieldName);
				}
			}
		}

		for (String key : objectRecords.keySet()) {
			List<Map<String, String>> rows = objectRecords.get(key);
			for(Map<String, String> row : rows ) {
				cellIndex = 0;
				xssfRow = sheet.createRow(rowIndex++);

				for (String fieldName : requiredFields) {
					Integer fieldCount = fieldCounts.get(fieldName) == null ? 1 : fieldCounts.get(fieldName);
					addCells(xssfRow, cellIndex, fieldCount, row.get(fieldName));
					cellIndex += fieldCount;
				}

				for (String fieldName : groupFields) {
					Integer fieldCount = fieldCounts.get(fieldName) == null ? 0 : fieldCounts.get(fieldName);
					addCells(xssfRow, cellIndex, fieldCount, row.get(fieldName));
					cellIndex += fieldCount;
				}

				// append all other fields
				for (String fieldName : fieldCounts.keySet()) {
					if (requiredFieldsList.indexOf(fieldName) < 0 && groupFieldsList.indexOf(fieldName) < 0) {
						Integer fieldCount = fieldCounts.get(fieldName) == null ? 1 : fieldCounts.get(fieldName);
						addCells(xssfRow, cellIndex, fieldCount, row.get(fieldName));
						cellIndex += fieldCount;
					}
				}
			}
		}
		return wb;
	}

	private void addCell(XSSFRow row, int cellIndex, String cellValue) {
		XSSFCell cell = row.createCell(cellIndex);
		cell.setCellValue(row.getSheet().getWorkbook().getCreationHelper().createRichTextString(cellValue));
	}

	/*
	 * Add cells for multiple values, blank when values are less than the max field count.
	 * @param row
	 * @param cellIndex
	 * @param fieldCount
	 * @param fieldValues
	 */
	private void addCells(XSSFRow row, int cellIndex, int fieldCount, String fieldValues) {
		if (StringUtils.isNotBlank(fieldValues)) {
			String[] values = fieldValues.split("\\|");

			Arrays.sort(values);
			for (String value : values) {
				addCell(row, cellIndex++, value);
			}

			// append extra fields
			if (fieldCount > values.length) {
				for (int i = values.length; i < fieldCount; i++) {
					addCell(row, cellIndex++, "");
				}
			}
		} else {
			for(int i = 0; i < fieldCount; i++) {
				addCell(row, cellIndex++, "");
			}
		}
	}

	/*
	 * Convert rdf data to Excel header format
	 * @param objectRecords
	 * @param fieldCounts
	 * @throws TransformerException
	 * @throws IOException
	 */
	private void convertRecords(Map<String, List<Map<String,String>>> objectRecords, Map<String, Integer> fieldCounts)
			throws TransformerException, IOException {
		String jsonString = null;

		try {
			jsonString = xslConvert(xslInput, rdfSource);
			JSONObject jsonObj = parseJson(jsonString);

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
		}
	}

	/**
	 * Parse the converted JSON stringL
	 * @param jsonString
	 * @return JSONObject
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 * @throws TransformerException 
	 */
	public static JSONObject parseJson(String jsonString) throws IOException, UnsupportedEncodingException, TransformerException {
		try (ByteArrayInputStream input = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));) {
			return (JSONObject) new JSONParser().parse(new InputStreamReader(input));
		} catch (ParseException ex) {
			String error = "Error parse json: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage());
			throw new TransformerException (error + "\nSource: " + jsonString);
		}
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

	private void appendValues(StringBuilder line, int fieldCount, String fieldValues, boolean multiValuesField) {
		if (StringUtils.isNotBlank(fieldValues)) {
			String[] values = fieldValues.split("\\" + TabularRecord.DELIMITER);
			if (multiValuesField) {
				String strVal = concatValues(values, " " + TabularRecord.DELIMITER + " ");
				values = new String[]{ strVal };
			}

			Arrays.sort(values);
			for (String value : values) {
				if (line.length() > 1 || (line.length() == 1 && line.charAt(0) != ','))
					line.append(",");

				line.append(escapeCsv(value));
			}

			// append commas for extra fields
			if (!multiValuesField && fieldCount > values.length) {
				for (int i= values.length; i< fieldCount; i++) {
					line.append(",");
				}
			}	
		} else {
			for(int i=0; i<fieldCount; i++)
				line.append(",");
		}
	}

	/*
	 * Join array elements into string with delimiter.
	 * @param values
	 * @param delimiter
	 * @return
	 */
	private static String concatValues(String[] values, String delimiter) {
	    StringBuilder builder = new StringBuilder();
	    for (String v : values) {
	        if (builder.length() > 0)
	            builder.append(delimiter);
	        builder.append(v);
	    }
	    return builder.toString();
	}

	private void appendFlatColumnNames(StringBuilder line, int fieldCount, String columnName) {
		for (int i=0; i< fieldCount; i++) {
			if (line.length() > 1)
				line.append(",");
	
			line.append(escapeCsv(columnName));
		}
	}

	/*
	 * Encode value in CSV format
	 * @param value
	 */
	public static String escapeCsv(String value) {
		if (value.indexOf(",") >= 0 || value.indexOf("\"") >= 0 
			|| value.indexOf(System.getProperty("line.separator")) >= 0)
			return "\"" + value.replace("\"", "\"\"") + "\"";
		else
			return value;
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
