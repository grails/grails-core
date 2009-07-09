/**
 * Tests a many-to-many and one-to-one relationship used together

 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 12, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

class ManyToManyAndOneToOneTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class Book {
    Long version
    Long id
    static belongsTo = Author
    Set authors
    
    static hasMany = [authors:Author]
    static mappedBy = [authors:"books"]
    String title
}
class Author {
    Long version
    Long id

    Set books
    static hasMany = [books:Book]
    static mappedBy = [books:"authors"]
    String name
    Book bookOther
}
        ''')
    }

    void testDomain() {
        GrailsDomainClass bookClass = ga.getDomainClass("Book")
        GrailsDomainClass authorClass = ga.getDomainClass("Author")
        assert authorClass
        assertTrue authorClass.getPropertyByName("bookOther").isOneToOne()
        assertTrue authorClass.getPropertyByName("books").isManyToMany()
        assertEquals "authors",authorClass.getPropertyByName("books").otherSide.name
        assertFalse authorClass.getPropertyByName("bookOther").isBidirectional()

        assert bookClass
        assert bookClass.getPropertyByName("authors").isManyToMany()
        assertEquals "books",bookClass.getPropertyByName("authors").otherSide.name



    }

}