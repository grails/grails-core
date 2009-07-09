package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.web.servlet.ModelAndView;

class ReturnModelAndViewController {
	 Closure withView = {
		return new ModelAndView("someView");
	}
	
	 Closure withoutView = {
		return new ModelAndView();
	}
	
	 String viewConfiguredView = "someOtherView";
	 Closure viewConfigured = {
		return new ModelAndView();
	}
	
	 String defaultClosure = "withView";
}