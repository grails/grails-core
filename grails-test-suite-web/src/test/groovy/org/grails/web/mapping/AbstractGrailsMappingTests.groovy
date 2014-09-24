package org.grails.web.mapping

import grails.web.mapping.UrlMappingEvaluator
import grails.core.GrailsApplication
import org.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.grails.web.mapping.DefaultUrlMappingEvaluator

abstract class AbstractGrailsMappingTests extends AbstractGrailsControllerTests {
    public UrlMappingEvaluator evaluator

    protected void setUp() {
        super.setUp()
        servletContext.setAttribute(GrailsApplication.APPLICATION_ID, ga)
        evaluator = new DefaultUrlMappingEvaluator(servletContext)
    }
}
