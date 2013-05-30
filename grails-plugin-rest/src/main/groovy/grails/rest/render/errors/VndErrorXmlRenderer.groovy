/*
 * Copyright 2013 the original author or authors.
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

package grails.rest.render.errors

import grails.rest.render.ContainerRenderer
import grails.rest.render.RenderContext
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

/**
 * A renderer that renders errors in in the Vnd.Error format (see https://github.com/blongden/vnd.error)
 *
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class VndErrorXmlRenderer implements ContainerRenderer<Errors, Object>{
    public static final String CONTENT_TYPE = "application/vnd.error+xml"

    @Autowired
    MessageSource messageSource

    @Autowired
    LinkGenerator linkGenerator

    @Override
    Class<Errors> getTargetType() {
        Errors
    }

    @Override
    MimeType[] getMimeTypes() {
        return [MimeType.XML, MimeType.TEXT_XML] as MimeType[]
    }

    @Override
    void render(Errors object, RenderContext context) {
        if (object instanceof BeanPropertyBindingResult) {

            context.setContentType(CONTENT_TYPE)
            def mkp = new MarkupBuilder(context.getWriter())
            Locale locale = context.locale
            final target = object.target
            final language = locale.language

            writeXmlResponse(object, mkp, language, locale, target)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void writeXmlResponse(BeanPropertyBindingResult object, MarkupBuilder mkp, String language, Locale locale, target) {
        final id = target.id
        mkp.errors('xml:lang': language) {
            for (ObjectError oe in object.allErrors) {
                error(logref: id) {
                    message messageSource.getMessage(oe, locale)
                    link rel: "resource", href: linkGenerator.link(resource: target, method:"GET", absolute: true)
                }
            }
        }
    }

    @Override
    Class<Object> getComponentType() {
        Object
    }
}
