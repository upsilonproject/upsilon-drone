package upsilon.management.amqp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.Configuration;
import upsilon.Daemon;
import upsilon.Main;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.QueueingConsumer;

public class DaemonAmqp extends Daemon implements Runnable {
	private enum UpsilonMessageType {
		RES_NODE_SUMMARY, REQ_NODE_SUMMARY, UNKNOWN;

		public static UpsilonMessageType lookup(String typeString) {
			UpsilonMessageType ret;

			try {
				ret = UpsilonMessageType.valueOf(typeString);
			} catch (Exception e) {
				DaemonAmqp.LOG.warn("Unknown message type: " + typeString);
				ret = UNKNOWN;
			}

			return ret;
		}
	}

	private static final transient Logger LOG = LoggerFactory.getLogger(DaemonAmqp.class);
	private QueueingConsumer consumerRecv;

	private boolean run = true;

	private Channel channelRecv;

	private final String EXCHANGE_NAME = "ex_upsilon";

	private String QUEUE_NAME_RECV;
	private Connection connection;

	public DaemonAmqp() throws Exception {

		this.start();
	}

	private String generateQueueName(String suffix) {
		return "upsilon-node-" + UUID.randomUUID().toString() + "-" + suffix;
	}

	private BasicProperties getNewMsgProps(UpsilonMessageType messageType) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("upsilon-msg-type", messageType.toString());
		headers.put("version", Main.getVersion());
		headers.put("identifier", Main.instance.node.getIdentifier());

		Builder builder = new BasicProperties.Builder();
		builder.headers(headers);

		return builder.build();
	}

	private void handleMessageType(UpsilonMessageType type, String body, long deliveryTag, String replyTo) throws IOException {
		switch (type) {
		case REQ_NODE_SUMMARY:
			String nodeSummary = "";
			nodeSummary += "Version: " + Main.getVersion() + "\n";
			nodeSummary += "Identifier: " + Main.instance.node.getIdentifier() + "\n";
			nodeSummary = nodeSummary.trim();

			DaemonAmqp.LOG.debug("req version summary, sending back:\n" + nodeSummary);

			BasicProperties props = this.getNewMsgProps(UpsilonMessageType.RES_NODE_SUMMARY);

			Channel channelSend = this.connection.createChannel();
			channelSend.queueBind(replyTo, this.EXCHANGE_NAME, "upsilon.res");
			channelSend.basicPublish(this.EXCHANGE_NAME, "upsilon.res", props, nodeSummary.getBytes());
			channelSend.close();

			this.channelRecv.basicAck(deliveryTag, false);
			break;
		case RES_NODE_SUMMARY:
			DaemonAmqp.LOG.debug("res version summary:\n" + body);
			this.channelRecv.basicAck(deliveryTag, false);
			break;
		case UNKNOWN:
			this.channelRecv.basicAck(deliveryTag, false);
			break;
		default:
			DaemonAmqp.LOG.warn("Unsupported upsilon message type: " + type);
		}
	}

	@Override
	public void run() {
		try {
			this.channelRecv.basicConsume(this.QUEUE_NAME_RECV, false, "upsilon-node Message Consumer", this.consumerRecv);

			while (this.run) {
				QueueingConsumer.Delivery delivery = this.consumerRecv.nextDelivery();

				String body = new String(delivery.getBody());
				Map<String, Object> headers = delivery.getProperties().getHeaders();

				if ((headers == null) || !headers.containsKey("upsilon-msg-type")) {
					DaemonAmqp.LOG.warn("recv non-upsilon message: " + body);
				} else {
					String typeString = ((LongString) headers.get("upsilon-msg-type")).toString();

					UpsilonMessageType type = UpsilonMessageType.lookup(typeString);
					DaemonAmqp.LOG.debug("recv type: " + typeString + " (enum=" + type + ") body: " + body.getBytes().length + " bytes long");

					String replyTo = delivery.getProperties().getReplyTo();

					this.handleMessageType(type, body, delivery.getEnvelope().getDeliveryTag(), replyTo);
				}
			}
		} catch (Exception e) {
			DaemonAmqp.LOG.error("AMQP daemon: " + e.toString());
			e.printStackTrace();
			this.stop();
		}
	}

	public void start() throws Exception {
		final String hostname = Configuration.instance.amqpHostname;
		DaemonAmqp.LOG.info("Starting the AMQP listener, connecting to host: " + hostname);

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(hostname);

		try {
			Map<String, Object> queueArgs = new HashMap<String, Object>();
			queueArgs.put("upsilon-version", Main.getVersion());
			queueArgs.put("upsilon-node-identifier", Main.instance.node.getIdentifier());

			this.connection = factory.newConnection();
			Channel channelAdmin = this.connection.createChannel();
			channelAdmin.exchangeDeclare(this.EXCHANGE_NAME, "x-federation-upstream", true);
			channelAdmin.close();

			this.QUEUE_NAME_RECV = this.generateQueueName("recv");

			this.channelRecv = this.connection.createChannel();
			this.channelRecv.queueDeclare(this.QUEUE_NAME_RECV, false, false, true, queueArgs);
			this.channelRecv.queueBind(this.QUEUE_NAME_RECV, this.EXCHANGE_NAME, "upsilon.cmds");
			this.consumerRecv = new QueueingConsumer(this.channelRecv);
		} catch (Exception e) {
			DaemonAmqp.LOG.error("Could not complete initial AMQP binding: " + e.getMessage(), e);
			return;
		}

		DaemonAmqp.LOG.info("AMQP connection looks good, my recv queue is: " + this.QUEUE_NAME_RECV);

		this.run = true;
	}

	@Override
	public void stop() {
		this.run = false;
	}
}
