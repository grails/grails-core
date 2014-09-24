package org.grails.web.metaclass

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.grails.web.servlet.GrailsFlashScope
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestFor(TestChainController)
@Mock(TestChainBook)
class ChainMethodTests {

    @Test
    void testChainMethodWithModel() {
        TestChainBook.metaClass.save = { false }

        controller.save()

        def flash = controller.flash

        assert flash.chainModel.book

        def id = System.identityHashCode(flash.chainModel.book)

        assert flash.chainModel[GrailsFlashScope.ERRORS_PREFIX+id]

        org.springframework.mock.web.MockHttpServletResponse response = controller.response

        assertEquals '/testChain/create', response.redirectedUrl
    }

    @Test
    void testChainMethodWithId() {
        controller.testId()

        assertEquals "Test param", controller.flash.chainModel.str
        assertEquals "/testChain/show/5", response.redirectedUrl
    }
}

@Artefact('Controller')
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

@Entity
class TestChainBook {
    String title
}

