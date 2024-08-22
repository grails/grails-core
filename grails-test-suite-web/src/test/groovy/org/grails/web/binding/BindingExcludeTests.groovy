package org.grails.web.binding

import grails.artefact.Artefact
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class BindingExcludeTests extends Specification implements ControllerUnitTest<ExcludingController>, DataTest {

    Class[] getDomainClassesToMock() {
        [Person, Location]
    }

    void testThatAssociationsAreExcluded() {
        when:
        request.method = "POST"
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

        then:
        p.name == 'John Doe'
        p.locations.size() == 0
    }

    void testBindingExcludeExpressedAsGstringExclude() {
        when:
        def model = controller.bindWithGstringExclude()
        def l = model.location

        then:
        l.shippingAddress == 'Shipping Address'
        l.billingAddress == null
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
