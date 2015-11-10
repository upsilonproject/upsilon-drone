package upsilon.node.management.amqp;

import java.io.IOException;
import java.util.Date;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;

import upsilon.node.Daemon;
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
				final Builder propsBuilder = MessageHandler.getNewMsgPropsBuilder(UpsilonMessageType.HEARTBEAT);
				propsBuilder.expiration("60000");

				try {
					DaemonAmqpHeatbeater.LOG.debug("Sending AMQP Heartbeat");
					channel.basicPublish(DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.node.heartbeats", propsBuilder.build(), (new Date()).toString().getBytes());
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
