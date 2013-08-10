package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import org.codehaus.groovy.grails.orm.hibernate.cfg.MapFakeUserType

class CustomUserTypeOverridesGrailsMapMappingTests extends AbstractGrailsHibernateTests {

    void testUserTypeOverridesGrailsMapMappingTests() {
        def d = new DomainUserTypeMappings()

        d.myMap = [foo:"bar"]

        assert d.save(flush:true) != null

        // the map should not be mapped onto a join table but instead a single column
        session.connection().prepareStatement("select my_map from domain_user_type_mappings").execute()

        session.clear()

        d = DomainUserTypeMappings.get(d.id)

        assert d != null
    }

    @Override protected getDomainClasses() {
        [DomainUserTypeMappings]
    }
}

@Entity
class DomainUserTypeMappings {
    Map<String, String> myMap

    static mapping = {
        myMap type:MapFakeUserType
    }
}
