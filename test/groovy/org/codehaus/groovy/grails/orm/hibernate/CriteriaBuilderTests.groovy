package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate
import org.hibernate.Criteria

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 27, 2008
 */
class CriteriaBuilderTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class CriteriaBuilderBook {
    Long id
    Long version
    CriteriaBuilderAuthor author
    String title
    static belongsTo = [author:CriteriaBuilderAuthor]

    static mapping = {
        cache true
    }
}

class CriteriaBuilderAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:CriteriaBuilderBook]

    static mapping = {
        cache true
    }
    
}
''')
    }


    void testSizeCriterion() {
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        assert authorClass.newInstance(name:"Stephen King")
                                    .addToBooks(title:"The Shining")
                                    .addToBooks(title:"The Stand")
                                    .addToBooks(title:"Rose Madder")
                                    .save(flush:true)

        assert authorClass.newInstance(name:"James Patterson")
                                    .addToBooks(title:"Along Came a Spider")
                                    .addToBooks(title:"A Time to Kill")
                                    .addToBooks(title:"Killing Me Softly")
                                    .addToBooks(title:"The Quickie")
                                    .save(flush:true)


        def results = authorClass.withCriteria {
            sizeGt('books', 3)
        }


        assertEquals 1, results.size()

        results = authorClass.withCriteria {
            sizeGe('books', 3)
        }

        assertEquals 2, results.size()

        results = authorClass.withCriteria {
            sizeNe('books', 1)
        }

        assertEquals 2, results.size()

        results = authorClass.withCriteria {
            sizeNe('books', 3)
        }
        assertEquals 1, results.size()

        results = authorClass.withCriteria {
            sizeLt('books', 4)
        }

        assertEquals 1, results.size()

        results = authorClass.withCriteria {
            sizeLe('books', 4)
        }

        assertEquals 2, results.size()
    }

    void testCacheMethod() {
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                    .addToBooks(title:"The Shining")
                                    .addToBooks(title:"The Stand")
                                    .save(flush:true)

        assert author


        session.clear()

        def authors = authorClass.withCriteria {
            eq('name', 'Stephen King')

            def criteriaInstance = getInstance()
            assertTrue criteriaInstance.cacheable
        }


        assertEquals 1, authors.size()
        

        // NOTE: note sure how to actually test the cache, I'm testing
        // that invoking the cache method works but need a better test
        // that ensure entries are pulled from the cache
        println "Second query"

        authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
            cache false

            def criteriaInstance = getInstance()
            assertFalse criteriaInstance.cacheable
            
        }

        assertEquals 1, authors.size()
    }

    void testLockMethod() {

        // NOTE: HSQLDB doesn't support the SQL SELECT..FOR UPDATE syntax so this test
        // is basically just testing that the lock method can be called without error
        
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                    .addToBooks(title:"The Shining")
                                    .addToBooks(title:"The Stand")
                                    .save(flush:true)


        assert author

        session.clear()

        def authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
            lock true

        }

        assert authors

        // test lock association

        authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
            books {
                lock true
            }
        }

        assert authors
    }

    void testJoinMethod() {        
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                    .addToBooks(title:"The Shining")
                                    .addToBooks(title:"The Stand")
                                    .save(flush:true)


        assert author

        session.clear()

        def authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
        }


        assert authors
        author = authors[0]

        assertFalse "books association is lazy by default and shouldn't be initialized",Hibernate.isInitialized(author.books)

        session.clear()

        authors = authorClass.withCriteria {
           eq('name', 'Stephen King')
           join "books"
        }
        author = authors[0]

        assertTrue "books association loaded with join query and should be initialized",Hibernate.isInitialized(author.books)

    }

}