package org.codehaus.groovy.grails.scaffolding

import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil

import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DefaultGrailsTemplateGeneratorTests extends GroovyTestCase {

    public static GrailsPlugin fakeHibernatePlugin = [getName: { -> 'hibernate' }] as GrailsPlugin
    private MockGrailsPluginManager pluginManager

    protected void setUp() {
        def buildSettings = new BuildSettings(new File("."))
        BuildSettingsHolder.settings = buildSettings
        pluginManager = new MockGrailsPluginManager()
        pluginManager.registerMockPlugin fakeHibernatePlugin
    }

    protected void tearDown() {
        BuildSettingsHolder.settings = null
    }

    GroovyClassLoader gcl = new GroovyClassLoader()
        String testDomain = '''
import grails.persistence.*

@Entity
class ScaffoldingTest {

   Integer status
   Date regularDate
   java.sql.Date sqlDate

   static constraints = {
      status inList:[1,2,3,4]
   }
}
'''

    void testGenerateDateSelect() {
        def templateGenerator = new DefaultGrailsTemplateGenerator(basedir:"../grails-resources")
        templateGenerator.pluginManager = pluginManager
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assert sw.toString().contains('g:datePicker name="regularDate" precision="day"  value="${scaffoldingTestInstance?.regularDate}"') == true
        assert sw.toString().contains('datePicker name="sqlDate" precision="day"  value="${scaffoldingTestInstance?.sqlDate}') == true
    }

    void testGenerateNumberSelect() {
        def templateGenerator = new DefaultGrailsTemplateGenerator(basedir:"../grails-resources")
        templateGenerator.pluginManager = pluginManager
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assertThat "Should have rendered a select box for the number editor",
            sw.toString(),
            containsString('g:select name="status" from="${scaffoldingTestInstance.constraints.status.inList}" required="" value="${fieldValue(bean: scaffoldingTestInstance, field: \'status\')}" valueMessagePrefix="scaffoldingTest.status"')
    }

    void testDoesNotGenerateInputForId() {
        def templateGenerator = new DefaultGrailsTemplateGenerator(basedir:"../grails-resources")
        templateGenerator.pluginManager = pluginManager
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")
        def domainClass = new DefaultGrailsDomainClass(testClass)
        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assert !sw.toString().contains('name="id"'), "Should not have rendered an input for the id"
    }

    void testGeneratesInputForAssignedId() {
        def templateGenerator = new DefaultGrailsTemplateGenerator(basedir:"../grails-resources")
        templateGenerator.pluginManager = pluginManager
        gcl.parseClass('''
import grails.persistence.*

@Entity
class ScaffoldingTest {
    String id
    static mapping = {
        id generator: "assigned"
    }
}
        ''')
        def testClass = gcl.loadClass("ScaffoldingTest")
        def domainClass = new DefaultGrailsDomainClass(testClass)
        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }
        GrailsDomainBinder.evaluateMapping(domainClass)

        assert GrailsDomainBinder.getMapping(domainClass)?.identity?.generator == 'assigned'

        def sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assertThat "Should have rendered an input for the id",
                sw.toString(),
                containsString('g:textField name="id" value="${scaffoldingTestInstance?.id}"')
    }
}
