package org.codehaus.groovy.grails.web.i18n

import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.support.RequestContextUtils
import org.springframework.beans.propertyeditors.LocaleEditor
import org.springframework.web.context.request.RequestContextHolder

/**
 * A LocaleChangeInterceptor instance that is aware of the Grails params object
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 28, 2008
 */
class ParamsAwareLocaleChangeInterceptor extends LocaleChangeInterceptor{

    String paramName =  DEFAULT_PARAM_NAME

    public void setParamName(String paramName) {
        this.paramName = paramName
        super.setParamName paramName
    }


    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()

        def params = webRequest.params

        def localeParam = params?.get(paramName)
        if(localeParam) {
            def localeResolver = RequestContextUtils.getLocaleResolver(request)
            def localeEditor = new LocaleEditor()
            localeEditor.setAsText localeParam
            localeResolver?.setLocale request, response, localeEditor.value
            return true
        }
        else {
            return super.preHandle(request, response, handler)
        }
    }


}