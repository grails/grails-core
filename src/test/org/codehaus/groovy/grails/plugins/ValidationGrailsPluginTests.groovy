package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.plugins.web.AbstractGrailsPluginTests
import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.context.support.StaticMessageSource

public class ValidationGrailsPluginTests extends AbstractGrailsPluginTests {

    void onSetUp() {
        def config = new ConfigSlurper().parse("grails.validateable.packages= ['org.codehaus.groovy.grails.plugins']")
        ConfigurationHolder.config = config

        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.ValidationGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        ctx.registerMockBean('messageSource', new StaticMessageSource())
    }

    void testAnnotatedClass() {
        if(notYetImplemented()) return
        def svc = gcl.loadClass('org.codehaus.groovy.grails.plugins.SomeValidateableClass').newInstance()
        svc.name = 'Jeff'
        assertTrue svc.validate()
        svc.name = 'Zack'
        assertFalse svc.validate()
    }
}


@Validateable
class SomeValidateableClass {

    String name

    static constraints = {
        name matches: /J.*/
    }
}
