package org.codehaus.groovy.grails.plugins.web.filters

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class FilterExecutionTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        FilterConfig.metaClass.getLog = { ->
            [error: { msg -> println "ERROR: $msg" },
             warn: { msg -> println "WARN: $msg" },
             info: { msg -> println "INFO: $msg" }]
        }

        gcl.parseClass '''
import junit.framework.Assert

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
      itRocks(action: 'b*', actionExclude: 'bieb*') {
          before = {
              request.rocks = 'yes'
          }
      }

      blues(controller: 'w*', controllerExclude: 'wing*') {
          before = {
              request.blues = 'yes'
          }
      }

      funky(uri: '/b*', uriExclude: '/blackoak*') {
          before = {
              request.funky = 'yes'
          }
      }

      heavy(uriExclude: '/music/pop*') {
          before = {
              request.heavy = 'yes'
          }
      }

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

        author(controller:"filterAuthor") {
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

        testRenderWithViewBefore(controller: "filterItem", action: "count") {
            before = {
                render(view: "error", model: [ total: 1000 ])
            }
        }

        testRenderWithViewAfter(controller: "filterItem", action: "*") {
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

class Group1Filters {
    def dependsOn = [Group3Filters]

    // these filters should run last since Group1Filters depends on them
    def filters = {
        filter1(uri:"/dependsOn") {
            before = {
                request.testString = request.testString + '4'
            }
        }
        filter2(uri:"/dependsOn") {
            before = {
                request.testString = request.testString + '5'
            }
        }
    }
}

class Group2Filters {

    // these filters should run first since they have no dependencies
    def filters = {
        filter3(uri:"/dependsOn") {
            before = {
                request.testString = '1'
            }
        }
        filter4(uri:"/neverCall") {
            before = {
                // uri doesn't match so this should not be called
                request.testString = request.testString + 'SHOULD_NEVER_HAPPEN'
            }
        }
    }
}

class Group3Filters {

    // these filters should run after Group2Filters and before Group1Filters
    def filters = {
        filter5(uri:"/dependsOn") {
            before = {
                request.testString = request.testString + '2'
            }
        }
        filter6(uri:"/dependsOn") {
            before = {
                request.testString = request.testString + '3'
            }
        }
    }
}
        '''
    }

     void testFilterExclusions() {
        HandlerInterceptor filterInterceptor = appCtx.getBean("filterInterceptor")

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/band/beardfish")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "band")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "beardfish")

        filterInterceptor.preHandle(request, response, null)
        assertEquals 'yes', request.rocks

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/band/bieber")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "band")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "bieber")

        filterInterceptor.preHandle(request, response, null)
        assertNull request.rocks

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/winter")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "winter")

        filterInterceptor.preHandle(request, response, null)
        assertEquals 'yes', request.blues

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/winger")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "winger")

        filterInterceptor.preHandle(request, response, null)
        assertNull request.blues

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/blackcrowes")

        filterInterceptor.preHandle(request, response, null)
        assertEquals 'yes', request.funky

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/blackoakarkansas")

        filterInterceptor.preHandle(request, response, null)
        assertNull request.funky

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/music/metal")

        filterInterceptor.preHandle(request, response, null)
        assertEquals 'yes', request.heavy

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/music/poprock")

        filterInterceptor.preHandle(request, response, null)
        assertNull request.heavy
    }

//    void testFilterOrdering() {
//        HandlerInterceptor filterInterceptor = appCtx.getBean("filterInterceptor")
//
//        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/dependsOn")
//        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "test")
//        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "index")
//
//        filterInterceptor.preHandle(request, response, null)
//        assertEquals 'filters did not run in the expected order', '12345', request.testString
//    }

    void testFilterMatching() {
        HandlerInterceptor filterInterceptor = appCtx.getBean("filterInterceptor")

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/filterAuthor/list")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "filterAuthor")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "list")

        filterInterceptor.preHandle(request, response, null)

        // all befores should have been executed
        assertEquals "one", request.beforeOne
        assertEquals "two", request.beforeTwo
        assertEquals "three", request.beforeThree
        assertEquals "four", request.beforeFour
        assert !request.beforeFive

        request.clearAttributes()

        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/filterAuthor/show")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "filterAuthor")
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
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/filterItem/count")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "filterItem")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "count")
        webRequest.controllerName = "filterItem"

        // Check that the new model and view have been set.
        def filterConfig = filterInterceptor.handlers.find{ it.filterConfig.name == "testRenderWithViewBefore" }.filterConfig

        assert !filterInterceptor.preHandle(request, response, null)
        assertEquals 1000, filterConfig.modelAndView.model['total']
        assertEquals "/filterItem/error", filterConfig.modelAndView.viewName

        // Test the rendering of a view in an 'after' interceptor.
        response.committed = false
        response.reset()
        request.clearAttributes()
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/filterItem/show/5")
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, "filterItem")
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "show")
        webRequest.controllerName = "filterItem"

        // Check that the new model and view have been set.
        ModelAndView mv = new ModelAndView()
        filterInterceptor.postHandle(request, response, null, mv)
        assertEquals "/filterItem/happyPath", mv.viewName
    }
}
