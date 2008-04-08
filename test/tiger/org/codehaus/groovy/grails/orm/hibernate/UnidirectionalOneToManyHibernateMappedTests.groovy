package org.codehaus.groovy.grails.orm.hibernate


import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Apr 8, 2008
 */
class UnidirectionalOneToManyHibernateMappedTests extends GroovyTestCase {

    void testAnnotatedOneToManyDomain() {
        def config = new GrailsAnnotationConfiguration()
        def gcl = new GroovyClassLoader()
        // a grails entity
        gcl.parseClass('''
class UnidirectionalOneToManyHibernateMapped {
    Long id
    Long version
}
''')
        DefaultGrailsApplication application = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        application.initialise()
        config.grailsApplication = application

        config.addAnnotatedClass OneEntity
        config.addAnnotatedClass ManyEntity
        config.buildMappings()


    }

}