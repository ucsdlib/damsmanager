package edu.ucsd.library.xdre.collection;

import java.text.NumberFormat;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class CollectionReportHandler provide functionalities to 
 * generate reports on the percentage done for each process
 * @author lsitu@ucsd.edu
 */
public class CollectionReportHandler extends CollectionHandler
{
	public String percentChecksumsValidated = null;
	public String percentImagesWithDerivatives = null;
	public String percentThumbnailsCached = null;
	public String percentTripleStorePopulated = null;
	public String percentRdfXmlFilesCreated = null;
	public String percentLuceneIndexed = null;
	public String percentMetsFilesCreated = null;
	public String percentSentToCDL = null;
	public String percentSrbSynchronized = null;
	public String percentJsonCreated = null;
	public String percentJsonCached = null;	
	public String percentSentToFlickr = null;
	public Boolean displayStatus = false;
	
	public CollectionReportHandler(DAMSClient damsClient, String collectionId, boolean displayStatus) throws Exception {
		super(damsClient, collectionId);
		this.displayStatus = displayStatus;
	}	
	
	public String getPercentChecksumsValidated() {
		return percentChecksumsValidated;
	}

	public void setPercentChecksumsValidated(String percentChecksumsValidated) {
		this.percentChecksumsValidated = percentChecksumsValidated;
	}

	public String getPercentImagesWithDerivatives() {
		return percentImagesWithDerivatives;
	}

	public void setPercentImagesWithDerivatives(String percentImagesWithDerivatives) {
		this.percentImagesWithDerivatives = percentImagesWithDerivatives;
	}
    
	public String getPercentTripleStorePopulated() {
		return percentTripleStorePopulated;
	}

	public void setPercentTripleStorePopulated(String percentTripleStorePopulated) {
		this.percentTripleStorePopulated = percentTripleStorePopulated;
	}

	public String getPercentLuceneIndexed() {
		return percentLuceneIndexed;
	}

	public void setPercentLuceneIndexed(String percentLuceneIndexed) {
		this.percentLuceneIndexed = percentLuceneIndexed;
	}



	public String getPercentMetsFilesCreated() {
		return percentMetsFilesCreated;
	}

	public void setPercentMetsFilesCreated(String percentMetsFilesCreated) {
		this.percentMetsFilesCreated = percentMetsFilesCreated;
	}

	public String getPercentRdfXmlFilesCreated() {
		return percentRdfXmlFilesCreated;
	}

	public void setPercentRdfXmlFilesCreated(String percentRdfXmlFilesCreated) {
		this.percentRdfXmlFilesCreated = percentRdfXmlFilesCreated;
	}

	public String getPercentSentToCDL() {
		return percentSentToCDL;
	}

	public void setPercentSentToCDL(String percentSentToCDL) {
		this.percentSentToCDL = percentSentToCDL;
	}

	public String getPercentSrbSynchronized() {
		return percentSrbSynchronized;
	}

	public String getPercentJsonCached() {
		return percentJsonCached;
	}

	public void setPercentJsonCached(String percentJsonCached) {
		this.percentJsonCached = percentJsonCached;
	}

	public String getPercentJsonCreated() {
		return percentJsonCreated;
	}

	public void setPercentJsonCreated(String percentJsonCreated) {
		this.percentJsonCreated = percentJsonCreated;
	}

	public void setPercentSrbSynchronized(String percentSrbSynchronized) {
		this.percentSrbSynchronized = percentSrbSynchronized;
	}

	public String getPercentThumbnailsCached() {
		return percentThumbnailsCached;
	}

	public void setPercentThumbnailsCached(String percentThumbnailsCached) {
		this.percentThumbnailsCached = percentThumbnailsCached;
	}

	public boolean execute() throws Exception{

		//xxx
		return true;
	}
	
	
	public String truncateDouble(double doubleValue) {
	    NumberFormat numberFormat = NumberFormat.getInstance();
	    numberFormat.setMaximumFractionDigits(2);
	    return numberFormat.format(doubleValue);
	}
	
	public String getExeInfo() {
		return null;
	}

	public String getPercentSentToFlickr() {
		return percentSentToFlickr;
	}

	public void setPercentSentToFlickr(String percentSentToFlickr) {
		this.percentSentToFlickr = percentSentToFlickr;
	}
}