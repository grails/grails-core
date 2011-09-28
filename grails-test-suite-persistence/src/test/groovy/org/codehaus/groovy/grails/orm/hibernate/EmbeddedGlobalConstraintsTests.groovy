package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class EmbeddedGlobalConstraintsTests extends AbstractGrailsHibernateTests {
    @Override protected void onSetUp() {
        gcl.parseClass('''
grails.gorm.default.constraints = {
   '*'(nullable:true)
}
''', "Config")
    }


    void testEmbeddedDomainWithinDomain() {
        def p = new PersonWithNullableAddress(name:"Bob")

        assert p.save(flush:true) != null

        p.address = new NullableAddress()

        assert p.save(flush:true) != null

        session.clear()

        p = PersonWithNullableAddress.get(p.id)

        assert p.address == null

        p.address = new NullableAddress(street:"Blah")

        assert p.save(flush:true) != null

        session.clear()

        p = PersonWithNullableAddress.get(p.id)

        assert p.address != null
        assert p.address.street == "Blah"
        assert p.address.postCode == null
    }

    @Override protected getDomainClasses() {
        return [PersonWithNullableAddress]
    }
}

@Entity
class PersonWithNullableAddress {

    String name
    NullableAddress address

    static embedded = ["address"]
}

class NullableAddress {
    String street
    String postCode
}
