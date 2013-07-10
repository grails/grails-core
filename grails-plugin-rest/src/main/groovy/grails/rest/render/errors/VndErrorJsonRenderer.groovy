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
import grails.rest.render.RenderContext
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
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
class VndErrorJsonRenderer extends AbstractVndErrorRenderer {
    public static final MimeType MIME_TYPE = new MimeType("application/vnd.error+json", "json")
    public static final String LINKS_ATTRIBUTE = "_links"
    public static final String FOUR_SPACES = '    '

    MimeType[] mimeTypes = [MIME_TYPE, MimeType.HAL_JSON, MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]

    @Autowired(required = false)
    Gson gson = new Gson()

    @Override
    void render(Errors object, RenderContext context) {
        if (messageSource == null) throw new IllegalStateException("messageSource property null")
        if (object instanceof BeanPropertyBindingResult) {

            context.setContentType(MIME_TYPE.name)
            Locale locale = context.locale
            final target = object.target


            JsonWriter writer = new JsonWriter(context.writer)
            if (prettyPrint) {
                writer.indent = FOUR_SPACES
            }
            writer.beginArray()
            for(ObjectError oe in object.allErrors) {
                final msg = messageSource.getMessage(oe, locale)
                writer

                String logref = resolveLogRef(target, oe)
                writer
                    .beginObject()
                      .name(LOGREF_ATTRIBUTE).value(gson.toJson(logref))
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

            writer.flush()

        }
    }
}
