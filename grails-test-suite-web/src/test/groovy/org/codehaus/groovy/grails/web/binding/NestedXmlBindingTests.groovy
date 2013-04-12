package org.codehaus.groovy.grails.web.binding

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

@TestFor(NestedXmlController)
@Mock([Person, Location, Foo, Bar])
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
    
    void testBindToArrayOfDomains() {
        request.xml = '''
<person>
   <name>John Doe</name>
   <locations>
      <location>
         <shippingAddress>foo</shippingAddress>
         <billingAddress>bar</billingAddress>
      </location>
      <location>
         <shippingAddress>foo2</shippingAddress>
         <billingAddress>bar2</billingAddress>
      </location>
   </locations>
</person>
'''
        def result = controller.bind()

        assert result != null

        Person p = result.person

        assert p != null
        assert p.name == "John Doe"
        assert p.locations.size() == 2
        assert p.locations[0].shippingAddress == 'foo'
        assert p.locations[0].billingAddress == 'bar'
        assert p.locations[1].shippingAddress == 'foo2'
        assert p.locations[1].billingAddress == 'bar2'

    }

    void testBindToOne() {
        request.xml = '''<?xml version="1.0" encoding="UTF-8"?>
<foo>
<bar id="1" />
</foo>
'''
        new Bar().save(flush:true)
        def result = controller.bindToOne()

        assert result != null

        assert result.bar != null
        assert result.bar.id == 1
    }

    void testBindToArrayOfDomainsWithJson() {
        request.json = '''
{
'class': 'Person',
"person": {
"name": "John Doe",
"locations": {
"location": [
{ "shipppingAddress": "foo", "billingAddress": "bar" },

{ "shipppingAddress": "foo2", "billingAddress": "bar2" }
]
}
}
}
'''
        def result = controller.bind()

        assert result != null

        Person p = result.person

        assert p != null
        assert p.name == "John Doe"
        assert p.locations.size() == 2
    }
}
class NestedXmlController {
    def bind() {
        println params['person']
        def person = new Person(params['person'])

        [person: person]
    }

    def bindToOne() {
        def fooInstance = new Foo(params['foo'])
        return  fooInstance
    }
}

@Entity
class Person {
    String name
    Location location
    List<Location> locations = []
    static hasMany = [locations:Location]
}

@Entity
class Location {
    String shippingAddress
    String billingAddress
    
    static constraints = {
        id bindable: true
    }
}

@Entity
class Foo {

    static belongsTo = [bar: Bar];

}
@Entity
class Bar {

}
