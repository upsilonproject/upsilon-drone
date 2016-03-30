import _mysql

def newSqlConnection(dbUser, dbPass):
	mysqlConnection = _mysql.connect(user=dbUser)

	return mysqlConnection
