package org.codehaus.groovy.grails.web.mapping;

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests;

public class RootUrlMappingTests extends AbstractGrailsTagTests{

    public void onSetUp() {
        gcl.parseClass '''
class StoreUrlMappings {
    static mappings = {
	  "/"(controller:"store")
      "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
      "/"(view:"/index")
	  "500"(view:'/error')
	}
}'''

        gcl.parseClass '''
class StoreController {

    def index = { }

	def showTime = {
		render "${new Date()}"
	}
}

'''
    }


	void testMappingToControllerAndAction() {
		def template = '<g:link controller="store" action="showTime">Show the time !</g:link>'

	    assertOutputEquals('<a href="/store/showTime">Show the time !</a>', template)
	}

	void testMappingToController() {
		def template = '<g:link controller="store">Show the time !</g:link>'

        assertOutputEquals('<a href="/">Show the time !</a>', template)
		
	}

	
}