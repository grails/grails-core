package org.codehaus.groovy.grails.web.binding

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

@TestFor(ExcludingController)
@Mock([Person, Location])
class BindingExcludeTests {

    @Test
    void testThatAssociationsAreExcluded() {
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
        def model = controller.bind()

        def p = model.person
        assert p.name == 'John Doe'
        assert p.locations.size() == 0

    }
}

class ExcludingController {
    def bind() {
        def p = new Person()
        bindData(p, request, [exclude:"locations"])
        return [person:p]
    }
}
