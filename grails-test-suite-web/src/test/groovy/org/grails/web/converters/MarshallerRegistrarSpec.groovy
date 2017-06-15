package org.grails.web.converters

import grails.artefact.Artefact
import grails.converters.JSON
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import javax.annotation.PostConstruct
import spock.lang.Specification

class MarshallerRegistrarSpec extends Specification implements ControllerUnitTest<JsonMarshallerController>, DomainUnitTest<Post> {

    Closure doWithSpring() {{ ->
        marshallerRegistrar(MarshallerRegistrar)
    }}
    
    def "should use custom marshaller"() {
        when:
        controller.show()
        then:
        response.contentAsString == '{"content":"Content","custom":true}'
    }
    
}

@Artefact("Controller")
class JsonMarshallerController {
    def show() {
        def post = new Post()
        post.content = 'Content'
        post.dateCreated = new Date()
        render post as JSON
    }
}

class MarshallerRegistrar {
    @PostConstruct
    void registerMarshallers() {
        println "Registering custom marshallers"
        JSON.registerObjectMarshaller(Post) { Post p ->
            return [ content: p.content, custom: true ]
        }
    }
}

@Entity
class Post {
    String content
    Date dateCreated
}