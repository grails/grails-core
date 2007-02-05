import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration

class DevelopmentDataSource {
   def configClass = GrailsAnnotationConfiguration.class
   boolean pooled = true
   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
   String url = "jdbc:hsqldb:mem:testDB"
   String driverClassName = "org.hsqldb.jdbcDriver"
   String username = "sa"
   String password = ""
}
