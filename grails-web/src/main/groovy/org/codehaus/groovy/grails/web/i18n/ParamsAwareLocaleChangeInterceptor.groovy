package org.codehaus.groovy.grails.web.i18n

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.propertyeditors.LocaleEditor
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import org.springframework.web.servlet.support.RequestContextUtils

 /**
 * A LocaleChangeInterceptor instance that is aware of the Grails params object.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ParamsAwareLocaleChangeInterceptor extends LocaleChangeInterceptor {

    private static final LOG = LogFactory.getLog(ParamsAwareLocaleChangeInterceptor)

    String paramName = DEFAULT_PARAM_NAME

    void setParamName(String name) {
        paramName = name
        super.setParamName name
    }

    @Override
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)

        def params = webRequest.params

        def localeParam = params?.get(paramName)
        if (!localeParam) {
            return super.preHandle(request, response, handler)
        }

        try {
            // choose first if multiple specified
            if (localeParam.getClass().isArray()) {
                localeParam = localeParam[0]
            }
            def localeResolver = RequestContextUtils.getLocaleResolver(request)
            def localeEditor = new LocaleEditor()
            localeEditor.setAsText localeParam
            localeResolver?.setLocale request, response, localeEditor.value
            return true
        }
        catch (Exception e) {
            LOG.error("Error intercepting locale change: ${e.message}", e)
            return true
        }
    }
}
