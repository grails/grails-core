package org.codehaus.groovy.grails.orm.hibernate;


class RequestBindingPersistenceTests extends AbstractGrailsHibernateTests {

	void testParentUpdate() {
		def stephenKing = gcl.loadClass("Author").newInstance()
		def johnGrisham = gcl.loadClass("Author").newInstance()
		def bookKujo = gcl.loadClass("Book").newInstance()
		
		stephenKing.name = "Stephen King"
		johnGrisham.name = "John Grisham"
		bookKujo.title = "Kujo"
		
		stephenKing.addBook(bookKujo)
		assert stephenKing.save()
		assert johnGrisham.save()
				
		assert stephenKing.books
		assert stephenKing.books.contains(bookKujo)
		assertEquals(stephenKing, bookKujo.author)
		
        // Reassign "Kujo" to John Grisham and save the book	
        johnGrisham.addBook(bookKujo)
        stephenKing.books.remove(bookKujo)
        
        assert johnGrisham.save()
        assert stephenKing.save()
        
        johnGrisham = johnGrisham.metaClass
                                 .invokeStaticMethod(johnGrisham,"get",johnGrisham.id)
        stephenKing = stephenKing.metaClass
                                 .invokeStaticMethod(stephenKing,"get",stephenKing.id)                                 
        // Verify that "Kujo" now has John Grisham as its author
        assertEquals(johnGrisham, bookKujo.author)
        // Verify that John Grisham now has "Kujo" in his list of books
		assert johnGrisham.books
        assert johnGrisham.books.contains(bookKujo)

        
        
        // Verify that Stephen King no longer has "Kujo" in his list of books
        assert !stephenKing.books
        assert !stephenKing.books.contains(bookKujo)
	}

	void onSetUp() {
		gcl.parseClass(
"""
class Book {
  Long id
  Long version
  def belongsTo = Author
  Author author
  String title
  boolean equals(obj) { title == obj?.title }
  int hashCode() { title ? title.hashCode() : super.hashCode() }
  String toString() { title }

  static constraints = {
      author(nullable:true)
  }
}
class Author {
  Long id
  Long version
  String name
  Set books
  def relatesToMany = [books:Book]
  boolean equals(obj) { name == obj?.name }
  int hashCode() { name ? name.hashCode() : super.hashCode() }
  String toString() { name }
}
"""				
		)
	}
	
	void onTearDown() {
		
	}
}
