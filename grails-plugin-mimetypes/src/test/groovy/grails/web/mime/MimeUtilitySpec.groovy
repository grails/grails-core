package grails.web.mime

import grails.web.mime.MimeUtility
import grails.core.DefaultGrailsApplication
import org.grails.plugins.web.mime.MimeTypesFactoryBean
import org.grails.web.mime.DefaultMimeUtility
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class MimeUtilitySpec extends Specification {

    MimeUtility getMimeUtility() {
        def ga = new DefaultGrailsApplication()
        ga.config.grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
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

        final factory = new MimeTypesFactoryBean(grailsApplication: ga)

        def mimeTypes = factory.getObject()
        return new DefaultMimeUtility(mimeTypes)
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
