package org.codehaus.groovy.grails.web.mapping

/**
 * @deprecated Use {@link grails.web.mapping.ResponseRedirector} instead
 */
@Deprecated
class ResponseRedirector extends grails.web.mapping.ResponseRedirector{
    ResponseRedirector(grails.web.mapping.LinkGenerator linkGenerator) {
        super(linkGenerator)
    }
}
