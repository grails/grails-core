package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 27, 2008
 */
class FindByInListTests extends AbstractGrailsHibernateTests{
    protected void onSetUp() {
        gcl.parseClass('''

class FindByInListBook {
    Long id
    Long version

    String title

}
''')
    }


    void testFindInList() {
        def bookClass = ga.getDomainClass("FindByInListBook").clazz

        assert bookClass.newInstance(title:"The Stand").save(flush:true)
        assert bookClass.newInstance(title:"The Shining").save(flush:true)
        assert bookClass.newInstance(title:"Rose Madder").save(flush:true)


        session.clear()

        def results = bookClass.findAllByTitleInList(['The Shining', 'Rose Madder'])

        assert results
        assertEquals 2, results.size()

        assertTrue "Should have returned 'The Shining' from inList query", results.any { it.title = "The Shining" }
        assertTrue "Should have returned 'Rose Madder' from inList query", results.any { it.title = "Rose Madder" }

    }
}