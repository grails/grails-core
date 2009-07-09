package org.codehaus.groovy.grails.orm.hibernate

import org.springframework.core.JdkVersion
import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 29, 2008
 */
class BasicArrayAndCollectionMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        if(JdkVersion.isAtLeastJava15()) {
            gcl.parseClass('''
class BasicCollections {
    Long id
    Long version

    Set names
    Set numbers
    Set moreNumbers
    Set shortNames
    List todos

    static hasMany = [names:String,
                      numbers:Integer,
                      moreNumbers:Integer,
                      shortNames:String,
                      todos:String]

    static mapping = {
       numbers joinTable:[name:'bunch_o_numbers', key:'basic_id', column:'number']
       shortNames joinTable:[column:'short', unique:2]
       
    }    
}
''')
        }
    }


    void testCollectionOfIntegers() {
        def BasicCollections = ga.getDomainClass("BasicCollections").clazz

        def test = BasicCollections.newInstance()

//        test.addToNames("one") TODO: addToNames not working for basic types
//            .addToNames("two")
//            .save(flush:true)

        test.moreNumbers = [5, 12] as Set
        assert test.save(flush:true)

        session.clear()

        test = BasicCollections.get(1)

        assertEquals 2, test.moreNumbers.size()
        assertTrue test.moreNumbers.contains(5)
        assertTrue test.moreNumbers.contains(12)

        def c = session.connection()
        def ps = c.prepareStatement("select * from basic_collections_more_numbers")

        def rs = ps.executeQuery()

        assert rs.next()

        assertEquals 1, rs.getLong("basic_collections_id")

        long num = rs.getLong("more_numbers_integer")
        assertTrue num == 5 || num == 12

    }

    void testDomain() {
        GrailsDomainClass dc = ga.getDomainClass("BasicCollections")

        assertTrue dc.getPropertyByName("numbers").isBasicCollectionType()
        assertTrue dc.getPropertyByName("names").isBasicCollectionType()
    }

    void testListOfBasicTypes() {
       def BasicCollections = ga.getDomainClass("BasicCollections").clazz

        def test = BasicCollections.newInstance()

//        test.addToNames("one") TODO: addToNames not working for basic types
//            .addToNames("two")
//            .save(flush:true)

        test.todos = ["one", "two"]
        assert test.save(flush:true)

        session.clear()

        test = BasicCollections.get(1)

        assertEquals 2, test.todos.size()
        assertEquals(["one", "two"], test.todos)
    }

    void testChangeColumnLength() {
        def BasicCollections = ga.getDomainClass("BasicCollections").clazz

        def test = BasicCollections.newInstance()

//        test.addToNames("one") TODO: addToNames not working for basic types
//            .addToNames("two")
//            .save(flush:true)

        test.shortNames = ["AB", "CD"] as Set
        assert test.save(flush:true)

        session.clear()

        test = BasicCollections.get(1)

        assertEquals 2, test.shortNames.size()
    }

    void testJoinTableMapping() {
        def BasicCollections = ga.getDomainClass("BasicCollections").clazz

        def test = BasicCollections.newInstance()

//        test.addToNames("one") TODO: addToNames not working for basic types
//            .addToNames("two")
//            .save(flush:true)

        test.numbers = [5, 12] as Set
        assert test.save(flush:true)

        session.clear()

        test = BasicCollections.get(1)

        assertEquals 2, test.numbers.size()
        assertTrue test.numbers.contains(5)
        assertTrue test.numbers.contains(12)

        def c = session.connection()
        def ps = c.prepareStatement("select * from bunch_o_numbers")

        def rs = ps.executeQuery()

        assert rs.next()

        assertEquals 1, rs.getLong("basic_id")

        long num = rs.getLong("number")
        assertTrue num == 5 || num == 12
    }

    void testDefaultMapping() {
        def BasicCollections = ga.getDomainClass("BasicCollections").clazz

        def test = BasicCollections.newInstance()

        assert test.addToNames("one")
                    .addToNames("two")
                    .save(flush:true)


        session.clear()

        test = BasicCollections.get(1)

        assertEquals 2, test.names.size()
        assertTrue test.names.contains("one")
        assertTrue test.names.contains("two")


        def c = session.connection()
        def ps = c.prepareStatement("select * from basic_collections_names")

        def rs = ps.executeQuery()

        assert rs.next()

        assertEquals 1, rs.getLong("basic_collections_id")

        def num = rs.getString("names_string")
        assertTrue num == "one" || num == "two"        
    }


}