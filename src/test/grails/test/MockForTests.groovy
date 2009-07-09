package grails.test

import javax.security.auth.Subject

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 1, 2009
 */

public class MockForTests extends GroovyTestCase{

    void testUnregisteredMockedStaticMethods() {
        def testcase = new MockForTestsTestCase()
        testcase.setUp()
        testcase.testOne()
        testcase.tearDown()

        testcase.setUp()
        testcase.testTwo()
        testcase.tearDown()
        
    }

}
class MockForTestsTestCase extends ControllerUnitTestCase {
  def securityMock
  def controller

    public MockForTestsTestCase() {
        super(MockForController);    
    }



    protected void setUp() {
      super.setUp()
      controller = new MockForController()
 }

  void testOne() {
      securityMock = mockFor(DummySecurityUtils)
      securityMock.demand.static.getSubject{-> [isAuthenticated:{false}] }
      
      assertFalse "Should have been used mock static method",controller.myAction()
  }

  void testTwo() {
      assertTrue "Should have been used mock static method",controller.myAction()
  }
}
class DummySecurityUtils {
    static getSubject() { [isAuthenticated:{true}]  }
}
class MockForController {
    def myAction = {
      DummySecurityUtils.subject.isAuthenticated()
    }
}