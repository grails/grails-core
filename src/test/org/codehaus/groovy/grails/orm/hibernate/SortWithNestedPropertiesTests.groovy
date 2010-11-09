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
            class Book {
                String title
                Author author
                Address address
                String publisher = 'Manning'
                static embedded = ["address"]
                static namedQueries = {
                    manningBooks {
                        eq('publisher', 'Manning')
                    }
                }
            }
            
            @Entity
            class Author {
                String name
                Person person
            }
            
            @Entity
            class Person {
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
        
        def personClass = ga.getDomainClass('Person').clazz
        def authorClass = ga.getDomainClass('Author').clazz
        bookClass = ga.getDomainClass('Book').clazz
        def addressClass = ga.classLoader.loadClass("Address")
        ['C','A','b','a','c','B'].eachWithIndex { name, i ->
            def person = personClass.newInstance(id:i, version:1, name:name).save(flush:true)
            def author = authorClass.newInstance(id:i, version:1, name:name, person:person).save(flush:true)
            def address = addressClass.newInstance(street:name, city:'Oslo')
            bookClass.newInstance(id:i, version:1, title:'foo', author:author, address:address).save(flush:true)
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

    void testPreserveOtherParameters() {
        assertEquals( ['b','B','C'], bookClass.list(max:3, offset:2, sort:'author.name').author.name )
        assertEquals( ['C','a','b'], bookClass.list(max:3, offset:2, sort:'author.name', ignoreCase:false).author.name )
        assertEquals( ['b','B','C'], bookClass.manningBooks().list(max:3, offset:2, sort:'author.name').author.name )
        assertEquals( ['b','B','C'], bookClass.findAll([max:3, offset:2, sort:'author.name']).author.name )
        assertEquals( ['b','B','C'], bookClass.findAllByPublisher('Manning', [max:3, offset:2, sort:'author.name']).author.name )
        assertEquals( ['b','B','C'], bookClass.list(max:3, offset:2, sort:'author.person.name').author.person.name )
    }
    
    void testSortByEmbeddedProperty() {
        assertEquals( ['A','a','b','B','C','c'], bookClass.list(sort:'address.street').address.street)
    }
}
