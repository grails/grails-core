package org.grails.plugins.testing

import grails.converters.XML

import grails.core.DefaultGrailsApplication
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer

import spock.lang.Issue
import spock.lang.Specification

class GrailsMockHttpServletRequestSpec extends Specification {
    @Issue("GRAILS-11493")
    def "should allow setting request.xml with XML instance"() {
        given:
            ConvertersConfigurationInitializer initializer=new ConvertersConfigurationInitializer()
            initializer.initialize(new DefaultGrailsApplication())
            GrailsMockHttpServletRequest request=new GrailsMockHttpServletRequest()
        when:
            request.xml = new XML([a:1, b:2, c:3])
        then:
            request.inputStream.getText('UTF-8') == '<?xml version="1.0" encoding="UTF-8"?><map><entry key="a">1</entry><entry key="b">2</entry><entry key="c">3</entry></map>'
    }
}
