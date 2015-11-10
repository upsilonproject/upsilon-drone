package upsilon.node.management.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum UpsilonMessageType {
	REQ_COMMAND_CREATE, REQ_SERVICE_CREATE, RES_NODE_SUMMARY, REQ_NODE_SUMMARY, HEARTBEAT, UNKNOWN;
	
	private static final transient Logger LOG = LoggerFactory.getLogger(UpsilonMessageType.class);

	public static UpsilonMessageType lookup(String typeString) {
		UpsilonMessageType ret;

		try {
			ret = UpsilonMessageType.valueOf(typeString);
		} catch (Exception e) {
			LOG.warn("Unknown message type: " + typeString);
			ret = UNKNOWN;
		}

		return ret;
	}
}