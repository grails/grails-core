package org.grails.plugins.web.mime

import grails.core.DefaultGrailsApplication
import grails.spring.BeanBuilder
import grails.web.mime.MimeType
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class MimeTypesConfigurationSpec extends Specification {

    void "test when no mimeTypes configured then default should be used"() {
        setup:
        def application = new DefaultGrailsApplication()
        def bb = new BeanBuilder()
        bb.beans {
            grailsApplication = application
            mimeConfiguration(MimeTypesConfiguration, application, [])
        }
        ApplicationContext applicationContext = bb.createApplicationContext()

        when:
        MimeTypesConfiguration mimeTypesConfiguration = applicationContext.getBean(MimeTypesConfiguration)

        then:
        MimeType.createDefaults() == mimeTypesConfiguration.mimeTypes()

    }
}
