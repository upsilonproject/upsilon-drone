#!/usr/bin/python

import os, sys

sys.path.insert(1, os.path.abspath("../"))

from common.util import log
from common.sql import newSqlConnection
from common.amqp import newAmqpConnection
from config import RuntimeConfig

config = RuntimeConfig()

class MessageHandler():
	amqp = None
	mysql = None

	def setMySqlConnection(self, mysqlConnection):
		self.mysql = mysqlConnection
	
	def setAmqpConnection(self, amqpConnection, amqpExchange, channel, amqpQueue):
		self.amqp = amqpConnection
		self.amqpExchange = amqpExchange
		channel.basic_consume(self.onMessage, queue = amqpQueue)
		channel.start_consuming()

		amqpConnection.ioloop.start();

	def onMessage(self, channel, method, properties, body):
		if "upsilon-msg-type" in properties.headers:
			if properties.headers["upsilon-msg-type"] == "REQ_NODE_SUMMARY":
				self.onMessagePing(channel)
		else:
			print "Got message", properties.headers

		channel.basic_ack(delivery_tag = method.delivery_tag, multiple = False)

	def onMessagePing(self, channel):
		headers = dict();
		headers["upsilon-msg-type"] = "RES_NODE_SUMMARY"
		headers["node-identifier"] = "alerter"
		headers["node-version"] = "alerter"
		headers["node-type"] = "alerter"

		import pika
		props = pika.BasicProperties(headers = headers);
		pingBody = "<blat>"

		channel.basic_publish(exchange = self.amqpExchange, routing_key = "upsilon.res", body = pingBody, properties = props)
		log("replied to ping")

messageHandler = MessageHandler()

mysqlConnection = newSqlConnection(config.dbUser, config.dbPass)
messageHandler.setMySqlConnection(mysqlConnection)

amqpConnection, amqpChannel = newAmqpConnection(config.amqpHost, config.amqpExchange, config.amqpQueue);
messageHandler.setAmqpConnection(amqpConnection, config.amqpExchange, amqpChannel, config.amqpQueue)

log("upsilon-node-alerter")


