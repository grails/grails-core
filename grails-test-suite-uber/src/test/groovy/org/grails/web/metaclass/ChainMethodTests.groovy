package org.grails.web.metaclass

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import grails.artefact.Artefact
import grails.persistence.Entity

import org.grails.web.servlet.GrailsFlashScope

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ChainMethodTests extends Specification implements ControllerUnitTest<TestChainController>, DomainUnitTest<TestChainBook> {

    void testChainMethodWithModel() {
        TestChainBook.metaClass.save = { false }

        when:
        controller.save()
        def flash = controller.flash
        def id = System.identityHashCode(flash.chainModel.book)

        then:
        flash.chainModel.book
        flash.chainModel[GrailsFlashScope.ERRORS_PREFIX+id]
        response.redirectedUrl == '/testChain/create'
    }

    void testChainMethodWithId() {
        when:
        controller.testId()

        then:
        controller.flash.chainModel.str == "Test param"
        response.redirectedUrl == "/testChain/show/5"
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

