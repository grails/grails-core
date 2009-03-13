package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 27, 2008
 */
class SortMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

class SortMappingBook {
    Long id
    Long version
    String title
    SortMappingAuthor author
    static belongsTo = [author:SortMappingAuthor]

    static mapping = {
        sort title:'desc'
    }

}

class SortMappingAuthor {
    Long id
    Long version

    String name
    Set books
    Set unibooks

    static hasMany = [books:SortMappingBook]

    static mapping = {
        sort 'name'
        books sort:'title'
    }
}

''')
    }

    void testDefaultSortOrderWithFinder() {
        def authorClass = ga.getDomainClass("SortMappingAuthor").clazz

        assert authorClass.newInstance(name:"Stephen King").save(flush:true)
        assert authorClass.newInstance(name:"Lee Child").save(flush:true)
        assert authorClass.newInstance(name:"James Patterson").save(flush:true)
        assert authorClass.newInstance(name:"Dean Koontz").save(flush:true)

        session.clear()

        def authors = authorClass.findAllByNameLike("%e%")

        assertEquals "Dean Koontz", authors[0].name
        assertEquals "James Patterson", authors[1].name
        assertEquals "Lee Child", authors[2].name
        assertEquals "Stephen King", authors[3].name

    }

    void testDefaultSortOrder() {
        def authorClass = ga.getDomainClass("SortMappingAuthor").clazz

        assert authorClass.newInstance(name:"Stephen King").save(flush:true)
        assert authorClass.newInstance(name:"Lee Child").save(flush:true)
        assert authorClass.newInstance(name:"James Patterson").save(flush:true)
        assert authorClass.newInstance(name:"Dean Koontz").save(flush:true)

        session.clear()

        def authors = authorClass.list()

        assertEquals "Dean Koontz", authors[0].name
        assertEquals "James Patterson", authors[1].name
        assertEquals "Lee Child", authors[2].name
        assertEquals "Stephen King", authors[3].name        
    }

    void testDefaultSortOrderMapSyntax() {
        def authorClass = ga.getDomainClass("SortMappingAuthor").clazz
        def bookClass = ga.getDomainClass("SortMappingBook").clazz


        def author = authorClass.newInstance(name:"John")
                                    .addToBooks(title:"E")
                                    .addToBooks(title:"C")
                                    .addToBooks(title:"Z")
                                    .addToBooks(title:"A")
                                    .addToBooks(title:"K")
                                    .save(flush:true)

        assert author

        session.clear()


        def books = bookClass.list()

        assertEquals "Z", books[0].title
        assertEquals "K", books[1].title
        assertEquals "E", books[2].title
        assertEquals "C", books[3].title
        assertEquals "A", books[4].title

    }

    void testSortMapping() {
        def authorClass = ga.getDomainClass("SortMappingAuthor").clazz


        def author = authorClass.newInstance(name:"John")
                                    .addToBooks(title:"E")
                                    .addToBooks(title:"C")
                                    .addToBooks(title:"Z")
                                    .addToBooks(title:"A")
                                    .addToBooks(title:"K")
                                    .save(flush:true)

        assert author

        session.clear()

        author = authorClass.get(1)

        assert author

        def books = author.books.toList()

        assertEquals "A", books[0].title
        assertEquals "C", books[1].title
        assertEquals "E", books[2].title
        assertEquals "K", books[3].title
        assertEquals "Z", books[4].title


    }
}