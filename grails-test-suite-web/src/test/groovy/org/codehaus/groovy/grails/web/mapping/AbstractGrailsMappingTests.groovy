package org.codehaus.groovy.grails.web.mapping

import grails.web.mapping.UrlMappingEvaluator
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.grails.web.mapping.DefaultUrlMappingEvaluator

abstract class AbstractGrailsMappingTests extends AbstractGrailsControllerTests {
    public UrlMappingEvaluator evaluator

    protected void setUp() {
        super.setUp()
        servletContext.setAttribute(GrailsApplication.APPLICATION_ID, ga)
        evaluator = new DefaultUrlMappingEvaluator(servletContext)
    }
}
