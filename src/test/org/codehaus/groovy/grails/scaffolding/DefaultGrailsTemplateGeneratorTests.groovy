package org.codehaus.groovy.grails.scaffolding

import org.codehaus.groovy.grails.validation.metaclass.ConstraintsEvaluatingDynamicProperty
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 *
 * Created: Dec 8, 2008
 */

public class DefaultGrailsTemplateGeneratorTests extends GroovyTestCase{

    protected void setUp() {
        def buildSettings = new BuildSettings(new File("."))
        BuildSettingsHolder.settings = buildSettings
    }

    protected void tearDown() {
        BuildSettingsHolder.settings = null
    }





    GroovyClassLoader gcl = new GroovyClassLoader()
    String testDomain = '''
class ScaffoldingTest {
   Long id
   Long version

   Integer status
   Date regularDate
   java.sql.Date sqlDate

   static constraints = {
      status inList:[1,2,3,4]
   }
}
'''


    void testGenerateDateSelect() {
        def templateGenerator = new DefaultGrailsTemplateGenerator()
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def cp = new ConstraintsEvaluatingDynamicProperty()
        def constrainedProperties = cp.get(testClass.newInstance())
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "create", sw


        println "sw: ${sw.toString()}"
        assertTrue "Should have rendered a datePicker for regularDate",sw.toString().contains('g:datePicker name="regularDate" value="${scaffoldingTestInstance?.regularDate}" precision="minute" ></g:datePicker>')
        assertTrue "Should have rendered a datePicker for sqlDate",sw.toString().contains('g:datePicker name="sqlDate" value="${scaffoldingTestInstance?.sqlDate}" precision="day" ></g:datePicker>')
    }

    void testGenerateNumberSelect() {
        def templateGenerator = new DefaultGrailsTemplateGenerator()
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def cp = new ConstraintsEvaluatingDynamicProperty()
        def constrainedProperties = cp.get(testClass.newInstance())
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "create", sw


        assertTrue "Should have rendered a select box for the number editor",sw.toString().contains('g:select id="status" name="status" from="${scaffoldingTest.constraints.status.inList}" value="${scaffoldingTest.status}" ></g:select>')
    }
}
