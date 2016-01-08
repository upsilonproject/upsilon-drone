package upsilon.node.management.amqp;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.QueueingConsumer;

import upsilon.node.Configuration;
import upsilon.node.Daemon;
import upsilon.node.Database;
import upsilon.node.Main;
import upsilon.node.util.Util;

public class DaemonConnectionHandler extends Daemon implements Runnable {
	private static final transient Logger LOG = LoggerFactory.getLogger(DaemonConnectionHandler.class);
	public static final String EXCHANGE_NAME = "ex_upsilon";

	public static final DaemonConnectionHandler instance = new DaemonConnectionHandler();
	private QueueingConsumer consumerRecv;
	private boolean run = true;
	private Channel channel;
	private String QUEUE_NAME_RECV;

	private Connection connection;

	private final MessageHandler handler = new MessageHandler();

	public DaemonConnectionHandler() {
		this.connect();
	}

	public void connect() {
		if ((this.connection != null) && this.connection.isOpen()) {
			return;
		}

		final String hostname = Configuration.instance.amqpHostname;
		DaemonConnectionHandler.LOG.info("Starting the AMQP listener, connecting to host: " + hostname);

		final ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(hostname);

		try {
			final Map<String, Object> queueArgs = new HashMap<String, Object>();
			queueArgs.put("upsilon-node-version", Main.getVersion());
			queueArgs.put("upsilon-node-identifier", Main.instance.node.getIdentifier());

			this.connection = factory.newConnection();
			final Channel channelAdmin = this.connection.createChannel();
			channelAdmin.exchangeDeclare(DaemonConnectionHandler.EXCHANGE_NAME, "topic", true);
			channelAdmin.close();
 
			this.QUEUE_NAME_RECV = this.generateQueueName("recv");

			this.channel = this.connection.createChannel();
			this.channel.queueDeclare(this.QUEUE_NAME_RECV, false, false, true, queueArgs);
			this.channel.queueBind(this.QUEUE_NAME_RECV, DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.cmds");
			
			if (Database.instance != null) {
				LOG.info("Active database connection, will subsuscribe to addtional AMQP routes"); 
				this.channel.queueBind(this.QUEUE_NAME_RECV, DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.res"); 
				this.channel.queueBind(this.QUEUE_NAME_RECV, DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.node.heartbeats");
				this.channel.queueBind(this.QUEUE_NAME_RECV, DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.node.serviceresults");
			}

			this.consumerRecv = new QueueingConsumer(this.channel);

			this.channel.basicConsume(this.QUEUE_NAME_RECV, false, "upsilon-node Message Consumer", this.consumerRecv);
		} catch (final Exception e) {
			DaemonConnectionHandler.LOG.error("Could not complete initial AMQP binding: " + e.getMessage(), e);
			return;
		}

		DaemonConnectionHandler.LOG.info("AMQP connection looks good, my recv queue is: " + this.QUEUE_NAME_RECV);

		this.run = true;
	}

	private String generateQueueName(final String suffix) {
		return "upsilon-node-" + Main.instance.node.getIdentifier() + "-" + suffix;
	}

	public Channel getChannel() {
		if ((this.connection == null) || !this.connection.isOpen()) {
			this.connect();
		}

		return this.channel;
	}

	@Override
	public void run() {
		while (this.run) {
			try {
				if ((this.connection == null) || !this.connection.isOpen()) {
					this.setStatus("Waiting for connection");
					Thread.sleep(1000);
					continue;
				}

				if ((this.channel == null) || !this.channel.isOpen()) {
					this.setStatus("Waiting for channel");
					Thread.sleep(1000);
					continue;
				}

				// The connection can be open and not accepting new
				// channels/consumers, like when it is shutting down. to sleep
				// for a moment to see if the connection is still good.
				Util.lazySleep(Duration.standardSeconds(1));

				final QueueingConsumer.Delivery delivery = this.consumerRecv.nextDelivery();

				final String body = new String(delivery.getBody());
				final Map<String, Object> headers = delivery.getProperties().getHeaders();

				if ((headers == null) || !headers.containsKey("upsilon-msg-type")) {
					DaemonConnectionHandler.LOG.warn("recv non-upsilon message: " + body);
				} else {
					final String typeString = ((LongString) headers.get("upsilon-msg-type")).toString();

					final UpsilonMessageType type = UpsilonMessageType.lookup(typeString);
					DaemonConnectionHandler.LOG.debug("recv type: " + typeString + " (enum=" + type + ") body: " + body.getBytes().length + " bytes long");

					final String replyTo = delivery.getProperties().getReplyTo();

					this.handler.handleMessageType(type, headers, body, delivery.getEnvelope().getDeliveryTag(), replyTo, this.channel);
				}
			} catch (final Exception e) {
				DaemonConnectionHandler.LOG.error("AMQP daemon: " + e.toString());
				e.printStackTrace();
			}
		}

		this.stop();
	}

	@Override
	public void stop() {
		this.run = false;
	}

}
