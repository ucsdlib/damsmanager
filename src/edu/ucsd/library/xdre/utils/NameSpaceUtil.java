package edu.ucsd.library.xdre.utils;

/**
 * Utility class for namespace subject mapping
 *
 * @author lsitu@ucsd.edu
 */

import java.util.HashMap;
import java.util.Map;

public class NameSpaceUtil {
	public static final int NS_SUBJECT_MAP = 1;
	public static final int SUBJECT_NS_MAP = 2;
    protected Map<String, String> subjectNSMap = null;
    protected Map<String, String> nsSubjectMap = null;
    protected String tripleStoreName = null;
    private static Map<String, NameSpaceUtil> NS_UTILS = null;
    protected NameSpaceUtil(String tripleStoreName) throws Exception {
    	this.tripleStoreName = tripleStoreName;
    	init();
    }

	protected void init() throws Exception{
		/*String[] vars = {"subject", "object"};
    	subjectNSMap = getPairsFromSparql(tsUtils, TripleStoreConstants.LOOKUP_NAMESPACES_ONLY_SPARCQL, vars);
    	nsSubjectMap = new HashMap<String, String>();
    	Iterator it = subjectNSMap.keySet().iterator();
    	while (it.hasNext()){
    		String key = (String) it.next();
    		String value = (String) subjectNSMap.get(key);
    		nsSubjectMap.put(value, key);
    	}*/

	}
	
	public static synchronized NameSpaceUtil getInstance(String tsName) throws Exception{
		NameSpaceUtil nsUtil = null;
		if(NS_UTILS != null)
			nsUtil = (NameSpaceUtil) NS_UTILS.get(tsName);
		else
			NS_UTILS = new HashMap<String, NameSpaceUtil>();
		
		if(nsUtil == null){
			nsUtil = new NameSpaceUtil(tsName);
			NS_UTILS.put(tsName, nsUtil);
		}
		return nsUtil;
	}
	
    public Map<String, String> getNSMap(int type){
    	switch(type){
	    	case NS_SUBJECT_MAP:
	    		return nsSubjectMap;
	    	default:
	    		return subjectNSMap;
    	}
    }
    
    public String getSubjectLabel(String subject){
    	return (String) subjectNSMap.get(subject);
    }
    
    public String getSubject(String label) throws Exception{
    	return (String) nsSubjectMap.get(label);
    }
}

