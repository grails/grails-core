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

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import grails.rest.render.ContainerRenderer
import grails.rest.render.RenderContext
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.http.HttpMethod
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

/**
 * A JSON renderer that renders errors in in the Vnd.Error format (see https://github.com/blongden/vnd.error)
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class VndErrorJsonRenderer implements ContainerRenderer<Errors, Object> {
    public static final MimeType MIME_TYPE = new MimeType("application/vnd.error+json", "json")
    public static final String LOGREF_ATTRIBUTE = 'logref'
    public static final String MESSAGE_ATTRIBUTE = "message"
    public static final String LINKS_ATTRIBUTE = "_links"
    public static final String RESOURCE_ATTRIBUTE = "resource"
    public static final String HREF_ATTRIBUTE = "href"

    boolean absoluteLinks = true

    @Autowired
    MessageSource messageSource

    @Autowired
    LinkGenerator linkGenerator

    @Autowired(required = false)
    Gson gson = new Gson()

    @Override
    Class<Errors> getTargetType() {
        Errors
    }

    @Override
    MimeType[] getMimeTypes() {
        return [MIME_TYPE, MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }

    @Override
    void render(Errors object, RenderContext context) {
        if (messageSource == null) throw new IllegalStateException("messageSource property null")
        if (object instanceof BeanPropertyBindingResult) {

            context.setContentType(MIME_TYPE.name)
            Locale locale = context.locale
            final target = object.target


            JsonWriter writer = new JsonWriter(context.writer)
            writer.beginArray()
            for(ObjectError oe in object.allErrors) {
                final msg = messageSource.getMessage(oe, locale)
                writer

                writer
                    .beginObject()
                      .name(LOGREF_ATTRIBUTE).value(gson.toJson(getObjectId(target)))
                      .name(MESSAGE_ATTRIBUTE).value(msg)
                      .name(LINKS_ATTRIBUTE)
                         .beginObject()
                             .name(RESOURCE_ATTRIBUTE)
                             .beginObject()
                                .name(HREF_ATTRIBUTE).value(linkGenerator.link(resource: target, method:HttpMethod.GET, absolute: absoluteLinks))
                             .endObject()
                         .endObject()
                      .endObject()
            }
            writer.endArray()

        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Object getObjectId(target) {
        target.id
    }


    @Override
    Class<Object> getComponentType() {
        Object
    }
}
