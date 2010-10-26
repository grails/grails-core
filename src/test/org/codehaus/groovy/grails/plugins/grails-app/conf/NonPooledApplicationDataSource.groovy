class NonPooledApplicationDataSource {
    boolean pooling = false
    String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
    String url = "jdbc:h2:mem:testDB"
    String driverClassName = "org.h2.Driver"
    String username = "sa"
    String password = ""
}
