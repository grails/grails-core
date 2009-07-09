package org.codehaus.groovy.grails.web.mapping;

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.commons.GrailsApplication;

public abstract class AbstractGrailsMappingTests extends AbstractGrailsControllerTests {
    public UrlMappingEvaluator evaluator

    protected void setUp() {
        super.setUp()
        servletContext.setAttribute(GrailsApplication.APPLICATION_ID, ga)
        this.evaluator = new DefaultUrlMappingEvaluator(servletContext)
    }
}
