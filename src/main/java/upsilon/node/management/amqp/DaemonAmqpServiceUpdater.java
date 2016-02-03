package upsilon.node.management.amqp;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;

import upsilon.node.Configuration;
import upsilon.node.Daemon;
import upsilon.node.Main;
import upsilon.node.dataStructures.StructureService;
import upsilon.node.util.Util;

public class DaemonAmqpServiceUpdater extends Daemon {
	private boolean run;
	private static final transient Logger LOG = LoggerFactory.getLogger(DaemonAmqpServiceUpdater.class);

	@Override
	public void run() {
		this.run = true;

		while (this.run) {
			final Channel channel = DaemonConnectionHandler.instance.getChannel();

			if ((channel != null) && channel.isOpen()) {
				for (StructureService service : Configuration.instance.services) {
					this.pushService(channel, service); 
				}
				
			}

			Util.lazySleep(Duration.standardSeconds(60));
		}
	}
	
	private void pushService (Channel channel, StructureService service) {
		final Map<String, Object> headers = MessageHandler.getNewMessageHeaders(UpsilonMessageType.SERVICE_CHECK_RESULT);
		headers.put("identifier", service.getIdentifier());  
		headers.put("karma", service.getKarmaString()); 
		headers.put("node-identifier", Main.instance.node.getIdentifier()); 
		
		Builder builder = MessageHandler.getBuilderFromHeaders(headers);
		
		builder.expiration("60000"); 

		try { 
			DaemonAmqpServiceUpdater.LOG.debug("Pushing service: " + service.getIdentifier());
			channel.basicPublish(DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.node.serviceresults", builder.build(), service.getOutput().toString().getBytes());
		} catch (final IOException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public void stop() {
		this.run = false;
	}
 
}
