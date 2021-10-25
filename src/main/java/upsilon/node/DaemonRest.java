package upsilon.node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.UnrecoverableKeyException;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import upsilon.node.util.ResourceResolver;

public class DaemonRest extends Daemon {
	private final Logger log = LoggerFactory.getLogger(DaemonRest.class);
	
	@Override
	public void run() {
		this.log.debug("DaemonREST not implemented");
	}

	@Override
	public void stop() {
		this.setStatus("stopped");
	}
}
