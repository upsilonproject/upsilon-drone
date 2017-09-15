import json

def getAmqpStats(helper):
    from urllib3 import PoolManager, util
    try: 
        username = "guest"
        password = "guest"

        headers = util.make_headers(basic_auth = username + ":" + password)

        http = PoolManager()
        r = http.request("GET", "upsilon:15672/api/channels", headers = headers)

        channels = json.loads(r.data)


        tbl = PrettyTable(["Connection", "username", "Unacked", "Publish"])

        if helper.args.debug:
            print json.dumps(channels, indent = 4);

        for conn in channels:
            tbl.add_row([conn['name'], conn['user'], conn['messages_unacknowledged'], 0])

        print tbl

    except Exception as e:
        print str(e)

global eap
eap.addType("amqp")
eap.addVerb("amqp", "stats", getAmqpStats)
