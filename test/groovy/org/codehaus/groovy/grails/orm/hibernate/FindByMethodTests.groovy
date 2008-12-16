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
class Book {
    Long id
    Long version
    String title
    Date releaseDate
    static constraints  = {
        releaseDate(nullable:true)
    }
}
class User {
    Long id
    Long version
    String firstName

    Set books
    static hasMany = [books:Book]
}
'''
    }


    void testNullParameters() {
        def bookClass = ga.getDomainClass("Book").clazz

        assert bookClass.newInstance(title:"The Stand").save()

        assert bookClass.findByReleaseDate(null)
        assert bookClass.findByTitleAndReleaseDate("The Stand", null)

    }

    void testFindByIsNotNull() {
        def userClass = ga.getDomainClass("User").clazz

        userClass.newInstance(firstName:"Bob").save()
        userClass.newInstance(firstName:"Jerry").save()
        userClass.newInstance(firstName:"Fred").save(flush:true)

        def users = userClass.findAllByFirstNameIsNotNull()
        users = userClass.findAllByFirstNameIsNotNull()

        assertEquals 3, users.size()
    }

    // test for GRAILS-3712
    void testFindByWithJoinQueryOnAssociation() {

        def User = ga.getDomainClass("User").clazz

        def user = User.newInstance(firstName:"Stephen")
        assert user.addToBooks(title:"The Shining")
                 .addToBooks(title:"The Stand")
                 .save(flush:true)

        session.clear()
        
        user = User.findByFirstName("Stephen", [fetch:[books:'eager']])

        assertEquals 2, user.books.size()
    }

}