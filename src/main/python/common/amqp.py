import pika

def on_timeout():
	global amqpConnection
	amqpConnection.close()

def newAmqpConnection(amqpHost, amqpExchange, amqpQueue):
	print "amqp host:", amqpHost
	amqpConnection = pika.BlockingConnection(pika.ConnectionParameters(host = amqpHost))
	print "conn:", amqpConnection.is_open
	 
	channel = amqpConnection.channel();
	channel.queue_declare(queue = amqpQueue, durable = False, auto_delete = True)
	channel.queue_bind(queue = amqpQueue, exchange = amqpExchange, routing_key = 'upsilon.node.serviceresults');
	channel.queue_bind(queue = amqpQueue, exchange = amqpExchange, routing_key = '#');
	channel.queue_bind(queue = amqpQueue, exchange = amqpExchange, routing_key = '*');

	return amqpConnection, channel
