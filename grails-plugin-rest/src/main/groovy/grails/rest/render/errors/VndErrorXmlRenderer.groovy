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
import org.codehaus.groovy.grails.web.xml.PrettyPrintXMLStreamWriter
import org.codehaus.groovy.grails.web.xml.StreamingMarkupWriter
import org.codehaus.groovy.grails.web.xml.XMLStreamWriter
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
class VndErrorXmlRenderer extends AbstractVndErrorRenderer {
    public static final MimeType MIME_TYPE = new MimeType("application/vnd.error+xml", "xml")
    public static final String ERRORS_TAG = "errors"
    public static final String ERROR_TAG = "error"
    public static final String LINK_TAG = "link"

    MimeType[] mimeTypes = [MIME_TYPE, MimeType.HAL_XML, MimeType.XML, MimeType.TEXT_XML] as MimeType[]

    @Override
    void render(Errors object, RenderContext context) {
        if (object instanceof BeanPropertyBindingResult) {
            context.setContentType(mimeTypes[0].name)
            Locale locale = context.locale
            final target = object.target
            final language = locale.language

            final streamingWriter = new StreamingMarkupWriter(context.writer, encoding)
            XMLStreamWriter w = prettyPrint ? new PrettyPrintXMLStreamWriter(streamingWriter) : new XMLStreamWriter(streamingWriter)
            w.startDocument(encoding, "1.0")
            w.startNode(ERRORS_TAG)
                .attribute('xml:lang', language)
            for (ObjectError oe in object.allErrors) {
                def logref = resolveLogRef(target, oe)
                w.startNode(ERROR_TAG)
                    .attribute(LOGREF_ATTRIBUTE, logref)
                    .startNode(MESSAGE_ATTRIBUTE)
                        .characters(messageSource.getMessage(oe, locale))
                    .end()
                    .startNode(LINK_TAG)
                        .attribute("rel", "resource")
                        .attribute("href", linkGenerator.link(resource: target, method: "GET", absolute: true))
                    .end()
                .end()
            }
            w.end()
        }
    }
}
