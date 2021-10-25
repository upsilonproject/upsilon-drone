global Diagnoser, DiagnosticResult

class DiagnosticResult:
  def __init__(self, result = True, message = "( no details provided )"):
    self.result = result
    self.message = message

class Diagnoser:
  def checkConnectAmqp(self):
    return DiagnosticResult(True)

  def checkEssentialServices(self):
    msg = amqp.UpsilonMessage("REQ_NODE_SUMMARY")

  def getAll(self):
    return filter(lambda x: "check" in x, dir(self))

def getDiagnostics(helper):
  d = Diagnoser()

  for f in d.getAll():
    dr = getattr(d, f)();

    if dr.result:
      prefix = "[  OK  ]"
    else:
      prefix = "[ FAIL ]"

    print prefix, f, ":", dr.message

def listDiagnostics(helper):
  d = Diagnoser()

  for func in d.getAll():
    print func

global eap
eap.addType("diagnostics")
eap.addVerb("diagnostics", "run", getDiagnostics)
eap.addVerb("diagnostics", "list", listDiagnostics)
