package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Jan 17, 2008
*/
class ManyToOneLazinessTests extends AbstractGrailsHibernateTests{   

    protected void onSetUp() {
        gcl.parseClass '''
class ManyToOneLazinessTestsBook {
    Long id
    Long version
    String title
    ManyToOneLazinessTestsAuthor author
    static belongsTo =  ManyToOneLazinessTestsAuthor

    static mapping = {
        author lazy:true
    }
}
class ManyToOneLazinessTestsAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:ManyToOneLazinessTestsBook]
}
'''
    }

    void testManyToOneLaziness() {
        def bookClass = ga.getDomainClass("ManyToOneLazinessTestsBook").clazz
        def authorClass= ga.getDomainClass("ManyToOneLazinessTestsAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
        assert author.save()

        author.addToBooks(title:"The Stand")
               .addToBooks(title:"The Shining")
               .save(flush:true)

        session.clear()

        def book = bookClass.get(1)

        author = book.author


        assert !Hibernate.isInitialized(author)

        assertEquals "Stephen King", author.name

        assert Hibernate.isInitialized(author)
    }

}