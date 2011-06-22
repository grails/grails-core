package org.codehaus.groovy.grails.web.servlet.mvc

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class RedirectToDefaultActionTests extends AbstractGrailsControllerTests {

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

''')
    }

    @Override
    protected Collection<Class> getControllerClasses() {
        [PortalController, RepositoryController]
    }

     void testRedirect() {
         def c = ga.getControllerClass(PortalController.name).newInstance()
         c.content()
         assertEquals "/repository/index", response.redirectedUrl
     }

     void testRedirectToExplicitDefaultAction() {
         def c = ga.getControllerClass(RepositoryController.name).newInstance()
         c.toPortal()
         assertEquals "/portal/content", response.redirectedUrl
     }
}
class PortalController {

    static defaultAction = 'content'

    def content = { redirect(controller:'repository') }
}

class RepositoryController {
    def index = { render "hello world" }

    def toPortal = { redirect(controller: "portal") }
}
