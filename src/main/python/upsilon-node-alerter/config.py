import os
import argparse

global args

argParser = argparse.ArgumentParser();
argParser.add_argument("--amqpHost", '-s')
args = argParser.parse_args()


import ConfigParser
global configParser
configParser = ConfigParser.ConfigParser()

configParser.add_section("database");
configParser.set("database", "user", "root")
configParser.set("database", "pass");

configParser.add_section("amqp")
configParser.set("amqp", "host", "localhost")
configParser.set("amqp", "exchange", "ex_upsilon")
configParser.set("amqp", "queue", "")

if (os.path.isfile("defaults.cfg")):
	configParser.readfp(open('defaults.cfg'))

class RuntimeConfig:
	dbUser = None
	dbPass = None

	amqpHost = None
	amqpExchange = None
	amqpQueue = None

	def __init__(self):
		self.applyConfig(configParser)
		self.applyArguments(args)

	def applyConfig(self, configParser):
		self.dbUser = configParser.get('database', 'user')
		self.dbPass = configParser.get('database', 'pass')

		self.amqpHost = configParser.get('amqp', 'host')
		self.amqpExchange = configParser.get('amqp', 'exchange');
		self.amqpQueue = configParser.get('amqp', 'queue');

	def applyArguments(self, args):
		if args.amqpHost != None:
			self.amqpHost = args.amqpHost


