/*
 * Copyright 2014 the original author or authors.
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
package grails.artefact

import grails.util.Environment
import grails.web.util.GrailsApplicationAttributes
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.buffer.GrailsPrintWriter
import org.grails.web.encoder.OutputEncodingStack
import org.grails.web.encoder.WithCodecHelper
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.taglib.AbstractTemplateVariableBinding
import org.grails.web.taglib.TagLibraryLookup
import org.grails.web.taglib.TagOutput
import org.grails.web.taglib.TemplateVariableBinding
import org.grails.web.taglib.WebRequestTemplateVariableBinding
import org.grails.web.taglib.exceptions.GrailsTagException
import org.grails.web.taglib.util.TagLibraryMetaUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestAttributes

/**
 * @since 3.0
 * @author Jeff Brown
 *
 */
trait TagLibrary {
    
    /**
     * Throws a GrailsTagException
     *
     * @param message The error message
     */
    void throwTagError(String message) {
        throw new GrailsTagException(message)
    }

    /**
     * Obtains the page scope instance
     *
     * @return  The page scope instance
     */
    TemplateVariableBinding getPageScope() {
        GrailsWebRequest webRequest = getWebRequest()
        TemplateVariableBinding binding = (TemplateVariableBinding) webRequest.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE, RequestAttributes.SCOPE_REQUEST)
        if (binding == null) {
            binding = new TemplateVariableBinding(new WebRequestTemplateVariableBinding(webRequest))
            binding.root = true
            webRequest.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, binding, RequestAttributes.SCOPE_REQUEST)
        }
        binding
    }

    /**
     * Obtains the currently output writer

     * @return The writer to use
     */
    GrailsPrintWriter getOut() {
        OutputEncodingStack.currentStack().taglibWriter
    }

    /**
     * Sets the current output writer
     * @param newOut The new output writer
     */
    void setOut(Writer newOut) {
        OutputEncodingStack.currentStack().push(newOut,true)
    }
    
    def withCodec(Object codecInfo, Closure<?> body) {
        WithCodecHelper.withCodec(getGrailsApplication(null), codecInfo, body)
    }
    
    /**
     * Property missing implementation that looks up tag library namespaces or tags in the default namespace
     *
     * @param name The property name
     * @return A tag namespace or a tag in the default namespace
     *
     * @throws MissingPropertyException When no tag namespace or tag is found
     */
    public Object propertyMissing(String name) {
        TagLibraryLookup gspTagLibraryLookup = getTagLibraryLookup();
        if (gspTagLibraryLookup != null) {

            Object result = gspTagLibraryLookup.lookupNamespaceDispatcher(name);
            if (result == null) {
                String namespace = getNamespace();
                GroovyObject tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name);
                if (tagLibrary == null) {
                    tagLibrary = gspTagLibraryLookup.lookupTagLibrary(TagOutput.DEFAULT_NAMESPACE, name);
                }

                if (tagLibrary != null) {
                    Object tagProperty = tagLibrary.getProperty(name);
                    if (tagProperty instanceof Closure) {
                        result = ((Closure<?>)tagProperty).clone();
                    }
                }
            }

            if (result != null && !Environment.isDevelopmentMode()) {
                MetaClass mc = InvokerHelper.getMetaClass(this);
                TagLibraryMetaUtils.registerPropertyMissingForTag(mc, name, result);
            }

            if (result != null) {
                return result;
            }
        }

        throw new MissingPropertyException(name, this.getClass());
    }
    
    @Autowired
    void setGspTagLibraryLookup(TagLibraryLookup lookup) {
        tagLibraryLookup = lookup;
    }
}
