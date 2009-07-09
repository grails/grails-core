package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 23, 2009
 */

public class ReverseMappingWithDefaultActionTests extends AbstractGrailsTagTests{


    void onSetUp() {
        gcl.parseClass('''      
class CustomUrlMappings {
    static mappings = {
	  "/rest/link/$id?"(resource:'link') {
		    constraints {
				id(matches: /\\d+/)
		    }
		}

      "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
      "/"(view:"/index")
	  "500"(view:'/error')
	}
}
''')

        gcl.parseClass('''
class LinkController {}
''')
    }


    void testReverseMappings() {
        // link to only controller
        def template = '<g:createLink controller="link" />'

        assertOutputEquals "/rest/link", template

        // link to controller and rest action
        template = '<g:createLink controller="link" action="save" />'

        assertOutputEquals "/rest/link", template

        // link to controller and non-rest action
        template = '<g:createLink controller="link" action="foo" />'

        assertOutputEquals "/link/foo", template
    }
}