/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.GroovyPagesMetaUtils
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup

/**
 * Allows dispatching to namespaced tag libraries and is used within controllers and tag libraries
 * to allow namespaced tags to be invoked as methods (eg. g.link(action:'foo')).
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class NamespacedTagDispatcher extends GroovyObjectSupport {

    String namespace
    GrailsApplication application
    Class type
    TagLibraryLookup lookup

    NamespacedTagDispatcher(String ns, Class callingType, GrailsApplication application, TagLibraryLookup lookup) {
        this.namespace = ns
        this.application = application
        this.lookup = lookup
        this.type = callingType
        // use per-instance metaclass
        ExpandoMetaClass emc = new ExpandoMetaClass(this.getClass(), false, true)
        emc.initialize()
        this.metaClass = emc
        if (ns == GroovyPage.DEFAULT_NAMESPACE) {
            GroovyPagesMetaUtils.registerMethodMissingWorkaroundsForDefaultNamespace(emc, lookup)
        }
    }

    def methodMissing(String name, args) {
        GroovyObject tagBean = lookup.lookupTagLibrary(namespace, name)
        if (tagBean && tagBean.respondsTo(name, args)) {
            MetaMethod method=tagBean.metaClass.getMetaMethod(name, args)
            synchronized(this) {
                metaClass."$name" = { Object[] varArgs ->
                    tagBean."$name"(*varArgs)
               }
            }
            return method.invoke(tagBean, args)
        }

        throw new MissingMethodException(name, type, args)
    }
}
