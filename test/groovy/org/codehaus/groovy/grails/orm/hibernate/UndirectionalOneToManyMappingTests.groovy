package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 29, 2008
 */
class UndirectionalOneToManyMappingTests extends AbstractGrailsHibernateTests {

    void testUnidirectionalOneToManyMapping() {
        def Author = ga.getDomainClass("MappedU2mAuthor").clazz
        def Book = ga.getDomainClass("MappedU2mBook").clazz

        def a = Author.newInstance(name:"Stephen King")

        a.addToBooks(Book.newInstance(title:"The Shining"))
         .addToBooks(Book.newInstance(title:"The Stand"))
         .save(true)
        assertEquals 2, Book.list().size()



        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from um_author_books")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 1,rs.getInt("um_author_id")
        assertEquals 1,rs.getInt("um_book_id")

        assert rs.next()
        assertEquals 1,rs.getInt("um_author_id")
        assertEquals 2,rs.getInt("um_book_id")        


    }
    protected void onSetUp() {
        gcl.parseClass('''
class MappedU2mBook {
	Long id
	Long version

    String title
}
class MappedU2mAuthor {
	Long id
	Long version
    String name
	Set books
	def hasMany = [books:MappedU2mBook]

    static mapping = {
        books joinTable:[name:"um_author_books", key:'um_author_id', column:'um_book_id']
    }
}

''')
    }


}