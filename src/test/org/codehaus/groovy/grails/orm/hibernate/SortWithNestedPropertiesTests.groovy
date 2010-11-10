package org.codehaus.groovy.grails.orm.hibernate

/**
 * Test for GRAILS-3911
 */
class SortWithNestedPropertiesTests extends AbstractGrailsHibernateTests {
    
    def bookClass

    protected void onSetUp() {
        gcl.parseClass '''
            class BookX {
                Long id
                Long version
                String title
                AuthorX author
                String publisher = 'Manning'
                static namedQueries = {
                    manningBooks {
                        eq('publisher', 'Manning')
                    }
                }
            }
            
            class AuthorX {
                Long id
                Long version
                String name
                PersonX person
            }
            
            class PersonX {
                Long id
                Long version
                String name
            }
            '''
    }

    protected void setUp() {
        super.setUp()
        
        def personClass = ga.getDomainClass('PersonX').clazz
        def authorClass = ga.getDomainClass('AuthorX').clazz
        bookClass = ga.getDomainClass('BookX').clazz
        ['C','A','b','a','c','B'].eachWithIndex { name, i ->
            def person = personClass.newInstance(id:i, version:1, name:name).save(flush:true)
            def author = authorClass.newInstance(id:i, version:1, name:name, person:person).save(flush:true)
            bookClass.newInstance(id:i, version:1, title:'foo', author:author).save(flush:true)
        }
    }
    
    void testListPersistentMethod() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.list(sort:'author.name').author.name )
        assertEquals( ['A','B','C','a','b','c'], bookClass.list(sort:'author.name', ignoreCase:false).author.name )
    }

    void testHibernateNamedQueriesBuilder() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.manningBooks().list(sort:'author.name').author.name )
    }

    void testFindAllPersistentMethod() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.findAll([sort:'author.name']).author.name )
    }

    void testFindAllByPersistentMethod() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.findAllByPublisher('Manning', [sort:'author.name']).author.name )
    }

    void testFindByPersistentMethod() {
        assertEquals( 'a', bookClass.findByPublisher('Manning', [sort:'author.name']).author.name )
    }
    
    void testDeepSort() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.list(sort:'author.person.name').author.person.name )
    }

}
