def getNode(helper):
    print "get node", helper.args.value

    msg = amqp.UpsilonMessage("GET_ITEM")
    msg.headers['itemType'] = 'node'
    msg.headers['itemQuery'] = helper.args.value
    msg.routingKey = "upsilon.custodian.requests"

    helper.chan.publishMessage(msg)
    msg = helper.chan.consumeUntilMessage("GET_ITEM_RESULT")

    print msg.body

def listNodes(helper):
    msg = amqp.UpsilonMessage("GET_LIST")
    msg.headers['itemType'] = 'node'
    msg.routingKey = "upsilon.custodian.requests"

    helper.chan.publishMessage(msg)
    msg = helper.chan.consumeUntilMessage("GET_LIST_RESULT")

    nodeList = json.loads(msg.body)

    tbl = PrettyTable(["ID", "Identifier", "Type", "Version"]);
    for node in nodeList:
        tbl.add_row([node['nodeId'], node['identifier'], node['serviceType'], node['instanceApplicationVersion']])

    print tbl

def getService(helper):
    print "get service", helper.args.value

    msg = amqp.UpsilonMessage("GET_ITEM")
    msg.headers['itemType'] = 'service'
    msg.headers['itemQuery'] = helper.args.value
    msg.routingKey = "upsilon.custodian.requests"

    helper.chan.publishMessage(msg)
    msg = helper.chan.consumeUntilMessage("GET_ITEM_RESULT")

    print msg.body

def listServices(helper):
    msg = amqp.UpsilonMessage("GET_LIST")
    msg.headers['itemType'] = 'service'
    msg.routingKey = 'upsilon.custodian.requests'

    helper.chan.publishMessage(msg);

    msg = helper.chan.consumeUntilMessage("GET_LIST_RESULT")

    tbl = PrettyTable(["ID", "Identifier", "Status"])
    for service in json.loads(msg.body):
        tbl.add_row([service['id'], service['identifier'], service['status']]);

    print tbl

global eap
eap.addType("node")
eap.addVerb("node", "list", listNodes)
eap.addVerbWithArgument("node", "get", getNode)

eap.addType("service")
eap.addVerb("service", "list", listServices)
eap.addVerbWithArgument("service", "get", getService)

