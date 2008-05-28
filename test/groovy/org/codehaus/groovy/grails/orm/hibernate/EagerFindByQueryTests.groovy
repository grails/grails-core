package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 28, 2008
 */
class EagerFindByQueryTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class EagerFindByQueryBookmark {
    Long id
    Long version
    String url
    Set tags
    static hasMany = [tags:EagerFindByQueryTag]
}
class EagerFindByQueryTag {
    Long id
    Long version
    String name
}
''')
    }


    void testDefaultLazyFetchingGet() {

        def bookmarkClass = ga.getDomainClass("EagerFindByQueryBookmark").clazz
        def tagClass = ga.getDomainClass("EagerFindByQueryTag").clazz

        assert bookmarkClass.newInstance(url:"http://grails.org")
                             .addToTags(name:"groovy")
                             .addToTags(name:"web")
                             .addToTags(name:"development")
                             .save(flush:true)

        session.clear()

        def bookmark = bookmarkClass.get(1)

        assertFalse Hibernate.isInitialized(bookmark.tags)
    }

    void testDefaultLazyFetchingFindBy() {

        def bookmarkClass = ga.getDomainClass("EagerFindByQueryBookmark").clazz
        def tagClass = ga.getDomainClass("EagerFindByQueryTag").clazz

        assert bookmarkClass.newInstance(url:"http://grails.org")
                             .addToTags(name:"groovy")
                             .addToTags(name:"web")
                             .addToTags(name:"development")
                             .save(flush:true)

        session.clear()

        def bookmark = bookmarkClass.findByUrl("http://grails.org")

        assertFalse Hibernate.isInitialized(bookmark.tags)
    }

    
    void testEagerFetchingFindBy() {
     def bookmarkClass = ga.getDomainClass("EagerFindByQueryBookmark").clazz
        def tagClass = ga.getDomainClass("EagerFindByQueryTag").clazz

        assert bookmarkClass.newInstance(url:"http://grails.org")
                             .addToTags(name:"groovy")
                             .addToTags(name:"web")
                             .addToTags(name:"development")
                             .save(flush:true)

        session.clear()

        def bookmark = bookmarkClass.findByUrl("http://grails.org",[fetch:[tags:'eager']])

        assertTrue Hibernate.isInitialized(bookmark.tags)
    }

   void testEagerFetchingFindAllBy() {
     def bookmarkClass = ga.getDomainClass("EagerFindByQueryBookmark").clazz
        def tagClass = ga.getDomainClass("EagerFindByQueryTag").clazz

        assert bookmarkClass.newInstance(url:"http://grails.org")
                             .addToTags(name:"groovy")
                             .addToTags(name:"web")
                             .addToTags(name:"development")
                             .save(flush:true)

        session.clear()

        def bookmarks = bookmarkClass.findAllByUrl("http://grails.org",[fetch:[tags:'eager']])
        def bookmark = bookmarks[0]
       
        assertTrue Hibernate.isInitialized(bookmark.tags)
    }


}