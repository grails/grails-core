/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 27, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource
import org.hibernate.SessionFactory

class MappingDslTests extends AbstractGrailsHibernateTests {

    void testTableMapping() {
         DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from people")
             statement.execute()
         } finally {
             con.close()
         }
    }

    void testColumnNameMappings() {
         def p = ga.getDomainClass("PersonDSL").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()

         DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select First_Name from people")
             def result = statement.executeQuery()
             assert result.next()
             def name = result.getString('First_Name')

             assertEquals "Wilma", name

         } finally {
             con.close()
         }
    }

    void testDisabledVersion() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()


         assertNull p.version
    }

    void testEnabledVersion() {
        def p = ga.getDomainClass("PersonDSL2").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()

         assertEquals 0, p.version

        p.firstName = "Bob"
        p.save()
        session.flush()

        assertEquals 1, p.version

    }

    void testCustomHiLoIdGenerator() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()

         assert p.id
         DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from hi_value")
             def result = statement.executeQuery()
             assert result.next()
             def value = result.getLong('next_value')

             assertEquals 1, value

         } finally {
             con.close()
         }
    }



    void testLazyinessControl() {
        def personClass = ga.getDomainClass("PersonDSL")
        def p = personClass.newInstance()
         p.firstName = "Wilma"

        println "SAVING PERSON"
        p.save(flush:true)
        p.addToChildren(firstName:"Dino", lastName:'Dinosaur')
        p.addToCousins(firstName:"Bob", lastName:'The Builder')
        println "SAVING RELATIONS"
        p.save(flush:true) 
         session.clear()

         p = personClass.clazz.get(1)

         assertTrue p.children.wasInitialized()
         assertFalse p.cousins.wasInitialized()

    }

    void testUserTypes() {
        DataSource ds = (DataSource)applicationContext.getBean('dataSource')
        def relativeClass = ga.getDomainClass("Relative")
        def r = relativeClass.newInstance()
         r.firstName = "Wilma"
         r.lastName = 'Flintstone'
         r.save()
         session.flush()

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from relative")
             def result = statement.executeQuery()
             assert result.next()
             def metadata = result.getMetaData()
             assertEquals "FIRST_NAME",metadata.getColumnLabel(3)
             // hsqldb returns -1 for text type, if it wasn't mapped as text it would be 12 so this is an ok test
             assertEquals( -1, metadata.getColumnType(3) )


         } finally {
             con.close()
         }
    }


    void testCompositeIdMapping() {
        def compositePersonClass = ga.getDomainClass("CompositePerson")
        def cp = compositePersonClass.newInstance()

        cp.firstName = "Fred"
        cp.lastName = "Flintstone"
        cp.save()
        session.flush()
        session.clear()

        cp = compositePersonClass.newInstance()

        cp.firstName = "Fred"
        cp.lastName = "Flintstone"

        def cp1 = compositePersonClass.clazz.get(cp)

        assert cp1
        assertEquals "Fred", cp1.firstName
        assertEquals "Flintstone", cp1.lastName
    }

    void testTablePerSubclassInheritance() {
        DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from payment")
             statement.execute()
             statement = con.prepareStatement("select * from credit_card_payment")
             statement.execute()

         } finally {
             con.close()
         }

         def p = ga.getDomainClass("Payment").newInstance()

         p.amount = 10
         p.save()
         session.flush()

         def ccp = ga.getDomainClass("CreditCardPayment").newInstance()
         ccp.amount = 20
         ccp.cardNumber = "43438094834380"
         ccp.save()
         session.flush()

         session.clear()

         ccp = ga.getDomainClass("CreditCardPayment").clazz.findByAmount(20)

         assert ccp
         assertEquals 20, ccp.amount
         assertEquals  "43438094834380", ccp.cardNumber
    }

    void testOneToOneForeignKeyMapping() {
        def personClass = ga.getDomainClass("MappedPerson").clazz
        def addressClass = ga.getDomainClass("MappedAddress").clazz

        def p = personClass.newInstance(name:"John")
        p.address = addressClass.newInstance()


        assert p.save()
        session.flush()

        DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select PERSON_ADDRESS_COLUMN from mapped_person")
             def resultSet = statement.executeQuery()
             assert resultSet.next()
         } finally {
             con.close()
         }
    }

    void testManyToOneForeignKeyMapping() {
        def personClass = ga.getDomainClass("MappedPerson").clazz
        def groupClass = ga.getDomainClass("MappedGroup").clazz

        def g = groupClass.newInstance()

        g.addToPeople name:"John"

        assert g.save()

        session.flush()
        session.clear()

        g = groupClass.get(1)

        assert g
        assertEquals 1, g.people.size()

        DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select PERSON_GROUP_COLUMN from mapped_person")
             def resultSet = statement.executeQuery()
             assert resultSet.next()
         } finally {
             con.close()
         }

    }

    void testManyToManyForeignKeyMapping() {
        def partnerClass = ga.getDomainClass("MappedPartner").clazz
        def groupClass = ga.getDomainClass("MappedGroup").clazz

        def g = groupClass.newInstance()

        g.addToPartners(partnerClass.newInstance())

        assert g.save()
        session.flush()
        session.clear()

        g = groupClass.get(1)

        assert g
        assertEquals 1, g.partners.size()

        DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select PARTNER_JOIN_COLUMN,GROUP_JOIN_COLUMN from PARTNER_GROUP_ASSOCIATIONS")
             def resultSet = statement.executeQuery()
             assert resultSet.next()
         } finally {
             con?.close()
         }
    }

    void testUnidirectionalOneToManyForeignKeyMapping() {
     def personClass = ga.getDomainClass("MappedPerson").clazz
     def childClass = ga.getDomainClass("MappedChild").clazz

        def p = personClass.newInstance(name:"John")

        p.addToChildren(childClass.newInstance())
        p.addToCousins(childClass.newInstance())
        p.save()


        assert p.save()
        session.flush()

        DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select PERSON_ID,COUSIN_ID from COUSINS_TABLE")
             def resultSet = statement.executeQuery()
             assert resultSet.next()
         } finally {
             con.close()
         }

        
    }

    protected void onSetUp() {
        gcl.parseClass('''
class MappedPerson {
    Long id
    Long version
    String name
    MappedAddress address
    MappedGroup group
    Set children
    Set cousins

    static hasMany = [children:MappedChild, cousins:MappedChild]
    static belongsTo = MappedGroup
    static mapping = {
        columns {
            address column:'PERSON_ADDRESS_COLUMN'
            group column:'PERSON_GROUP_COLUMN'
            children column:'PERSON_CHILD_ID'
            cousins joinTable:[name:'COUSINS_TABLE', key:'PERSON_ID', column:'COUSIN_ID']
        }
    }
    static constraints = {
        group(nullable:true)
        address(nullable:true)
    }

}
class MappedChild {
    Long id
    Long version
}
class MappedAddress {
    Long id
    Long version

    static belongsTo = MappedPerson

}
class MappedGroup {
    Long id
    Long version

    Set people
    Set partners
    static hasMany = [people:MappedPerson, partners:MappedPartner]
    static mapping = {
        columns {
            partners column:'PARTNER_JOIN_COLUMN', joinTable:'PARTNER_GROUP_ASSOCIATIONS'
        }
    }

}
class MappedPartner {
    Long id
    Long version

    Set groups
    static belongsTo = MappedGroup
    static hasMany = [groups:MappedGroup]
    static mapping = {
        columns {
            groups column:'GROUP_JOIN_COLUMN', joinTable:'PARTNER_GROUP_ASSOCIATIONS'
        }
    }

}
class Payment {
    Long id
    Long version
    Integer amount

    static mapping = {
        tablePerHierarchy false
    }
}
class CreditCardPayment extends Payment  {
    String cardNumber
}

class CompositePerson implements Serializable {
    Long id
    Long version
    String firstName
    String lastName

    static mapping = {
        id composite:['firstName', 'lastName']
    }
}
class PersonDSL {
    Long id
    Long version
    String firstName
    Set children
    Set cousins

    static hasMany = [children:Relative, cousins:Relative]
    static mapping = {
        table 'people'
        version false
        cache usage:'read-only', include:'non-lazy'
        id generator:'hilo', params:[table:'hi_value',column:'next_value',max_lo:100]

        columns {
            firstName name:'First_Name'
            children lazy:false, cache:'read-write\'
        }
    }
}

class Relative {
    Long id
    Long version

    String firstName
    String lastName

    static mapping = {
        columns {
            firstName type:'text', index:'name_index'
            lastName index:'name_index,other_index'
        }
    }
}

class PersonDSL2 {
    Long id
    Long version
    String firstName

    static mapping = {
        table 'people2'
        version true
        cache usage:'read-write', include:'non-lazy'
        id column:'person_id'

        columns {
            firstName name:'First_Name'
        }
    }
}
        ''')
    }




}