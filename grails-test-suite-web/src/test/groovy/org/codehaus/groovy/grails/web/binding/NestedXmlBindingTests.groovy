package org.codehaus.groovy.grails.web.binding

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/10/11
 * Time: 6:30 PM
 * To change this template use File | Settings | File Templates.
 */

@TestFor(NestedXmlController)
@Mock([Person, Location])
class NestedXmlBindingTests {

    void testNestedXmlBinding() {
        request.xml = '''
<person>
<name>John Doe</name>
<location>
<shippingAddress>foo</shippingAddress>
<billingAddress>bar</billingAddress>
</location>
</person>
'''
        def result = controller.bind()

        assert result != null

        Person p = result.person

        assert p != null
        assert p.name == "John Doe"
        assert p.location != null
        assert p.location.shippingAddress == 'foo'
        assert p.location.billingAddress == 'bar'
    }

    void testNestedXmlBindingWithId() {
        request.xml = '''
<person>
<name>John Doe</name>
<location id="1">
<shippingAddress>foo</shippingAddress>
<billingAddress>bar</billingAddress>
</location>
</person>
'''
        def result = controller.bind()

        assert result != null

        Person p = result.person

        assert p != null
        assert p.name == "John Doe"
        assert p.location != null
        assert p.location.id == 1
        assert p.location.shippingAddress == 'foo'
        assert p.location.billingAddress == 'bar'
    }
}
class NestedXmlController {
    def bind() {
        def person = new Person(params['person'])

        [person: person]
    }
}

@Entity
class Person {
    String name
    Location location
}

@Entity
class Location {
    String shippingAddress
    String billingAddress
}
