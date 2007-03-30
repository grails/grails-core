import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration

class TestDataSource {
   def configClass = GrailsAnnotationConfiguration.class   
   boolean pooling = true
   String dbCreate = "update" // one of 'create', 'create-drop','update'
   String url = "jdbc:hsqldb:mem:testDB"
   String driverClassName = "org.hsqldb.jdbcDriver"
   String username = "sa"
   String password = ""
}
