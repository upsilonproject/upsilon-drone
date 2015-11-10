package upsilon.node.management.amqp;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;

import upsilon.node.Main;
import upsilon.node.configuration.ValidatedConfiguration;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;

public class MessageHandler {
	private static final transient Logger LOG = LoggerFactory.getLogger(MessageHandler.class);

	public static BasicProperties getNewMsgProps(final UpsilonMessageType messageType) {
		return MessageHandler.getNewMsgPropsBuilder(messageType).build();
	}

	public static Builder getNewMsgPropsBuilder(final UpsilonMessageType messageType) {
		final Map<String, Object> headers = new HashMap<>();
		headers.put("upsilon-msg-type", messageType.toString());
		headers.put("version", Main.getVersion());
		headers.put("identifier", Main.instance.node.getIdentifier());

		final Builder builder = new BasicProperties.Builder();
		builder.headers(headers);

		return builder;
	}

	public void handleMessageType(final UpsilonMessageType type, final String body, final long deliveryTag, final String replyTo, final Channel channel) throws Exception {
		switch (type) {
		case REQ_NODE_SUMMARY:
			String nodeSummary = "";
			nodeSummary += "Version: " + Main.getVersion() + "\n";
			nodeSummary += "Identifier: " + Main.instance.node.getIdentifier() + "\n";
			nodeSummary = nodeSummary.trim();

			MessageHandler.LOG.debug("req version summary, sending back:\n" + nodeSummary);

			final BasicProperties props = MessageHandler.getNewMsgProps(UpsilonMessageType.RES_NODE_SUMMARY);

			channel.basicPublish(DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.res", props, nodeSummary.getBytes());

			channel.basicAck(deliveryTag, false);
			break;
		case REQ_COMMAND_CREATE:
			break;
		case REQ_SERVICE_CREATE:
			final XmlConfigurationValidator validator = new XmlConfigurationValidator(body, true);
			final ValidatedConfiguration vcfg = validator.getValidatedConfiguration();

			final XmlConfigurationLoader loader = new XmlConfigurationLoader();
			loader.load(vcfg, false);

			System.out.println("service create SS");

			break;
		case UNKNOWN:
		default:
			MessageHandler.LOG.warn("Unsupported upsilon message type: " + type);
			channel.basicAck(deliveryTag, false);
			break;
		}
	}
}
