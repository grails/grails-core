package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup

class TemplateNamespacedTagDispatcher extends NamespacedTagDispatcher {
    protected GroovyObject renderTagLib

    TemplateNamespacedTagDispatcher(Class callingType, GrailsApplication application, TagLibraryLookup lookup) {
        super(GroovyPage.DEFAULT_NAMESPACE, callingType, application, lookup)
        renderTagLib = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, 'render')
    }

    def methodMissing(String name, args) {
        if (renderTagLib.respondsTo('render', args)) {
            MetaMethod method=renderTagLib.metaClass.getMetaMethod('render', args)
            synchronized(this) {
                metaClass."$name" = { Object[] varArgs ->
                    renderTagLib.render(argsToAttrs(name, varArgs))
               }
            }
            return method.invoke(renderTagLib, argsToAttrs(name, args))
        }

        throw new MissingMethodException(name, type, args)
    }

    protected Map argsToAttrs(String name, Object args) {
        def attr = [template: name]
        if (args instanceof Object[]) {
            Object[] tagArgs = ((Object[])args)
            if (tagArgs.length > 0 && tagArgs[0] instanceof Map) {
                attr.put("model", tagArgs[0])
            }
        }
        attr
    }
}
