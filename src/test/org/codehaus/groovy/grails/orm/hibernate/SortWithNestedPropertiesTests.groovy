package org.codehaus.groovy.grails.orm.hibernate

/**
 * Test for GRAILS-3911
 */
class SortWithNestedPropertiesTests extends AbstractGrailsHibernateTests {
    
    def bookClass

    protected void onSetUp() {
        gcl.parseClass '''
            class Book {
                Long id
                Long version
                String title
                Author author
                String publisher = 'Manning'
                static namedQueries = {
                    manningBooks {
                        eq('publisher', 'Manning')
                    }
                }
            }
            
            class Author {
                Long id
                Long version
                String name
                Person person
            }
            
            class Person {
                Long id
                Long version
                String name
            }
        '''
    }

    protected void setUp() {
        super.setUp()
        
        def personClass = ga.getDomainClass('Person').clazz
        def authorClass = ga.getDomainClass('Author').clazz
        bookClass = ga.getDomainClass('Book').clazz
        ['C','A','b','a','c','B'].eachWithIndex { name, i ->
            def person = personClass.newInstance(id:i, version:1, name:name).save(flush:true)
            def author = authorClass.newInstance(id:i, version:1, name:name, person:person).save(flush:true)
            bookClass.newInstance(id:i, version:1, title:'foo', author:author).save(flush:true)
        }
    }
    
    void testListPersistentMethod() {
        assertEquals( ['A','a','b','B','C','c'], bookClass.list(sort:'author.name').author.name )
        assertEquals( ['A','B','C','a','b','c'], bookClass.list(sort:'author.name', ignoreCase:false).author.name )
    }

    void testHibernateNamedQueriesBuilder() {
        assertEquals( ['A','a','b','B','C','c'], bookClass.manningBooks().list(sort:'author.name').author.name )
    }

    void testFindAllPersistentMethod() {
        assertEquals( ['A','a','b','B','C','c'], bookClass.findAll([sort:'author.name']).author.name )
    }

    void testFindAllByPersistentMethod() {
        assertEquals( ['A','a','b','B','C','c'], bookClass.findAllByPublisher('Manning', [sort:'author.name']).author.name )
    }

    void testFindByPersistentMethod() {
        assertEquals( 'A', bookClass.findByPublisher('Manning', [sort:'author.name']).author.name )
    }
    
    void testDeepSort() {
        assertEquals( ['A','a','b','B','C','c'], bookClass.list(sort:'author.person.name').author.person.name )
    }

}
