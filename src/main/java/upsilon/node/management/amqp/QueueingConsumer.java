package upsilon.node.management.amqp;

import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;

import java.io.IOException;

import java.util.Stack;

public class QueueingConsumer extends DefaultConsumer {
	static class Delivery {
		String consumerTag;
		Envelope env; 
		AMQP.BasicProperties props;
		byte[] body;

		public String getConsumerTag() {
			return this.consumerTag;
		}

		public Envelope getEnvelope() {
			return this.env;
		}

		public AMQP.BasicProperties getProperties() {
			return this.props;
		}

		public byte[] getBody() {
			return this.body;
		}
	}

	private Stack<Delivery> queue = new Stack<>();

	public QueueingConsumer(Channel chan) {
		super(chan);
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope env, AMQP.BasicProperties properties, byte[] body) throws IOException {
		Delivery d = new Delivery();
		d.consumerTag = consumerTag;
		d.env = env;
		d.props = properties;

		queue.add(d);
	}

	public Delivery nextDelivery() {
		return queue.pop();
	}

	public boolean isEmpty() {
		return this.queue.isEmpty();
	}
}
