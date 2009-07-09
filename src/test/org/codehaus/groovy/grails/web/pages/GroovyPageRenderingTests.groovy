package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException
import grails.util.Environment

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 5, 2009
 */

public class GroovyPageRenderingTests extends AbstractGrailsTagTests{

    void testGroovyPageExpressionExceptionInDevelopmentEnvironment() {
        def template = '${foo.bar.next}'

        shouldFail(GroovyPagesException) {
            applyTemplate(template)
        }
        
    }

    void testGroovyPageExpressionExceptionInOtherEnvironments() {
        def template = '${foo.bar.next}'

        System.setProperty(Environment.KEY, "production")

        shouldFail(NullPointerException) {
            applyTemplate(template)
        }
    }

    protected void onDestroy() {
        System.setProperty(Environment.KEY, "")
    }


}