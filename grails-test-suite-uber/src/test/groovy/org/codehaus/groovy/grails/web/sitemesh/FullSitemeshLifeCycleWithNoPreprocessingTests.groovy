package org.codehaus.groovy.grails.web.sitemesh

import org.junit.Ignore

import com.opensymphony.module.sitemesh.RequestConstants

class FullSitemeshLifeCycleWithNoPreprocessingTests extends FullSitemeshLifeCycleTests {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        def config = new ConfigSlurper().parse('''
grails.views.gsp.sitemesh.preprocess=false
''')
        buildMockRequest(config)
        def page = new GSPSitemeshPage()
        request.setAttribute(RequestConstants.PAGE, page)
        request.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, page)
    }

}
