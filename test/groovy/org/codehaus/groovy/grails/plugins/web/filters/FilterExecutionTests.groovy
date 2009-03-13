/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 11, 2007
 */
package org.codehaus.groovy.grails.plugins.web.filters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

class FilterExecutionTests extends AbstractGrailsControllerTests {

    public void onSetUp() {
        FilterConfig.metaClass.getLog = {->
            [ error: { msg -> println "ERROR: $msg" },
              warn: { msg -> println "WARN: $msg" },
              info: { msg -> println "INFO: $msg" }]
        }

		gcl.parseClass(
'''
class ItemController {
    def count = {
        render(view:'testView')
    }

	def show = {
		render(template:"xmlTemplate",contentType:"text/xml")
	}
}
''')

        gcl.parseClass('''\
import junit.framework.Assert

class AuthorController {
    def index = {}
    def list = {}
}
class Filters {
    // Test property on the filters definition.
    def myName = "John Doe"

    // Test method that returns a string.
    def sum(list) {
        return list.sum()
    }

    // Test multi-argument method.
    def fullName(String firstName, String lastName) {
        return "$firstName $lastName".toString()
    }

	def beforeClosure = { ctx ->
		println "***beforeClosure: $ctx.uri"
		return true
	}

	def afterClosure = {
		println "afterClosure!!!"
	}

	def afterCompleteClosure = {
		println "afterCompleteClosure!!!"
	}

    def filters = {
      "default"(controller:"*", action:"*") {
            before = {
                // Check that the filters property is available. This
                // tests that FilterConfig's propertyMissing handling
                // is working.
                Assert.assertEquals("Filters property 'myName' not available.", "John Doe", myName)

                // And check the multi-arg method.
                Assert.assertEquals("Multi-arg method 'fullName' not available.", "Jane Doe", fullName('Jane', 'Doe'))

                request.beforeOne = "one"
            }
            after = afterClosure
            afterView = afterCompleteClosure
        }

		author(controller:"author") {
			before = {
                // Check that the filters method is available. This
                // tests that FilterConfig's methodMissing handling
                // is working.
                Assert.assertEquals("Filters method 'sum' not available.", 10, sum([ 1, 2, 3, 4 ]))

                request.beforeTwo = "two"
			}
		}

		list(action:"list") {
			before = {
				request.beforeThree = "three"
			}
		}

		uri(uri:"/*/list") {
			before = {
                request.beforeFour = "four"
                false
			}
		}

        testRedirect(controller: "admin", action: "index") {
            before = {
                request.beforeSix = "number6"
                redirect(uri: '/')
                return false
            }
        }

        testRedirectToController(controller: "admin", action: "save") {
            after = {
                redirect(controller: "admin", action: "show")
            }
        }

        testRender(controller: "person", action: "*") {
            before = {
                render(text:"<xml>some xml</xml>",contentType:"text/xml",encoding:"UTF-8")
                return false
            }
        }

        testRenderWithViewBefore(controller: "item", action: "count") {
            before = {
                render(view: "error", model: [ total: 1000 ])
            }
        }

        testRenderWithViewAfter(controller: "item", action: "*") {
            after = {
                render(view: "happyPath")
            }
        }

        testRenderString(controller: "account", action: "index") {
            before = {
                render "Page not available"
                return false
            }
        }

		shouldNotGetHere(controller:"*") {
			before = {
                request.beforeFive = "five"
                false
			}
		}
    }
}
        ''')
    }


    void testFilterMatching() {
        HandlerInterceptor filterInterceptor = appCtx.getBean("filterInterceptor")

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/author/list")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "author")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "list")

        filterInterceptor.preHandle(request, response, null)

        // all befores should have been executed
        assertEquals "one", request.beforeOne
        assertEquals "two", request.beforeTwo
        assertEquals "three", request.beforeThree
        assertEquals "four", request.beforeFour
        assert !request.beforeFive

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/author/show")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "author")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "show")

        filterInterceptor.preHandle(request, response, null)

        // only first two should have been
        assertEquals "one", request.beforeOne
        assertEquals "two", request.beforeTwo
        assert !request.beforeThree
        assert !request.beforeFour
        assertEquals "five", request.beforeFive

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/book/show")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "book")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "show")

        filterInterceptor.preHandle(request, response, null)

        // only first two should have been
        assertEquals "one", request.beforeOne
        assert !request.beforeTwo
        assert !request.beforeThree
        assert !request.beforeFour
        assertEquals "five", request.beforeFive

        // Test the plain redirect within a 'before' interceptor.
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/admin/index")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "admin")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "index")

        // Check that 'false' is returned by the interceptor and that
        // we have have been redirected to the home page.
        assert !filterInterceptor.preHandle(request, response, null)
        assertEquals "number6", request.beforeSix
        assertEquals "/", response.redirectedUrl

        // Test the redirect to a specific controller and action, within
        // an 'after' interceptor.
        response.committed = false
        response.reset()
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/admin/save")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "admin")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "save")

        // Check that we have been redirected to the expected URL.
        filterInterceptor.postHandle(request, response, null, null)
        assertEquals "/admin/show", response.redirectedUrl

        // Test the rendering of some XML in a 'before' interceptor.
        response.committed = false
        response.reset()
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/person/show/5")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "person")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "show")

        // Check that the response contains the expected XML.
        assert !filterInterceptor.preHandle(request, response, null)
        assertEquals "<xml>some xml</xml>", response.contentAsString
        assertTrue response.contentType.startsWith("text/xml")
        assertEquals "UTF-8", response.characterEncoding

        // Test the rendering of some plain text in a 'before' interceptor.
        response.committed = false
        response.reset()
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/account/index")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "account")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "index")

        // Check that the response contains the expected XML.
        assert !filterInterceptor.preHandle(request, response, null)
        assertEquals "Page not available", response.contentAsString

        // Test the rendering of a view in a 'before' interceptor.
        response.committed = false
        response.reset()
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/item/count")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "item")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "count")
        webRequest.controllerName = "item"

        // Check that the new model and view have been set.
        def filterConfig = filterInterceptor.handlers.find{ it.filterConfig.name == "testRenderWithViewBefore" }.filterConfig
        
        assert !filterInterceptor.preHandle(request, response, null)
        assertEquals 1000, filterConfig.modelAndView.model['total']
        assertEquals "/item/error", filterConfig.modelAndView.viewName

        // Test the rendering of a view in an 'after' interceptor.
        response.committed = false
        response.reset()
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/item/show/5")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "item")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "show")
        webRequest.controllerName = "item"

        // Check that the new model and view have been set.
        ModelAndView mv = new ModelAndView()
        filterInterceptor.postHandle(request, response, null, mv)
        assertEquals "/item/happyPath", mv.viewName
    }
}
