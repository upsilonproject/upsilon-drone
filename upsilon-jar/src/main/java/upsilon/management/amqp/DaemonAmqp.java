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
	private QueueingConsumer consumer;

	private boolean run = true;

	private Channel channel;

	private final String EXCHANGE_NAME = "ex_upsilon";

	private final String ROUTING_KEY = "upsilon";

	private String QUEUE_NAME;

	public DaemonAmqp() throws Exception {

		this.start();
	}

	private String generateQueueName() {
		return "upsilon-node-" + UUID.randomUUID().toString();
	}

	private void handleMessageType(UpsilonMessageType type, String body, long deliveryTag, String replyTo) throws IOException {
		switch (type) {
		case REQ_NODE_SUMMARY:
			String nodeSummary = "";
			nodeSummary += "Version: " + Main.getVersion() + "\n";
			nodeSummary += "Identifier: " + Main.instance.node.getIdentifier() + "\n";
			nodeSummary = nodeSummary.trim();

			DaemonAmqp.LOG.debug("req version summary, sending back:\n" + nodeSummary);

			this.publish(UpsilonMessageType.RES_NODE_SUMMARY, nodeSummary);
			this.channel.basicAck(deliveryTag, false);
			break;
		case RES_NODE_SUMMARY:
			DaemonAmqp.LOG.debug("res version summary:\n" + body);
			this.channel.basicAck(deliveryTag, false);
			break;
		case UNKNOWN:
			this.channel.basicAck(deliveryTag, false);
			break;
		default:
			DaemonAmqp.LOG.warn("Unsupported upsilon message type: " + type);
		}
	}

	private void publish(UpsilonMessageType messageType, byte[] body) throws IOException {
		Map<String, Object> headers = new HashMap<>();
		headers.put("upsilon-msg-type", messageType.toString());

		Builder builder = new BasicProperties.Builder();
		builder.headers(headers);

		this.channel.basicPublish(this.EXCHANGE_NAME, this.ROUTING_KEY, builder.build(), body);
	}

	private void publish(UpsilonMessageType messageType, String body) throws IOException {
		this.publish(messageType, body.getBytes());
	}

	@Override
	public void run() {
		try {
			this.channel.basicConsume(this.QUEUE_NAME, false, "upsilon-node Message Consumer", this.consumer);

			while (this.run) {
				QueueingConsumer.Delivery delivery = this.consumer.nextDelivery();

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
			Connection connection = factory.newConnection();
			this.channel = connection.createChannel();

			this.channel.exchangeDeclare(this.EXCHANGE_NAME, "fanout");

			this.QUEUE_NAME = this.generateQueueName();

			Map<String, Object> queueArgs = new HashMap<String, Object>();
			queueArgs.put("upsilon-version", Main.getVersion());
			queueArgs.put("upsilon-node-identifier", Main.instance.node.getIdentifier());

			this.channel.queueDeclare(this.QUEUE_NAME, false, false, true, queueArgs);
			this.channel.queueBind(this.QUEUE_NAME, this.EXCHANGE_NAME, this.ROUTING_KEY);

			this.consumer = new QueueingConsumer(this.channel);
		} catch (Exception e) {
			DaemonAmqp.LOG.error("Could not complete initial AMQP binding: " + e.getMessage(), e);
			return;
		}

		DaemonAmqp.LOG.info("AMQP connection looks good.");

		this.run = true;
	}

	@Override
	public void stop() {
		this.run = false;
	}
}
