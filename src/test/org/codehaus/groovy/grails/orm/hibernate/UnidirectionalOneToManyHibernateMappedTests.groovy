package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class UnidirectionalOneToManyHibernateMappedTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp()
        PluginManagerHolder.pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.pluginManager.registerMockPlugin([getName: { -> 'hibernate' }] as GrailsPlugin)
    }

    protected void tearDown() {
        super.tearDown()
        PluginManagerHolder.pluginManager = null
    }

    void testAnnotatedOneToManyDomain() {
        def config = new GrailsAnnotationConfiguration()
        def gcl = new GroovyClassLoader()
        // a grails entity
        gcl.parseClass '''
class UnidirectionalOneToManyHibernateMapped {
    Long id
    Long version
}
'''

        DefaultGrailsApplication application = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        application.initialise()
        config.grailsApplication = application

        config.addAnnotatedClass OneEntity
        config.addAnnotatedClass ManyEntity
        config.buildMappings()
    }
}
