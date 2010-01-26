package grails.test;

import org.springframework.web.servlet.ModelAndView;

/**
 * Test case for {@link ControllerUnitTestCase}.
 */
class ControllerUnitTestCaseTests extends GroovyTestCase {
    void testControllerClass() {
        UnitTestControllerTestCase testCase = new UnitTestControllerTestCase()
        testCase.setUp()
        testCase.testControllerClass()
        testCase.tearDown()
    }

    void testExplicitControllerClass() {
        new OtherTestCase().testControllerClass()
    }

    /**
     * Tests that the {@link ControllerUnitTestCase#mockCommandObject(Class)}
     * method works.
     */
    void testMockCommandObject() {
        def testCase = new UnitTestControllerTestCase()
        testCase.setUp()
        testCase.testCommandObject()
        testCase.tearDown()
    }
	
	void testGetSetModelAndView() {
		def testCase = new UnitTestControllerTestCase()
		testCase.setUp()
		testCase.testModelAndView()
		testCase.tearDown()
				
	}
}

class UnitTestControllerTestCase extends ControllerUnitTestCase {
	
	void testModelAndView() {
		controller.testSetModelAndView()
	}
	
    void testControllerClass() {
        assertEquals UnitTestController, controllerClass
        assertEquals "unitTest", controller.controllerName

        assertNull controller.actionName
        webRequest.actionName = "foo"
        assertEquals "foo", controller.actionName
    }

    void testCommandObject() {
        mockCommandObject(ControllerUnitTestCaseCommandObject)

        def cmd = new ControllerUnitTestCaseCommandObject()
        assertFalse cmd.validate()
        assertEquals 2, cmd.errors.errorCount
        assertEquals "nullable", cmd.errors["name"]
        assertEquals "range", cmd.errors["age"]

        cmd.name = "Pedro"
        cmd.age = 5
        assertFalse cmd.validate()
        assertEquals 1, cmd.errors.errorCount
        assertEquals "range", cmd.errors["age"]

        cmd.age = 24
        assertTrue cmd.validate()
        assertFalse cmd.errors.hasErrors()
    }
}

class OtherTestCase extends ControllerUnitTestCase {
    OtherTestCase() {
        super(UnitTestController)
    }

    void testControllerClass() {
        assertEquals UnitTestController, controllerClass
    }
}

class UnitTestController {
    String name

    def index = {
        
    }
	
	def testSetModelAndView() {
    	modelAndView = new ModelAndView()
		println modelAndView.viewName
	}
}

class ControllerUnitTestCaseCommandObject {
    String name
    int age

    static constraints = {
        name(nullable: false, blank: false)
        age(range: 10..50)
    }
}
