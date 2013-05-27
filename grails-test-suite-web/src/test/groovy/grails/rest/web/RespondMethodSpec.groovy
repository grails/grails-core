/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.rest.web

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.junit.Before
import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

import java.lang.reflect.Method

/**
 */
@TestFor(BookController)
@Mock(Book)
class RespondMethodSpec extends Specification{

    void setup() {
        def ga = grailsApplication
        ga.config.grails.mime.types =
            [ html: ['text/html','application/xhtml+xml'],
            xml: ['text/xml', 'application/xml'],
            text: 'text/plain',
            js: 'text/javascript',
            rss: 'application/rss+xml',
            atom: 'application/atom+xml',
            css: 'text/css',
            csv: 'text/csv',
            all: '*/*',
            json: ['application/json','text/json'],
            form: 'application/x-www-form-urlencoded',
            multipartForm: 'multipart/form-data'
        ]

        defineBeans {
            mimeTypes(MimeTypesFactoryBean) {
                grailsApplication = ga
            }
        }

    }

    void "Test that the respond method produces the correct model for a domain instance and no specific content type"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            webRequest.actionName = 'show'
            controller.show(book.id)
            def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
            modelAndView != null
            modelAndView instanceof ModelAndView
            modelAndView.model.book == book
            modelAndView.viewName == 'show'


    }

    void "Test that the respond method produces XML for a domain instance and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.show(book.id)


        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml'
            response.xml.title.text() == 'The Stand'

    }
}
@Artefact("Controller")
class BookController {
    def show(Long id) {
        respond Book.get(id)
    }
}
@Entity
class Book {
    String title
}


