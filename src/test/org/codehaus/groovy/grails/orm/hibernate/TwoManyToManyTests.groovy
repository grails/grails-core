package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 29, 2008
 */
class TwoManyToManyTests extends AbstractGrailsHibernateTests{

    void testManyToManyMapping() {
        def Person = ga.getDomainClass("TwoMMPerson").clazz
        def Book = ga.getDomainClass("TwoMMBook").clazz

        def a = Person.newInstance(name:"Stephen King")

        a.addToBooks(Book.newInstance(title:"The Shining"))
         .addToBooks(Book.newInstance(title:"The Stand"))
         .addToPublishedBooks(Book.newInstance(title:"Rose Madder"))
         .addToPublishedBooks(Book.newInstance(title:"Misery"))
         .addToPublishedBooks(Book.newInstance(title:"It"))
         .save(true)
        
        assertEquals 5, Book.list().size()

        def b = Book.get(1)
        assert b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()

        a = Person.get(1)
        assert a
        assertNotNull a.books
        assertEquals 2, a.books.size()

        assertNotNull a.publishedBooks
        assertEquals 3, a.publishedBooks.size()



        assertEquals b, a.books.find { it.id == 1}
        this.session.flush()
        session.clear()

        a = Person.get(1)
        assert a
        assert a.books

        b = Book.findByTitle("It")
        assert b
        assert b.publishers
        assertEquals 1, b.publishers.size()

        def publisher = Person.newInstance(name:"Apress")
        publisher.addToPublishedBooks(b)
        assert publisher.save(flush:true)

        session.clear()

        b = Book.findByTitle("It")
        publisher = Person.findByName("Apress")
        assertEquals 2, b.publishers.size()
        assertEquals 1, publisher.publishedBooks.size()
        assertTrue b.publishers.any { it.name == 'Apress' }
        assertTrue b.publishers.any { it.name == 'Stephen King' }

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from twommperson_books")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 1,rs.getInt("twommperson_id")
        assertEquals 1,rs.getInt("twommbook_id")

        assert rs.next()
        assertEquals 1,rs.getInt("twommperson_id")
        assertEquals 2,rs.getInt("twommbook_id")


        ps = c.prepareStatement("select * from twommperson_published_books")
        rs = ps.executeQuery()
        assert rs.next()

        assertEquals 1,rs.getInt("twommperson_id")
        assertEquals 3,rs.getInt("twommbook_id")

        assert rs.next()
        assertEquals 1,rs.getInt("twommperson_id")
        assertEquals 4,rs.getInt("twommbook_id")

        assert rs.next()
        assertEquals 1,rs.getInt("twommperson_id")
        assertEquals 5,rs.getInt("twommbook_id")


        assert rs.next()
        assertEquals 2,rs.getInt("twommperson_id")        

    }


    void onSetUp() {
        this.gcl.parseClass('''
class TwoMMBook {
    Long id
    Long version

    String title
    Set authors
    Set publishers
    static belongsTo = TwoMMPerson

    static mappedBy = [publishers:'publishedBooks',
                       authors:'books']
    static hasMany = [authors:TwoMMPerson,
                      publishers:TwoMMPerson]
}
class TwoMMPerson {
    Long id
    Long version
    String name
    Set books
    Set publishedBooks

    static mappedBy = [books:'authors',
                       publishedBooks:'publishers']
    static hasMany = [books:TwoMMBook,
                   publishedBooks:TwoMMBook]
}


'''
        )
    }


}