/*
 * Copyright 2014 original authors
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
package grails.artefact.gsp

import grails.util.Environment
import grails.util.GrailsMetaClassUtils
import grails.web.api.WebAttributes
import groovy.transform.CompileStatic
import org.grails.gsp.GroovyPage
import org.grails.web.encoder.WithCodecHelper
import org.grails.web.taglib.NamespacedTagDispatcher
import org.grails.web.taglib.TagLibraryLookup
import org.grails.web.taglib.util.TagLibraryMetaUtils
import org.springframework.beans.factory.annotation.Autowired

/**
 * A trait that adds the ability invoke tags to any class
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait TagLibraryInvoker extends WebAttributes{

    @Autowired
    TagLibraryLookup tagLibraryLookup

    private boolean developmentMode = Environment.isDevelopmentMode();


    /**
     * Method missing implementation that handles tag invocation by method name
     *
     * @param instance The instance
     * @param methodName The method name
     * @param argsObject The arguments
     * @return The result
     */
    Object methodMissing(String methodName, Object argsObject) {
        Object[] args = argsObject instanceof Object[] ? (Object[])argsObject : [argsObject] as Object[]
        if (shouldHandleMethodMissing(methodName, args)) {
            TagLibraryLookup lookup = tagLibraryLookup
            if (lookup) {
                GroovyObject tagLibrary = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, methodName)
                if (tagLibrary) {
                    if (!developmentMode) {
                        MetaClass controllerMc = GrailsMetaClassUtils.getMetaClass(this)
                        TagLibraryMetaUtils.registerMethodMissingForTags(controllerMc, lookup, GroovyPage.DEFAULT_NAMESPACE, methodName)
                    }
                    return tagLibrary.invokeMethod(methodName, args)
                }
            }
        }
        throw new MissingMethodException(methodName, this.getClass(), args)
    }

    /**
     * Looks up namespaces on missing property
     *
     * @param instance The instance
     * @param propertyName The property name
     * @return The namespace or a MissingPropertyException
     */
     Object propertyMissing(String propertyName) {
        TagLibraryLookup lookup = tagLibraryLookup
        NamespacedTagDispatcher namespacedTagDispatcher = lookup.lookupNamespaceDispatcher(propertyName)
        if (namespacedTagDispatcher) {
            if (!developmentMode) {
                TagLibraryMetaUtils.registerPropertyMissingForTag(GrailsMetaClassUtils.getMetaClass(this),propertyName, namespacedTagDispatcher)
            }
            return namespacedTagDispatcher
        }

        throw new MissingPropertyException(propertyName, this.getClass())
    }

    def <T> T withCodec(Object instance, Object codecInfo, Closure<T> body) {
        return WithCodecHelper.withCodec(getGrailsApplication(), codecInfo, body)
    }

    private boolean shouldHandleMethodMissing(String methodName, Object[] args) {
        return !"render".equals(methodName);
    }

}