package org.grails.web.metaclass

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.GrailsFlashScope
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ChainMethodSpec extends Specification implements ControllerUnitTest<TestChainController>, DataTest {

    void setupSpec() {
        mockDomain TestChainBook
    }

    void testChainMethodWithModel() {
        when:
        TestChainBook.metaClass.save = { false }

        controller.save()

        def flash = controller.flash

        then:
        flash.chainModel.book

        when:
        def id = System.identityHashCode(flash.chainModel.book)

        then:
        flash.chainModel[GrailsFlashScope.ERRORS_PREFIX + id]

        when:
        org.springframework.mock.web.MockHttpServletResponse response = controller.response

        then:
        '/testChain/create' == response.redirectedUrl
    }

    void testChainMethodWithId() {
        when:
        controller.testId()

        then:
        "Test param" == controller.flash.chainModel.str
        "/testChain/show/5" == response.redirectedUrl
    }
}

@Artefact('Controller')
class TestChainController {
    def save = {
        def book = new TestChainBook(params)
        if (!book.hasErrors() && book.save()) {
            flash.message = "Book ${book.id} created"
            redirect(action: "show", id: book.id)
        } else {
            chain(action: 'create', model: [book: book])
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

