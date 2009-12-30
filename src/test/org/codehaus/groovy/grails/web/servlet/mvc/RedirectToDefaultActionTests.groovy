package org.codehaus.groovy.grails.web.servlet.mvc
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class RedirectToDefaultActionTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?"{
	        constraints {
		        // apply constraints here
		    }
	    }
	}
}

class PortalController {

    static defaultAction = 'content'

    def content = {
        redirect(controller:'repository')
    }

}
class RepositoryController {

	def index = {
		render "hello world"
	}

}
''')
    }


    void testRedirect() {
        def c = ga.getControllerClass("PortalController").newInstance()

        c.content()

        assertEquals "/repository", response.redirectedUrl
    }

}