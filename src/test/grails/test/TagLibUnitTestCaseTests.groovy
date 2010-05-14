package grails.test

import junit.framework.TestFailure 
import junit.framework.TestResult 

class TagLibUnitTestCaseTests extends GrailsUnitTestCase {

    void testTagThatPopulatesPageScope() {
        def result = new TestResult()
        def testCase = new PagePropertyTagLibTestCase()
        testCase.name = 'testTagThatPopulatesPageScope'
        testCase.run(result)
        checkFailures(result)
    }
    
    void testTagThatAccessesPageScope() {
        def result = new TestResult()
        def testCase = new PagePropertyTagLibTestCase()
        testCase.name = 'testTagThatAccessesPageScope'
        testCase.run(result)
        checkFailures(result)
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

class PagePropertyTagLibTestCase extends TagLibUnitTestCase {
    
    void testTagThatPopulatesPageScope() {
        tagLib.tagThatPopulatesPageScope([food: 'nachos'])
        assertEquals 'nachos', tagLib.pageScope.favoriteFood
    }
    
    void testTagThatAccessesPageScope() {
        tagLib.pageScope.food = 'tacos'
        tagLib.tagThatAccessesPageScope([:])
        assertEquals 'food is tacos', tagLib.out.toString()
    }
}

class PagePropertyTagLib {
    
    def tagThatPopulatesPageScope = { attrs ->
        pageScope.favoriteFood = attrs.food
    }
    
    def tagThatAccessesPageScope = {
        out << "food is ${pageScope.food}"
    }
}