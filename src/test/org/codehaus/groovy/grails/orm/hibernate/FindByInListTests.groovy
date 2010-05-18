package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 27, 2008
 */
class FindByInListTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''

class FindByInListBook {
    Long id
    Long version

    String title
}
'''
    }

    void testFindInList() {
        def FindByInListBook = ga.getDomainClass("FindByInListBook").clazz
        createBooks FindByInListBook

        def results = FindByInListBook.findAllByTitleInList(['The Shining', 'Rose Madder'])

        assertNotNull results
        assertEquals 2, results.size()

        assertTrue "Should have returned 'The Shining' from inList query", results.any { it.title = "The Shining" }
        assertTrue "Should have returned 'Rose Madder' from inList query", results.any { it.title = "Rose Madder" }
    }

    void testFindInListEmpty() {
        def FindByInListBook = ga.getDomainClass("FindByInListBook").clazz
        createBooks FindByInListBook

        def results = FindByInListBook.findAllByTitleInList([])
        assertEquals 0, results.size()
    }

    void testFindInListEmptyUsingOr() {
        def FindByInListBook = ga.getDomainClass("FindByInListBook").clazz
        createBooks FindByInListBook

        def results = FindByInListBook.findAllByTitleInListOrTitle([], 'The Stand')
        assertEquals 1, results.size()

        assertEquals "Should have returned 'The Stand' from inList query", 'The Stand', results[0].title
    }

    private void createBooks(FindByInListBook) {
        assertNotNull FindByInListBook.newInstance(title:"The Stand").save(flush:true)
        assertNotNull FindByInListBook.newInstance(title:"The Shining").save(flush:true)
        assertNotNull FindByInListBook.newInstance(title:"Rose Madder").save(flush:true)
        session.clear()
    }
}
