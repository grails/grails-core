package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 31/05/2011
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
class EmbeddedGlobalConstraintsTests extends AbstractGrailsHibernateTests {
    @Override protected void onSetUp() {
        gcl.parseClass('''
grails.gorm.default.constraints = {
   '*'(nullable:true)
}
''', "Config")
    }


    void testEmbeddedDomainWithinDomain() {
        def p = new Person(name:"Bob")

        assert p.save(flush:true) != null

        p.address = new Address()

        assert p.save(flush:true) != null

        session.clear()

        p = Person.get(p.id)

        assert p.address == null

        p.address = new Address(street:"Blah")

        assert p.save(flush:true) != null

        session.clear()


        p = Person.get(p.id)

        assert p.address != null
        assert p.address.street == "Blah"
        assert p.address.postCode == null
    }

    @Override protected getDomainClasses() {
        return [Person]
    }


}
@Entity
class Person {

	String name
	Address address

	static embedded = ["address"]

    static constraints = {
    }
}


class Address {
	String street
	String postCode
}
