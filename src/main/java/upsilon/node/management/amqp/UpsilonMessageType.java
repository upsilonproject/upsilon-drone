package upsilon.node.management.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum UpsilonMessageType {
	REQ_COMMAND_CREATE, UPDATED_NODE_CONFIG, REQ_SERVICE_CREATE, RES_NODE_SUMMARY, REQ_NODE_SUMMARY, HEARTBEAT, SERVICE_CHECK_RESULT, EXECUTE_SINGLE, UNKNOWN;
	
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
