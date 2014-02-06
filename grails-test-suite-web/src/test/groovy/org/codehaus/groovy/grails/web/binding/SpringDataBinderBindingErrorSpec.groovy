package org.codehaus.groovy.grails.web.binding

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.GrailsWebUtil
import grails.validation.Validateable

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Issue
import spock.lang.Specification


@TestMixin(GrailsUnitTestMixin)
class SpringDataBinderBindingErrorSpec extends Specification {

    @Issue('GRAILS-11044')
    void 'Test binding errors when using the Spring binder'() {
        given: 'The spring binder is being used'
        def appCtx = new MockApplicationContext()
        appCtx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication)
        appCtx.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        GrailsWebUtil.bindMockWebRequest(appCtx)
        grailsApplication.config.grails.databinding.useSpringBinder=true
        grailsApplication.configChanged()
        
        when: 'a binding error occurs'
        RequestContextHolder.getRequestAttributes().currentRequest.setParameter 'number', 'forty two'
        def house = new House()
        DataBindingUtils.bindObjectToInstance(house, null)
        
        then: ' the non-domain class object has the expected binding error associated with it'
        house.hasErrors()
        house.errors.errorCount == 1
        house.errors.getFieldError('number').code == 'typeMismatch'
    }
}

@Validateable
class House {
    Integer number
}