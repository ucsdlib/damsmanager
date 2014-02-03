package edu.ucsd.library.xdre.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ReportFormater {
	private String csv = null;
	private String excelFile = null;
	public ReportFormater(String csv, String excelFile){
		this.csv = csv;
		this.excelFile = excelFile;
	}

	public HSSFWorkbook toExcel() throws IOException{
		HSSFWorkbook workbook = null;
		HSSFSheet sheet =  null;
		if(excelFile != null){
			InputStream in = null;
			try{
				in = new FileInputStream(excelFile);
				POIFSFileSystem fileSystem = new POIFSFileSystem (in);
				workbook = new HSSFWorkbook(fileSystem);
				sheet = workbook.getSheetAt(0);
			}finally{
				if(in != null){
					try{
						in.close();
					}catch(Exception e){}
				}
			}
		}
		
		if(workbook == null){
			workbook = new HSSFWorkbook();
			sheet = workbook.createSheet();
		}
		StringReader reader = null;
		BufferedReader bf = null;
		try{
			reader = new StringReader(csv);
			bf = new BufferedReader(reader);
			String line = null;
			String[] tokens = null;
			String delimiter = "\t";
			HSSFRow row = null;
			int rowCount = 0;//sheet.getLastRowNum() + 1;
			//if(sheet.getRow(rowCount) != null)
			//	rowCount += 1;
			while((line=bf.readLine()) != null){
				if(line.length() > 0){
					tokens = line.split(delimiter);
					row = sheet.getRow(rowCount);
					if(row == null)
						row = sheet.createRow((short)(rowCount));
					createRow(row, tokens, workbook);
					rowCount++;
				}
			}
		}finally{
			if(bf != null)
				try{bf.close();}catch (Exception e){}
			if(reader != null)
				try{reader.close();}catch (Exception e){}
		}
		return workbook;
	}
	
	private void createRow(HSSFRow row, String[] tokens, HSSFWorkbook workbook){
		HSSFCell cell = null;
		HSSFHyperlink link = null;
		int idx = -1;
		for(int i=0; i<tokens.length; i++){
			cell = row.getCell(i);
			if(cell == null)
				cell = row.createCell((short)i);
			//if( i>0 )
			//	cell.getCellStyle().setAlignment(HSSFCellStyle.ALIGN_CENTER);
			if(tokens[i] == null)
				tokens[i] = "";
			
			idx = tokens[i].indexOf("<a>");
			if(idx >= 0){
				//Add Link
				String url = tokens[i].substring(idx + 3);
				tokens[i] = tokens[i].substring(0, idx);
				link = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
		        link.setAddress(url);
		        cell.setHyperlink(link);
			}
			HSSFRichTextString text = new HSSFRichTextString(tokens[i]);
			cell.setCellValue(text);
		}	
	}
	
	public static SimpleDateFormat getDateFormat(){
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	}
	
	public static String getCurrentTimestamp() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = getDateFormat();
		String dateString = formatter.format(currentTime);
		return dateString;
	}
	
	public static void main(String[] args){
		String csv = "Collection	JETL Ingestion	Derivatives Creation	Metadata Population	METS Creation	METS Validation	METS Uploading	CDL Ingested\nBaja	X	X	X	X	X	";
		String excelFile = "C:\\Documents and Settings\\lsitu\\Desktop\\excel.xls";
		ReportFormater formater = new ReportFormater(csv, excelFile);
		try {
			HSSFWorkbook workbook = formater.toExcel();
			OutputStream out = new FileOutputStream("E:\\unzip\\excel.xls");
			workbook.write(out);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
