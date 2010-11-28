package org.codehaus.groovy.grails.orm.hibernate

/**
 * Test for GRAILS-3911
 */
class SortWithNestedPropertiesTests extends AbstractGrailsHibernateTests {
    
    def bookClass

    protected void onSetUp() {
        gcl.parseClass '''
            import grails.persistence.*
            @Entity
            class BookX {
                String title
                AuthorX author
                Address address
                String publisher = 'Manning'
                static embedded = ["address"]
                static namedQueries = {
                    manningBooks {
                        eq('publisher', 'Manning')
                    }
                }
                static mapping = {
                    sort 'author.name'
                }
            }
            
            @Entity
            class AuthorX {
                String name
                PersonX person
            }
            
            @Entity
            class PersonX {
                String name
            }
            
            class Address {
                String street
                String city
            }
            '''
    }

    protected void setUp() {
        super.setUp()
        
        def personClass = ga.getDomainClass('PersonX').clazz
        def authorClass = ga.getDomainClass('AuthorX').clazz
        bookClass = ga.getDomainClass('BookX').clazz
        def addressClass = ga.classLoader.loadClass("Address")
        ['C','A','b','a','c','B'].eachWithIndex { name, i ->
            def person = personClass.newInstance(id:i, version:1, name:name).save(flush:true)
            def author = authorClass.newInstance(id:i, version:1, name:name, person:person).save(flush:true)
            def address = addressClass.newInstance(street:name, city:'Oslo')
            bookClass.newInstance(id:i, version:1, title:'foo', author:author, address:address).save(flush:true)
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

    void testPreserveOtherParameters() {
        assertEquals( ['B','b','c'], bookClass.list(max:3, offset:2, sort:'author.name').author.name )
        assertEquals( ['C','a','b'], bookClass.list(max:3, offset:2, sort:'author.name', ignoreCase:false).author.name )
        assertEquals( ['B','b','c'], bookClass.manningBooks().list(max:3, offset:2, sort:'author.name').author.name )
        assertEquals( ['B','b','c'], bookClass.findAll([max:3, offset:2, sort:'author.name']).author.name )
        assertEquals( ['B','b','c'], bookClass.findAllByPublisher('Manning', [max:3, offset:2, sort:'author.name']).author.name )
        assertEquals( ['B','b','c'], bookClass.list(max:3, offset:2, sort:'author.person.name').author.person.name )
    }
    
    void testSortByEmbeddedProperty() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.list(sort:'address.street').address.street)
    }

    void testDefaultSort() {
        assertEquals( ['a','A','B','b','c','C'], bookClass.list().address.street)
    }
}
