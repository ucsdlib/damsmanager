package edu.ucsd.library.xdre.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Node;

/**
 * Utility class to extract embedded metadata for audio
 * @author lsitu
 */
public class AudioMetadata extends EmbeddedMetadata{

	public AudioMetadata (DAMSClient damsClient) throws Exception {
		super(damsClient);
	}

	/**
	 * Extract metadata for audio
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

		// publisher
		metadata.put("publisher", oid + " - " + getPublisher (rdf.selectSingleNode("/rdf:RDF/dams:Object")));

		// copyright
		metadata.put("copyright", getCopyright (rdf.selectSingleNode("/rdf:RDF/dams:Object")));

		return metadata;
	}
}
