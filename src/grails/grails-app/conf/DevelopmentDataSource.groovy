class DevelopmentDataSource {
   boolean pooling = true
   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
   String url = "jdbc:hsqldb:mem:devDB"
   String driverClassName = "org.hsqldb.jdbcDriver"
   String username = "sa"
   String password = ""
}
