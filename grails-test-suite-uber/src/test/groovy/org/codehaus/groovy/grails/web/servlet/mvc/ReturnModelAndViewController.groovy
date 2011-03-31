package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.web.servlet.ModelAndView

class ReturnModelAndViewController {

    Closure withView = { new ModelAndView("someView") }

    Closure withoutView = { new ModelAndView() }

    String viewConfiguredView = "someOtherView"

    Closure viewConfigured = { new ModelAndView() }

    String defaultClosure = "withView"
}
