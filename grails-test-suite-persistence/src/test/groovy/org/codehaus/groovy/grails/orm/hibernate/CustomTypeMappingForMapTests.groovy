package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class CustomTypeMappingForMapTests extends AbstractGrailsHibernateTests {

    void testCustomTypeWithMapMapping() {
        def d = new DomainWithUserTypeMappings()

        d.name = "Bob"
        d.ser = "12345"
        d.map = [foo:"bar"]

        assert d.save(flush:true) != null

        shouldFail {
            // the map should not be mapped onto a join table but instead a single column
            session.connection().prepareStatement("select * from domain_with_user_type_mappings_map").execute()
        }

        session.clear()

        d = DomainWithUserTypeMappings.get(d.id)

        assert d != null
        assert d.map != null
    }

    @Override protected getDomainClasses() {
        [DomainWithUserTypeMappings]
    }
}

@Entity
class DomainWithUserTypeMappings {
    String name
    String ser
    Map<String, String> map

    static mapping = {
        ser type:'serializable'
        map type:'serializable'
    }
}
