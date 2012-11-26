package edu.ucsd.library.xdre.utils;

/**
 * Class StringOperations wrap up functions for the invocation of a string functions
 * @author lsitu
 *
 */
public class StringOperations {
	public String substring(String value, Integer beginIndex){
		return value.substring(beginIndex.intValue());
	}
	
	public String substring(String value, Integer beginIndex, Integer endIndex){
		return value.substring(beginIndex.intValue(), endIndex.intValue());
	}
	
	public String substringbefore(String value, String expr){
		int index = value.indexOf(expr);
		if(index > 0)
			return value.substring(0, index);
		else
			return "";
	}
	
	public String substringafter(String value, String expr){
		int index = value.indexOf(expr);
		if(index >= 0)
			return value.substring(index + expr.length());
		else
			return "";
	}
	
	public int indexOf(String value, String expr){
		return value.indexOf(expr);
	}
	
	public int lastIndexOf(String value, String expr){
		return value.lastIndexOf(expr);
	}
}
