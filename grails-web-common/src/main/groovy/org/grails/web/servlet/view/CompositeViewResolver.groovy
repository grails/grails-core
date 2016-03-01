package org.grails.web.servlet.view

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver

/**
 * Performs the job of iterating across registered view resolvers and returning the first matching view similar to the
 * hard coded behavior in DispatcherServlet
 *
 * @author Graeme Rocher
 * @since 3.1.1
 */
@CompileStatic
class CompositeViewResolver {

    public static final String BEAN_NAME = "compositeViewResolver"

    @Autowired(required = false)
    List<ViewResolver> viewResolvers = []

    View resolveView(String viewName, Locale locale) {
        for(resolver in viewResolvers) {

            def view = resolver.resolveViewName(viewName, locale)
            if(view != null) {
                return view
            }
        }
        return null
    }
}
