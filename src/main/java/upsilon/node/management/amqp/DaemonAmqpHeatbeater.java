package upsilon.node.management.amqp;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;

import upsilon.node.Configuration;
import upsilon.node.Daemon;
import upsilon.node.Main;
import upsilon.node.util.Util;

public class DaemonAmqpHeatbeater extends Daemon {
	private static final transient Logger LOG = LoggerFactory.getLogger(DaemonAmqpHeatbeater.class);
	private boolean run = true;

	@Override
	public void run() {
		this.run = true;

		while (this.run) {
			final Channel channel = DaemonConnectionHandler.instance.getChannel();

			if ((channel != null) && channel.isOpen()) {
				final Map<String, Object> headers = MessageHandler.getNewMessageHeaders(UpsilonMessageType.HEARTBEAT);
				headers.put("node-identifier", Main.instance.node.getIdentifier());
				headers.put("node-version", Main.getVersion());
				headers.put("node-service-count", Configuration.instance.services.size());	
				
				Builder builder = MessageHandler.getBuilderFromHeaders(headers);
				
				builder.expiration("60000"); 

				try {
					DaemonAmqpHeatbeater.LOG.debug("Sending AMQP Heartbeat");
					channel.basicPublish(DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.node.heartbeats", builder.build(), (new Date()).toString().getBytes());
				} catch (final IOException e) {
					e.printStackTrace();
				} 
			}

			Util.lazySleep(Duration.standardSeconds(60));
		}
	}

	@Override
	public void stop() {
		this.run = false;
	}

}
