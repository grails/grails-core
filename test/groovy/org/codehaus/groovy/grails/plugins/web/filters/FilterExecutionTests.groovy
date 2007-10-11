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

class FilterExecutionTests extends AbstractGrailsControllerTests {

    public void onSetUp() {
        gcl.parseClass('''
class AuthorController {
    def index = {}
    def list = {}
}
class Filters {
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
                request.beforeOne = "one"
            }
            after = afterClosure
            afterView = afterCompleteClosure
        }

		author(controller:"author") {
			before = {
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
    }

}