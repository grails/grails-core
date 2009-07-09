package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 23, 2009
 */

public class DefaultSortOrderForCollectionTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class DefaultSortOrderForCollectionBook {
    String bookTitle
    static belongsTo = [author:DefaultSortOrderForCollectionAuthor]
}

@Entity
class DefaultSortOrderForCollectionAuthor {
    static hasMany = [books:DefaultSortOrderForCollectionBook]

    static mapping = { books sort:'bookTitle' }
}
''')
    }



    void testDefaultSortOrderWithCollection() {
         def Book = ga.getDomainClass("DefaultSortOrderForCollectionBook").clazz
         def Author = ga.getDomainClass("DefaultSortOrderForCollectionAuthor").clazz


         def a = Author.newInstance()

         .addToBooks(bookTitle:"It")
         .addToBooks(bookTitle:"Stand by me")
         .addToBooks(bookTitle:"Along came a spider")
         .save(flush:true)


        session.clear()


        a = Author.get(1)

        def books = a.books.toList()

        assertEquals "Along came a spider", books[0].bookTitle
        assertEquals "It", books[1].bookTitle
        assertEquals "Stand by me", books[2].bookTitle
    }

}