/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class FindByMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class FindByMethodBook {
    Long id
    Long version
    String title
    Date releaseDate
    static constraints  = {
        releaseDate(nullable:true)
    }
}
class FindByMethodUser {
    Long id
    Long version
    String firstName

    Set books
    static hasMany = [books:FindByMethodBook]
}
class FindByBooleanPropertyBook {
    Long id
    Long version
    String author
    String title
    Boolean published
}
'''
    }


    void testNullParameters() {
        def bookClass = ga.getDomainClass("FindByMethodBook").clazz

        assert bookClass.newInstance(title:"The Stand").save()

        assert bookClass.findByReleaseDate(null)
        assert bookClass.findByTitleAndReleaseDate("The Stand", null)

    }

    void testFindByIsNotNull() {
        def userClass = ga.getDomainClass("FindByMethodUser").clazz

        userClass.newInstance(firstName:"Bob").save()
        userClass.newInstance(firstName:"Jerry").save()
        userClass.newInstance(firstName:"Fred").save(flush:true)

        def users = userClass.findAllByFirstNameIsNotNull()
        users = userClass.findAllByFirstNameIsNotNull()

        assertEquals 3, users.size()
    }

    // test for GRAILS-3712
    void testFindByWithJoinQueryOnAssociation() {

        def User = ga.getDomainClass("FindByMethodUser").clazz

        def user = User.newInstance(firstName:"Stephen")
        assert user.addToBooks(title:"The Shining")
                 .addToBooks(title:"The Stand")
                 .save(flush:true)

        session.clear()

        user = User.findByFirstName("Stephen", [fetch:[books:'eager']])

        assertEquals 2, user.books.size()
    }

    void testBooleanPropertyQuery() {
        def bookClass = ga.getDomainClass("FindByBooleanPropertyBook").clazz
        assert bookClass.newInstance(author: 'Jeff', title: 'Fly Fishing For Everyone', published: false).save()
        assert bookClass.newInstance(author: 'Jeff', title: 'DGGv2', published: true).save()
        assert bookClass.newInstance(author: 'Graeme', title: 'DGGv2', published: true).save()
        assert bookClass.newInstance(author: 'Dierk', title: 'GINA', published: true).save()

        def book = bookClass.findPublishedByAuthor('Jeff')
        assertEquals 'Jeff', book.author
        assertEquals 'DGGv2', book.title

        book = bookClass.findPublishedByAuthor('Graeme')
        assertEquals 'Graeme', book.author
        assertEquals 'DGGv2', book.title

        book = bookClass.findPublishedByTitleAndAuthor('DGGv2', 'Jeff')
        assertEquals 'Jeff', book.author
        assertEquals 'DGGv2', book.title

        book = bookClass.findNotPublishedByAuthor('Jeff')
        assertEquals 'Fly Fishing For Everyone', book.title

        book = bookClass.findPublishedByTitleOrAuthor('Fly Fishing For Everyone', 'Dierk')
        assertEquals 'GINA', book.title

        def books = bookClass.findAllPublishedByTitle('DGGv2')
        assertEquals 2, books?.size()

        books = bookClass.findAllPublishedByTitleAndAuthor('DGGv2', 'Graeme')
        assertEquals 1, books?.size()

        books = bookClass.findAllPublishedByAuthorOrTitle('Graeme', 'GINA')
        assertEquals 2, books?.size()

    }

}