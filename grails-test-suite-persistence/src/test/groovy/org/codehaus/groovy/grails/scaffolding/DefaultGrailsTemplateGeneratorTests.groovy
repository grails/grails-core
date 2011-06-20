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

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DefaultGrailsTemplateGeneratorTests extends GroovyTestCase {

    public static GrailsPlugin fakeHibernatePlugin = [getName: { -> 'hibernate' }] as GrailsPlugin

    protected void setUp() {
        def buildSettings = new BuildSettings(new File("."))
        BuildSettingsHolder.settings = buildSettings
        PluginManagerHolder.pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.pluginManager.registerMockPlugin fakeHibernatePlugin
    }

    protected void tearDown() {
        BuildSettingsHolder.settings = null
        PluginManagerHolder.pluginManager = null
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
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assertThat "Should have rendered a datePicker for regularDate",
            sw.toString(),
            containsString('g:datePicker name="regularDate" precision="day" value="${scaffoldingTestInstance?.regularDate}"')
        assertThat "Should have rendered a datePicker for sqlDate",
            sw.toString(),
            containsString('datePicker name="sqlDate" precision="day" value="${scaffoldingTestInstance?.sqlDate}"')
    }

    void testGenerateNumberSelect() {
        def templateGenerator = new DefaultGrailsTemplateGenerator(basedir:"../grails-resources")
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
}
