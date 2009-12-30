package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FlushMode

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class CustomValidatorAndDynamicFinderTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Foo {
	String name

	Foo partner

	static constraints = {
		name(validator: {name, foo ->
			Foo similarFoo = Foo.findByNameIlike(name)
			if (similarFoo && similarFoo.id != foo.id) return ['similar']
		})
	}
}
''')
    }

    // test for GRAILS-4981
    void testCustomValidatorWithFinder() {
        session.setFlushMode(FlushMode.AUTO) 
        def Foo = ga.getDomainClass("Foo").clazz

        def foo = Foo.newInstance(name: 'partner1')
        assert foo.save()
        def partner = Foo.newInstance(name: 'partner2')
        assert partner.save()
        foo.partner = partner
        assert foo.save()

    }
}