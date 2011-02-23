package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.mock.web.MockHttpServletRequest

class DataBindingWithUnidirectionalAssociationTests extends AbstractGrailsHibernateTests{

    @Override
    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class DataBindingWithUnidirectionalAssociationAuthor {

    String name

    static constraints = {
    }
}

@Entity
class DataBindingWithUnidirectionalAssociationBook {

    String title

    static hasMany = [authors:DataBindingWithUnidirectionalAssociationAuthor]

    static constraints = {
    }
}
        '''
    }

    void testBindToNewInstance() {
        def Author = ga.getDomainClass("DataBindingWithUnidirectionalAssociationAuthor").clazz
        def Book = ga.getDomainClass("DataBindingWithUnidirectionalAssociationBook").clazz

        def id1 = Author.newInstance(name:"Stephen King").save(flush:true).id
        def id2 = Author.newInstance(name:"James Patterson").save(flush:true).id

        def book = Book.newInstance(title:"The Stand")

        def request = new MockHttpServletRequest()
        request.addParameter("authors[0].id","$id1")
        request.addParameter("authors[1].id","$id2")

        def params = new GrailsParameterMap(request)

        book.properties = params
        book.save(flush:true)

        session.clear()

        book = Book.findByTitle("The Stand")

        assert 2 == book.authors.size()
        assert book.authors.find { it.name == "Stephen King" } != null
    }
}
