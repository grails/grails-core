package org.grails.web.servlet.mvc

class TestController {
    Closure test = {
        return [ "test" : "123" ]
    }
}
