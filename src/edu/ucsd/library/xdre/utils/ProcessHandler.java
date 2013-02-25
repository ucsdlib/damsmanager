package edu.ucsd.library.xdre.utils;

/**
 * Signitures for request processing
 *
 * @author lsitu@ucsd.edu
 * 
 */
public interface ProcessHandler {

	/**
	 * Process the request
	 * @return
	 * @throws Exception
	 */
	public boolean execute() throws Exception;
	
	/**
	 * Provide extra information regarding the execution
	 * @return 
	 */
	public String getExeInfo();
	
	/**
	 * Task invokation
	 * @return 
	 */
	public void invokeTask(String subjectId, int idx) throws Exception;
}
