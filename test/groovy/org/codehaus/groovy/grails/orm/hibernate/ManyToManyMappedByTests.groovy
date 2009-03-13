/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 4, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests

class ManyToManyMappedByTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class ManyToManyMappedByBook implements Serializable{
    Long id
    Long version
	String title

	ManyToManyMappedByAuthor creator

    Set d
	static hasMany = [d: ManyToManyMappedByAuthor]
	static belongsTo = [ManyToManyMappedByAuthor]
}

class ManyToManyMappedByAuthor implements Serializable {
    Long id
    Long version
	String email
    Set books
	static hasMany = [books: ManyToManyMappedByBook]
    static mappedBy = [books:'d']
}

'''
    }

    void testDomain() {
        def bookClass = ga.getDomainClass("ManyToManyMappedByBook")
        def authorClass = ga.getDomainClass("ManyToManyMappedByAuthor")

        assert bookClass.getPropertyByName("d").manyToMany
        assert bookClass.getPropertyByName("d").bidirectional
        assertEquals(authorClass.getPropertyByName("books"), bookClass.getPropertyByName("d").otherSide)
        assert authorClass.getPropertyByName("books").manyToMany
        assert authorClass.getPropertyByName("books").bidirectional
    }

    void testMapping() {
        def bookClass = ga.getDomainClass("ManyToManyMappedByBook").clazz
        def authorClass = ga.getDomainClass("ManyToManyMappedByAuthor").clazz


        assert authorClass.newInstance(email:"foo@bar.com").save(flush:true)

        def a = authorClass.get(1)

        def book = bookClass.newInstance(creator:a, title:"The Stand")
        a.addToBooks(book)

        a.save(flush:true)

        session.clear()

        a = authorClass.get(1)

        assertEquals 1, a.books.size()


        book = bookClass.get(1)

        assert book.creator
        assertEquals 1, book.d.size()                                        
    }

}