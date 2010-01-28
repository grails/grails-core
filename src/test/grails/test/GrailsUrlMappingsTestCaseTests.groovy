package grails.test

import grails.util.GrailsWebUtil
import junit.framework.ComparisonFailure
import junit.framework.TestFailure
import junit.framework.TestResult
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.web.context.WebApplicationContext
import org.codehaus.groovy.grails.web.mapping.UrlMapping
import org.codehaus.groovy.grails.web.mapping.RegexUrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappingData
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.web.mapping.AbstractUrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
import org.springframework.web.multipart.commons.CommonsMultipartResolver

/**
 * @author Luke Daley
 */
class GrailsUrlMappingsTestCaseTests extends GrailsUnitTestCase {
    def mockApplicationContext
    def mockApplication

    protected void setUp() {
        super.setUp()
        
        mockApplication = new DefaultGrailsApplication(
                [ GrailsUrlMappingsTestCaseFakeController, MoneyController, GRAILS_3571_UrlMappings, TestInternalUrlMappings, OverrideUrlMappings,
                  GrailsUrlMappingTestCaseTestsBaseController, GrailsUrlMappingTestCaseTestsSubclassController
                ] as Class[],
                new GroovyClassLoader(this.getClass().classLoader))
        mockApplication.initialise()
        mockApplication.config.disableMultipart = true

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, mockApplication)
        ctx.registerMockBean("multipartResolver", new CommonsMultipartResolver()) 
        mockApplication.applicationContext = ctx

        def servletContext = ctx.servletContext
        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx )
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)

        mockApplicationContext = ctx
        ctx = new GrailsWebApplicationContext(ctx)
        ctx.servletContext = servletContext
        
        GrailsWebUtil.bindMockWebRequest(ctx)
    }

    void testSetup() {
        TestResult result = new TestResult()
        def testCase = new TestUrlMappingsTestCase()
        testCase.grailsApplication = mockApplication
        testCase.name = "testBasicMapping"
        testCase.run(result)

        checkFailures(result)
    }

    void testSetupExplicitMappingClass() {
        TestResult result = new TestResult()
        def testCase = new TestUrlMappingsTestCase()
        testCase.grailsApplication = mockApplication
        testCase.name = "testBasicMapping"
        testCase.run(result)

        checkFailures(result)
    }

    void testGetUrlMappingEvaluatees() {
        def test = new ExplicitMappingTestCase()
        def e = test.urlMappingEvaluatees
        assertTrue "Mappings to evaluate is not a collection!", e instanceof Collection
        assertSame ExplicitMappingTestCase.mappings, e[0]
        assertEquals 1, e.size()

        ExplicitMappingTestCase.mappings = Class
        assertEquals([Class], test.urlMappingEvaluatees)

        ExplicitMappingTestCase.mappings = [Class]
        assertEquals([Class], test.urlMappingEvaluatees)

        ExplicitMappingTestCase.mappings = [Class, String, Integer]
        assertEquals([Class, String, Integer], test.urlMappingEvaluatees)

        test.grailsApplication = [
                urlMappingsClasses: [ new Expando(clazz: String), new Expando(clazz: Integer) ]
        ]

        test.mappings = null
        assertEquals([String, Integer], test.urlMappingEvaluatees)
    }

    void testCreateMappingsHolder() {
        def test = new ExplicitMappingTestCase()
        ExplicitMappingTestCase.mappings = {
            "/a/b"(controller: "a")
            "/c/d"(controller: "c")
        }

        test.mappingEvaluator = [
                evaluateMappings: {
                    assertTrue "evaluateMappings() argument should be a closure.", it instanceof Closure
                    assertSame ExplicitMappingTestCase.mappings, it
                    return [ new MockUrlMapping("a", "show"), new MockUrlMapping("c", "list") ]
                }
        ]

        def holder = test.createMappingsHolder()
        assertEquals 2, holder.urlMappings.size()
        assertNotNull holder.urlMappings.find { it == new MockUrlMapping("a", "show") }
        assertNotNull holder.urlMappings.find { it == new MockUrlMapping("c", "list") }
    }

    void testCreateControllerMap() {
        def test = new GrailsUrlMappingsTestCase()

        test.grailsApplication = [
                controllerClasses: [
                        [
                                getLogicalPropertyName: {-> "dilbert"},
                                newInstance: {-> "DilbertController"}
                        ] as GrailsControllerClass,
                        [
                                getLogicalPropertyName: {-> "catbert"},
                                newInstance: {-> "CatbertController"}
                        ] as GrailsControllerClass
                ]
        ]

        def controllers = test.createControllerMap()
        assertEquals 2, controllers.size()
        assertEquals "DilbertController", controllers["dilbert"]
        assertEquals "CatbertController", controllers["catbert"]
    }

    void testGetActions() {
        def test = new GrailsUrlMappingsTestCase()
        test.controllers = [grailsUrlMappingsTestCaseFake: new GrailsUrlMappingsTestCaseFakeController()]
        def actions = test.getActions("grailsUrlMappingsTestCaseFake")
        assertEquals 3, actions.size()
        (1..3).each {
            assertTrue("should contain action 'action$it'", actions.contains("action$it" as String))
        }
    }

    void testAssertView() {
        def test = new GrailsUrlMappingsTestCase()
        def expectedPattern = ""
        test.patternResolver = [
                getResources: {
                    assertEquals(expectedPattern, it)
                    return [1]
                }
        ]

        expectedPattern = "grails-app/views/c/v.*"
        test.assertView("c", "v", "u")

        expectedPattern = "grails-app/views/v.*"
        test.assertView(null, "v", "u")
    }

    void test_GRAILS_3571_Bug() {
        def test = new MoneyMappingTestCase()
        test.grailsApplication = mockApplication
        test.setUp()

        test.testMoneyMapping()

    }

    void testAssertUrlMapping() {
        def test = new MultipleMappingsTestCase()
        test.grailsApplication = mockApplication
        test.setUp()

        shouldFail(IllegalArgumentException) {
            test.assertUrlMapping("/nonexistent", controller: "grailsUrlMappingsTestCaseFake")
        }

        shouldFail(IllegalArgumentException) {
            test.assertUrlMapping("/action1", controller: "blah")
        }

        shouldFail(IllegalArgumentException) {
            test.assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "xxx")
        }

        shouldFail(IllegalArgumentException) {
            test.assertUrlMapping("/action1", action: "action1")
        }

        shouldFail(ComparisonFailure) {
            try {
                test.assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action2")
            } catch (e) {
                e.printStackTrace()

            }
        }

        test.assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        test.assertUrlMapping("/action2", controller: "grailsUrlMappingsTestCaseFake", action: "action2")

        test.assertForwardUrlMapping("/default", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        shouldFail(ComparisonFailure) {
            test.assertReverseUrlMapping("/default", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        }

        shouldFail(IllegalArgumentException) {
            test.assertUrlMapping(300, controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        }

        test.assertUrlMapping(500, controller: "grailsUrlMappingsTestCaseFake", action: "action1")

        test.assertForwardUrlMapping("/controllerView", controller: "grailsUrlMappingsTestCaseFake", view: "view")
        shouldFail(ComparisonFailure) {
            test.assertForwardUrlMapping("/controllerView", controller: "grailsUrlMappingsTestCaseFake", view: "viewXXX")
        }

        shouldFail(ComparisonFailure) {
            test.assertUrlMapping("/absoluteView", controller: "grailsUrlMappingsTestCaseFake", view: "view")
        }

        test.assertUrlMapping("/absoluteView", view: "view")
        test.assertUrlMapping("/absoluteView", view: "/view")
        test.assertUrlMapping("/absoluteViewWithSlash", view: "view")
        test.assertUrlMapping("/absoluteViewWithSlash", view: "/view")

        test.assertUrlMapping("/params/value1/value2", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
            param1 = "value1"
            param2 = "value2"
        }

        shouldFail(ComparisonFailure) {
            test.assertUrlMapping("/params/value3/value4", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
                param1 = "value1"
                param2 = "value2"
            }
        }

        shouldFail(ComparisonFailure) {
            test.assertUrlMapping("/params/value1/value2", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
                param1 = "value1"
                param2 = "value2"
                xxx = "value3"
            }
        }

        test.assertUrlMapping("/params/value1", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
            param1 = "value1"
        }
    }

    void testGrails5786() {
        def test = new Grails5786TestCase()
        test.grailsApplication = mockApplication
        test.setUp()
        test.testSuperClassMapping()
    }
    
    private void checkFailures(TestResult result) {
        result.errors().each { TestFailure failure ->
            println ">> Error: ${failure.toString()}"
            failure.thrownException()?.printStackTrace()
        }
        assertEquals 0, result.errorCount()

        result.failures().each { TestFailure failure ->
            println ">> Failure: ${failure.toString()}"
            failure.thrownException()?.printStackTrace()
        }
        assertEquals 0, result.failureCount()
    }
}

class Grails5786TestCase extends GrailsUrlMappingsTestCase {
    void testSuperClassMapping() {
        assertUrlMapping('/grailsUrlMappingTestCaseTestsSubclass/base', controller: 'grailsUrlMappingTestCaseTestsSubclass', action:'base')
    }
}

class GRAILS_3571_UrlMappings {
  static mappings = {
    "/$controller/$action?/$id?"{
      constraints {
        // apply constraints here
      }
    }

    "/showMoney/$currencyName" (controller: 'money', action: 'display')

    "500"(view:'/error')
  }
}
class MoneyMappingTestCase extends GrailsUrlMappingsTestCase {

    void testMoneyMapping() {
        assertUrlMapping('/showMoney/dollars',
                         controller: 'money', action:'display') {
            currencyName = 'dollars'
        }
    }
}
class MoneyController {

    def display = {
        render "display action called for currencyName ${params.currencyName}"
    }
}

class TestUrlMappingsTestCase extends GrailsUrlMappingsTestCase {
    void testBasicMapping() {
        assertUrlMapping("/showPerson/Jeff_Beck", controller: "grailsUrlMappingsTestCaseFake", action: "action2")
        assertUrlMapping("/showPerson/Ozzy_Osbourne", controller: "grailsUrlMappingsTestCaseFake", action: "action1") {
            personName = "Ozzy_Osbourne"
        }
    }
}

class ExplicitMappingTestCase extends GrailsUrlMappingsTestCase {
    static mappings = {
        "/$controller/$action?" {}
    }
}

class MultipleMappingsTestCase extends GrailsUrlMappingsTestCase {
    static mappings = {
        "/action1"(controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        "/action2"(controller: "grailsUrlMappingsTestCaseFake", action: "action2")
        "/default"(controller: "grailsUrlMappingsTestCaseFake")
        "500"(controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        "/controllerView"(controller: "grailsUrlMappingsTestCaseFake", view: "view")
        "/absoluteView"(view: "view")
        "/absoluteViewWithSlash"(view: "/view")
        "/params/$param1/$param2?"(controller: "grailsUrlMappingsTestCaseFake", action: "action3")
    }

    def assertView(controller, view, url) {
        return true
    }
}

class TestInternalUrlMappings {
    static mappings = {
        "/showPerson/$personName"(controller:'grailsUrlMappingsTestCaseFake', action:'action1')
    }
}

class OverrideUrlMappings {
    static mappings = {
        "/showPerson/Jeff_Beck"(controller:'grailsUrlMappingsTestCaseFake', action:'action2') {}
    }
}

class GrailsUrlMappingsTestCaseFakeController {

    def defaultAction = "action1"

    def action1 = {}
    def action2 = {}
    def action3Flow = {}
    def notAction1 = 1
    def notAction2 = 2

}

class MockUrlMapping implements UrlMapping {
    String controller
    String action
    boolean restfulMapping

    MockUrlMapping(String controller, String action) {
        this.controller = controller
        this.action = action
    }

    boolean equals(other) {
        return other instanceof MockUrlMapping && other.controller == this.controller && other.action == this.action
    }

    public UrlMappingInfo match(String uri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UrlMappingData getUrlData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String createURL(Map parameterValues, String encoding) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String createURL(Map parameterValues, String encoding, String fragment) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String createRelativeURL(String controller, String action, Map parameterValues, String encoding) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String createRelativeURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ConstrainedProperty[] getConstraints() {
        return new ConstrainedProperty[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getControllerName() {
        return this.controller
    }

    public Object getActionName() {
        return this.action
    }

    public Object getViewName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setParameterValues(Map parameterValues) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setParseRequest(boolean shouldParse) {
        // do nothing
    }

    public String getMappingName() {
        return null;
    }

    public void setMappingName(String name) {
    }

	public boolean hasRuntimeVariable(String name) {
		return false;
	}
    
}

abstract class GrailsUrlMappingTestCaseTestsBaseController {
	def base = {}
}

class GrailsUrlMappingTestCaseTestsSubclassController extends GrailsUrlMappingTestCaseTestsBaseController {}