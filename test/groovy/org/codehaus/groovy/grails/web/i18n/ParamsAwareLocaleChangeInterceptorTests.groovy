package org.codehaus.groovy.grails.web.i18n

import org.springframework.web.context.request.RequestContextHolder
import grails.util.GrailsWebUtil
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import org.springframework.mock.web.MockHttpServletRequest

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 28, 2008
 */
class ParamsAwareLocaleChangeInterceptorTests extends GroovyTestCase{

    protected void tearDown() {
        RequestContextHolder.setRequestAttributes null
    }


    void testSwitchLocaleWithParamsObject() {

        def webRequest = GrailsWebUtil.bindMockWebRequest()

        def request = webRequest.getCurrentRequest()
        def response = webRequest.getCurrentResponse()

        SessionLocaleResolver localeResolver = new SessionLocaleResolver()

        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,localeResolver)

        def localeChangeInterceptor = new ParamsAwareLocaleChangeInterceptor()
        localeChangeInterceptor.paramName = "lang"

        def locale = localeResolver.resolveLocale(request)
        assert localeChangeInterceptor.preHandle(request, response, null)

        assertEquals locale, localeResolver.resolveLocale(request)

        webRequest.params.lang = "de_DE"

        assert localeChangeInterceptor.preHandle(request, response, null)

        assertNotSame locale, localeResolver.resolveLocale(request)

        locale = localeResolver.resolveLocale(request)

        assertEquals "de", locale.getLanguage()
        assertEquals "DE", locale.getCountry()

    }

    void testSwithLocaleWithRequestParameter() {

        def webRequest = GrailsWebUtil.bindMockWebRequest()

        MockHttpServletRequest request = webRequest.getCurrentRequest()
        def response = webRequest.getCurrentResponse()

        SessionLocaleResolver localeResolver = new SessionLocaleResolver()

        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,localeResolver)

        def localeChangeInterceptor = new ParamsAwareLocaleChangeInterceptor()
        localeChangeInterceptor.paramName = "lang"

        def locale = localeResolver.resolveLocale(request)
        assert localeChangeInterceptor.preHandle(request, response, null)

        assertEquals locale, localeResolver.resolveLocale(request)

        request.addParameter "lang", "de_DE"

        assert localeChangeInterceptor.preHandle(request, response, null)

        assertNotSame locale, localeResolver.resolveLocale(request)

        locale = localeResolver.resolveLocale(request)

        assertEquals "de", locale.getLanguage()
        assertEquals "DE", locale.getCountry()
    }

}