package org.codehaus.groovy.grails.web.metaclass

import grails.util.MockRequestDataValueProcessor

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.web.servlet.support.RequestDataValueProcessor

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ChainMethodTests extends AbstractGrailsControllerTests {

    void registerRequestDataValueProcessor() {
        RequestDataValueProcessor requestDataValueProcessor = new MockRequestDataValueProcessor()
        MockApplicationContext applicationContext = (MockApplicationContext)ctx
        applicationContext.registerMockBean("requestDataValueProcessor",requestDataValueProcessor)
    }
    void unRegisterRequestDataValueProcessor() {
        MockApplicationContext applicationContext = (MockApplicationContext)ctx
        applicationContext.registerMockBean("requestDataValueProcessor",null)
    }

    protected void onSetUp() {
        gcl.parseClass('''
class TestChainController {
    def save = {
        def book = new TestChainBook(params)
        if (!book.hasErrors() && book.save()) {
            flash.message = "Book ${book.id} created"
            redirect(action:"show",id:book.id)
        }
        else {
            chain(action:'create',model:[book:book])
        }
    }

    def testId = {
        chain action: 'show', id: 5, model: [str: "Test param"]
    }
}
class TestChainBook {
    Long id
    Long version
    String title
}
''')
    }

    void testChainMethodWithModel() {
        def domainClass = ga.getDomainClass("TestChainBook").clazz
        domainClass.metaClass.save = { false }
        def controller = ga.getControllerClass("TestChainController").newInstance()

        controller.save()

        def flash = controller.flash

        assert flash.chainModel.book

        def id = System.identityHashCode(flash.chainModel.book)

        assert flash.chainModel[GrailsFlashScope.ERRORS_PREFIX+id]

        org.springframework.mock.web.MockHttpServletResponse response = controller.response

        assertEquals '/testChain/create', response.redirectedUrl
    }

    void testChainMethodWithModelAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        def domainClass = ga.getDomainClass("TestChainBook").clazz
        domainClass.metaClass.save = { false }
        def controller = ga.getControllerClass("TestChainController").newInstance()

        controller.save()

        def flash = controller.flash

        assert flash.chainModel.book

        def id = System.identityHashCode(flash.chainModel.book)

        assert flash.chainModel[GrailsFlashScope.ERRORS_PREFIX+id]

        org.springframework.mock.web.MockHttpServletResponse response = controller.response

        assertEquals '/testChain/create?requestDataValueProcessorParamName=paramValue', response.redirectedUrl
        this.unRegisterRequestDataValueProcessor()
    }

    void testChainMethodWithId() {
        def controller = ga.getControllerClass("TestChainController").newInstance()
        controller.testId()

        assertEquals "Test param", controller.flash.chainModel.str
        assertEquals "/testChain/show/5", response.redirectedUrl
    }
}
