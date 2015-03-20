package org.grails.web.binding

import grails.artefact.Artefact
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

    @Test
    void testBindingExcludeExpressedAsGstringExclude() {
        def model = controller.bindWithGstringExclude()

        def l = model.location
        assert l.shippingAddress == 'Shipping Address'
        assert l.billingAddress == null
    }
}

@Artefact('Controller')
class ExcludingController {
    def bind() {
        def p = new Person()
        bindData(p, request, [exclude:"locations"])
        return [person:p]
    }

    def bindWithGstringExclude() {
        def l = new Location()
        def qualifier = 'billing'
        def bindingSource = [shippingAddress: 'Shipping Address', billingAddress: 'Billing Address']
        bindData l, bindingSource, [exclude: "${qualifier}Address"]
        [location: l]
    }
}
