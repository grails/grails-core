package org.grails.web.i18n

import grails.util.GrailsWebMockUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.SessionLocaleResolver

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ParamsAwareLocaleChangeInterceptorTests {

    @AfterEach
    protected void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    void testSwitchLocaleWithStringArrayParamsObject() {

        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        def request = webRequest.getCurrentRequest()
        def response = webRequest.getCurrentResponse()

        SessionLocaleResolver localeResolver = new SessionLocaleResolver()

        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,localeResolver)

        def localeChangeInterceptor = new ParamsAwareLocaleChangeInterceptor()
        localeChangeInterceptor.paramName = "lang"

        def locale = localeResolver.resolveLocale(request)
        assert localeChangeInterceptor.preHandle(request, response, null)

        assertEquals locale, localeResolver.resolveLocale(request)

        webRequest.params.lang = ["de_DE", "en_GB"] as String[]

        assert localeChangeInterceptor.preHandle(request, response, null)

        assertNotEquals locale, localeResolver.resolveLocale(request)

        locale = localeResolver.resolveLocale(request)

        assertEquals "de", locale.getLanguage()
        assertEquals "DE", locale.getCountry()
    }

    @Test
    void testSwitchLocaleWithParamsObject() {

        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

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

        assertNotEquals locale, localeResolver.resolveLocale(request)

        locale = localeResolver.resolveLocale(request)

        assertEquals "de", locale.getLanguage()
        assertEquals "DE", locale.getCountry()
    }

    @Test
    void testSwitchLocaleWithRequestParameter() {

        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

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

        assertNotEquals locale, localeResolver.resolveLocale(request)

        locale = localeResolver.resolveLocale(request)

        assertEquals "de", locale.getLanguage()
        assertEquals "DE", locale.getCountry()
    }
}
