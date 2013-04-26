package org.codehaus.groovy.grails.web.taglib

import grails.util.Environment;
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.env.GrailsEnvironment;
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup

@CompileStatic
class TemplateNamespacedTagDispatcher extends NamespacedTagDispatcher {
    protected GroovyObject renderTagLib
    
    private boolean developmentMode=Environment.current.isDevelopmentMode()

    TemplateNamespacedTagDispatcher(Class callingType, GrailsApplication application, TagLibraryLookup lookup) {
        super(GroovyPage.DEFAULT_NAMESPACE, callingType, application, lookup)
        renderTagLib = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, 'render')
    }

    def methodMissing(String name, Object args) {
        List<MetaMethod> methods=renderTagLib.getMetaClass().respondsTo(renderTagLib, 'render', (Object[])args)
        if(methods) {
            MetaMethod method = methods.first()
            synchronized(this) {
                if(developmentMode) {
                    ((GroovyObject)getMetaClass()).setProperty(name, { Object[] varArgs ->
                        renderTagLib.invokeMethod("render", argsToAttrs(name, varArgs))
                    })
                } else {
                    ((GroovyObject)getMetaClass()).setProperty(name, { Object[] varArgs ->
                        method.invoke(renderTagLib, argsToAttrs(name, varArgs))
                    })
                }
            }
            return method.invoke(renderTagLib, argsToAttrs(name, args))
        }

        throw new MissingMethodException(name, type, args)
    }

    protected Map argsToAttrs(String name, Object args) {
        Map<String, Object> attr = [:]
        attr.template = name
        if (args instanceof Object[]) {
            Object[] tagArgs = ((Object[])args)
            if (tagArgs.length > 0 && tagArgs[0] instanceof Map) {
                Map<String, Object> modelMap = (Map<String, Object>)tagArgs[0]
                Object encodeAs = modelMap.remove(GroovyPage.ENCODE_AS_ATTRIBUTE_NAME)
                if(encodeAs != null) {
                    attr.put(GroovyPage.ENCODE_AS_ATTRIBUTE_NAME, encodeAs);
                }
                attr.put("model", modelMap)
            }
        }
        attr
    }
}
