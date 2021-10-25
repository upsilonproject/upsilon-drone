package upsilon.node.management.rest.client;

import java.net.URL;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.node.Main;
import upsilon.node.dataStructures.StructureNode;
import upsilon.node.dataStructures.StructureRemoteService;
import upsilon.node.dataStructures.StructureService;
import upsilon.node.util.SslUtil;

public class RestClient {
	private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

	public RestClient(final URL uri) throws IllegalArgumentException {
		RestClient.LOG.info("Creating new REST client");

		if (uri.getPort() == 0) {
			throw new IllegalArgumentException("The port for the remote host URL in a rest client is not valid: " + uri.getPort());
		}

		if (uri.getHost().isEmpty()) {
			throw new IllegalArgumentException("The host part for the remote host URL in a rest client is not valid: " + uri.getHost());
		}

		// Not implemented

	}

	public StructureService getService(final String identifier) {
		return null;
	}

	public void postNode(final StructureNode node) {
		if (!node.isPeerUpdateRequired()) {
			return;
		}

		RestClient.LOG.debug("Peer update required, posting node: " + node);

		// Not Implemented anymore.
	}

	public void postService(final StructureService s) {
		if (!s.isRegistered()) {
			return;
		}

		if (!s.isPeerUpdateRequired()) {
			return;
		}

		// Not Implemented anymore.

		return;
	}
}
