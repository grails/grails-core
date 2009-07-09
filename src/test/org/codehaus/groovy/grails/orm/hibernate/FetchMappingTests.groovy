package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.*
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 27, 2008
 */
class FetchMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

class FetchMappingBook {
    Long id
    Long version
    String title
    
}

class FetchMappingAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:FetchMappingBook]
}

class FetchMappingPublisher {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:FetchMappingBook]

    static mapping = {
        books fetch:'join'
    }
}

''')
    }


    void testFetchMapping() {
        def authorClass = ga.getDomainClass("FetchMappingAuthor").clazz
        def publisherClass = ga.getDomainClass("FetchMappingPublisher").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                    .addToBooks(title:"The Shining")
                                    .addToBooks(title:"The Stand")
                                    .save(flush:true)

        def publisher = publisherClass.newInstance(name:"Apress")
                                    .addToBooks(title:"DGG")
                                    .addToBooks(title:"BGG")
                                    .save(flush:true)


        assert author
        assert publisher

        session.clear()

        author = authorClass.get(1)
        assertFalse "books association is lazy by default and shouldn't be initialized",Hibernate.isInitialized(author.books)

        publisher = publisherClass.get(1)

        assertTrue "books association mapped with join query and should be initialized",Hibernate.isInitialized(publisher.books)

    }


}