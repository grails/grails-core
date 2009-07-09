package org.codehaus.groovy.grails.web.metaclass

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope
import grails.util.MockHttpServletResponse

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 2, 2008
 */
class ChainMethodTests extends AbstractGrailsControllerTests{

    public void onSetUp() {
        gcl.parseClass('''
class TestChainController {
    def save = {
        def book = new TestChainBook(params)
        if(!book.hasErrors() && book.save()) {
            flash.message = "Book ${book.id} created"
            redirect(action:"show",id:book.id)
        }
        else {
            chain(action:'create',model:[book:book])
        }
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
        println flash
    }


}