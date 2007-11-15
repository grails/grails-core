/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 15, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class InheritanceWithOneToOneTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class Product{
    Long id
    Long version
    String name
    Attachment detailPicture
    Attachment previewPicture
}
class Book extends Product {
    Long id
    Long version
    String title
}
class Attachment {
    Long id
    Long version
    static belongsTo = [Product]
    String filepath
}


''')
    }

    void testOneToOneInSuperClass() {

        def bookClass = ga.getDomainClass("Book").clazz
        def attClass = ga.getDomainClass("Attachment").clazz

        def book= bookClass.newInstance(name:"King Series",title:"The Stand")

        book.detailPicture = attClass.newInstance(filepath:"/path/to/det")
        book.previewPicture = attClass.newInstance(filepath:"/path/to/prev")

        assert book.save()
        session.flush()
        session.clear()

        book = bookClass.get(1)

        assert book
        assert book.detailPicture
        assert book.previewPicture

        assertEquals "King Series", book.name
        assertEquals "The Stand", book.title        

        assertEquals "/path/to/det", book.detailPicture.filepath
        assertEquals "/path/to/prev", book.previewPicture.filepath        

    }
}
