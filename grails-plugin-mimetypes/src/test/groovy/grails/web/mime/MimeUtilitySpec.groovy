package grails.web.mime

import grails.core.DefaultGrailsApplication
import grails.spring.BeanBuilder
import org.grails.plugins.web.mime.MimeTypesConfiguration
import org.grails.web.mime.DefaultMimeUtility
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class MimeUtilitySpec extends Specification {

    MimeUtility getMimeUtility() {
        def application = new DefaultGrailsApplication()
        application.config.grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

        final def mainContext = new GenericApplicationContext()
        mainContext.refresh()
        application.setApplicationContext(mainContext)

        def bb = new BeanBuilder()
        bb.beans {
            grailsApplication = application
            mimeConfiguration(MimeTypesConfiguration, application, [])
        }
        final ApplicationContext context = bb.createApplicationContext()
        final MimeTypesConfiguration mimeTypesConfiguration = context.getBean(MimeTypesConfiguration)
        return new DefaultMimeUtility(mimeTypesConfiguration.mimeTypes())
    }

    void "Test get mime by extension method"() {
        when:"We lookup the mime type for the js extension"
            def mimeType = mimeUtility.getMimeTypeForExtension("js")

        then:"The mime name should be 'text/javascript'"
            mimeType != null
            mimeType.extension == 'js'
            mimeType.name == 'text/javascript'

        when:"We lookup the mime type for an extension with multiple mime types"
            mimeType = mimeUtility.getMimeTypeForExtension("xml")

        then: "We get the first specified mime type back"
            mimeType != null
            mimeType.extension == 'xml'
            mimeType.name == 'text/xml'
    }

    void "Test get mime by URI method"() {
        when:"We lookup the mime type for the js extension"
            def mimeType = mimeUtility.getMimeTypeForURI("/myapp/js/jquery-1.8.1.js")

        then:"The mime name should be 'text/javascript'"
            mimeType != null
            mimeType.extension == 'js'
            mimeType.name == 'text/javascript'

        when:"We lookup the mime type for an extension with multiple mime types"
            mimeType = mimeUtility.getMimeTypeForURI("/WEB-INF/web.xml")

        then: "We get the first specified mime type back"
            mimeType != null
            mimeType.extension == 'xml'
            mimeType.name == 'text/xml'
    }
}
