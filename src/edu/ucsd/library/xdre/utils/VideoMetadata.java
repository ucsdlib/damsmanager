package edu.ucsd.library.xdre.utils;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Node;

/**
 * Utility class to extract embedded metadata for videos
 * @author lsitu
 */
public class VideoMetadata extends EmbeddedMetadata{

	public VideoMetadata (DAMSClient damsClient) throws Exception {
		super(damsClient);
	}

	/**
	 * Extract metadata for video
	 * @param oid
	 * @param fileUri
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getMetadata(String oid, String fileUri) throws Exception {
		Map<String, String> metadata = new HashMap<>();
		Document rdf = damsClient.getFullRecord(oid, true);
		Node fileNode = rdf.selectSingleNode("//*[@rdf:about='" + fileUri + "']");

		Node containerNode = fileNode.getParent().getParent();

		// title
		metadata.put("title", getTile(containerNode));

		// artist
		metadata.put("artist", getArtist (containerNode));

		// album
		metadata.put("album", getAlbum (rdf.selectSingleNode("/rdf:RDF/dams:Object")));

		// artist
		metadata.put("date", getYear (containerNode));

		// comments and copyright
		String comments = getComments (rdf.selectSingleNode("/rdf:RDF/dams:Object"));
		String copyright = getCopyright (rdf.selectSingleNode("/rdf:RDF/dams:Object"));
		metadata.put("comment", oid + " - " + comments + ". " + copyright);

		return metadata;
	}
}
