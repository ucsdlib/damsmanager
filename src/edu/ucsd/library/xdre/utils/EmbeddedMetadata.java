package edu.ucsd.library.xdre.utils;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Node;

/**
 * Abstract class to extract embedded metadata
 * @author lsitu
 */
public abstract class EmbeddedMetadata {
	protected DAMSClient damsClient;

	public EmbeddedMetadata (DAMSClient damsClient) throws Exception {
		this.damsClient = damsClient;
	}

	/**
	 * Extract metadata for audio
	 * @param oid
	 * @param fileUri
	 * @return
	 * @throws Exception
	 */
	public abstract Map<String, String> getMetadata(String oid, String fileUri) throws Exception;

	protected String getTile(Node node) {
		String title = "";
		Node tNode = node;
		while (tNode != null) {
			Node labelNode = tNode.selectSingleNode("dams:title/mads:Title/mads:authoritativeLabel");
			if (labelNode != null)
				title = labelNode.getText() + (title.length() > 0 ? ": " : "") + title;
			tNode = tNode.getParent();
		}
		return title;
	}

	protected String getArtist (Node node) {
		String artists = "";
		List<Node> nodes = node.selectNodes("dams:relationship/dams:Relationship");
		for (Node n : nodes) {
			Node lNode = n.selectSingleNode("*[contains(local-name(), 'Name')]//mads:authoritativeLabel");
			if (lNode != null)
				artists += (artists.length() > 0 ? ", " : "") + lNode.getText();
			lNode = n.selectSingleNode("dams:role//mads:authoritativeLabel");
			if (lNode != null)
				artists += " (" + lNode.getText() + ")";
			else {
				// role with resources id
				Node ridNode = n.selectSingleNode("dams:role/@rdf:resource");
				if (ridNode != null) {
					lNode = n.selectSingleNode("//dams:role/mads:Authority[@rdf:about='" + ridNode.getStringValue() + "']/mads:authoritativeLabel");
					if (lNode != null)
						artists += " (" + lNode.getText() + ")";
				}
			}
		}

		// retrieve from top level object if no artists found in the component level
		if (StringUtils.isBlank(artists)) {
			Node objNode = node.getDocument().selectSingleNode("/rdf:RDF/dams:Object");
			if (objNode != node)
				artists = getArtist (objNode);
		}
		return artists;
	}

	protected String getAlbum (Node node) {
		String album = "";
		Node albumNode = node.selectSingleNode("*[contains(local-name(), 'Collection')]//dams:title/mads:Title/mads:authoritativeLabel");
		if (albumNode != null)
			album = albumNode.getText();
		return album;
	}

	protected String getYear (Node node) {
		String year = "";
		Node yNode = node.selectSingleNode("dams:date/dams:Date/dams:beginDate");
		if (yNode != null) {
			year = yNode.getText();
		}

		// retrieve from top level object if no year found in the component level
		if (StringUtils.isBlank(year)) {
			Node objNode = node.getDocument().selectSingleNode("/rdf:RDF/dams:Object");
			if (objNode != node)
				year = getYear (objNode);
		}
		return year;
	}

	protected String getPublisher (Node node) {
		return getComments(node);
	}

	protected String getComments (Node node) {
		String publisher = "";
		Node publisherNode = node.selectSingleNode("dams:note/dams:Note[dams:type='local attribution']/rdf:value");
		if (publisherNode != null)
			publisher = publisherNode.getText();
		return publisher;
	}

	protected String getCopyright (Node node) {
		String copyright = "";
		Node rightsHolderNode = node.selectSingleNode("*[contains(local-name(), 'rightsHolder')]//mads:authoritativeLabel");
		if (rightsHolderNode != null)
			copyright = "© " + rightsHolderNode.getText() + ". ";
		Node copyrightNode = node.selectSingleNode("dams:copyright/dams:Copyright/dams:copyrightNote");
		if (copyrightNode != null)
			copyright += copyrightNode.getText();
		return copyright;
	}
}
