class ProductionDataSource {
   boolean pooling = true
   String dbCreate = "update" // one of 'create', 'create-drop','update'
   String url = "jdbc:hsqldb:file:prodDb;shutdown=true"
   String driverClassName = "org.hsqldb.jdbcDriver"
   String username = "sa"
   String password = ""
}
