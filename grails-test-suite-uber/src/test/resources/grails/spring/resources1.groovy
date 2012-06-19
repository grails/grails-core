import org.apache.commons.dbcp.BasicDataSource
beans {
	dataSource(BasicDataSource) {
		driverClassName = "org.h2.Driver"
		url = "jdbc:h2:mem:grailsDB"
		username = "sa"
		password = ""
	}
}