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
package grails.rest.render.hal

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import grails.rest.Link
import grails.rest.render.RenderContext
import groovy.transform.CompileStatic
import org.apache.commons.beanutils.PropertyUtils
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod

/**
 * Renders domain instances in HAL JSON format (see http://tools.ietf.org/html/draft-kelly-json-hal-05)
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class HalDomainClassJsonRenderer<T> extends AbstractHalRenderer<T> {
    public static final String LINKS_ATTRIBUTE = "_links"
    public static final String EMBEDDED_ATTRIBUTE = "_embedded"

    private MimeType[] mimeTypes = [MIME_TYPE] as MimeType[]

    @Autowired(required = false)
    Gson gson = new Gson()

    HalDomainClassJsonRenderer(Class<T> targetType) {
        super(targetType)
    }

    HalDomainClassJsonRenderer(Class<T> targetType, MimeType... mimeTypes) {
        super(targetType)
        this.mimeTypes = mimeTypes
    }

    @Override
    MimeType[] getMimeTypes() {
        return mimeTypes
    }

    @Override
    void render(T object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: mimeTypes[0]
        context.setContentType(mimeType.name)

        JsonWriter writer = new JsonWriter(context.writer)

        if(prettyPrint)
            writer.setIndent('  ')

        final clazz = object.class

        if (isDomainResource(clazz)) {
            writeDomainWithEmbeddedAndLinks(clazz, object, writer, context.locale, mimeType, [] as Set)
        } else if (object instanceof Collection) {
            writer.beginObject()
                  .name(LINKS_ATTRIBUTE)
                  .beginObject()
            final resourceRef = linkGenerator.link(uri:context.resourcePath, method: HttpMethod.GET.toString(), absolute: absoluteLinks)
            final locale = context.locale
            writeLink(RELATIONSHIP_SELF, getResourceTitle(resourceRef, locale),resourceRef, locale, mimeType ? mimeType.name : null, writer)
            writer.endObject()
                  .name(EMBEDDED_ATTRIBUTE)
                  .beginArray()

            for(o in ((Collection)object)) {
                if (o && isDomainResource(o.getClass())) {
                    writeDomainWithEmbeddedAndLinks(o.class, o, writer, locale, mimeType, [] as Set)
                }
            }
            writer.endArray()

        }
    }


    protected void writeDomainWithEmbeddedAndLinks(Class clazz, Object object, JsonWriter writer, Locale locale, MimeType contentType, Set writtenObjects) {

        PersistentEntity entity = mappingContext.getPersistentEntity(clazz.name)
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        Map<Association, Object> associationMap = writeLinks(metaClass, object, entity, locale, contentType, writer)

        writeDomain(metaClass, entity, object, writer)

        if (associationMap) {
            writer.name(EMBEDDED_ATTRIBUTE)
            writer.beginObject()
            for (entry in associationMap.entrySet()) {
                final property = entry.key
                writer.name(property.name)
                final isSingleEnded = property instanceof ToOne
                if (isSingleEnded) {
                    Object value = entry.value
                    if (writtenObjects.contains(value)) {
                        continue
                    }

                    if (value != null) {
                        final associatedEntity = property.associatedEntity
                        if (associatedEntity) {
                            writtenObjects << value
                            writeDomainWithEmbeddedAndLinks(associatedEntity.javaClass, value, writer, locale, null, writtenObjects)
                        }
                    }
                } else {
                    final associatedEntity = property.associatedEntity
                    if(associatedEntity) {
                        writer.beginArray()
                        for (obj in entry.value) {
                            writtenObjects << obj
                            writeDomainWithEmbeddedAndLinks(associatedEntity.javaClass, obj, writer, locale,null, writtenObjects)
                        }
                        writer.endArray()
                    }
                }

            }
            writer.endObject()
        }
        writer.endObject()
    }

    protected Map<Association, Object> writeLinks(MetaClass metaClass, object, PersistentEntity entity, Locale locale, MimeType contentType, JsonWriter writer) {
        writer.beginObject()
        writer.name(LINKS_ATTRIBUTE)
        writer.beginObject()
        final entityHref = linkGenerator.link(resource: object, method: HttpMethod.GET.toString(), absolute: absoluteLinks)
        final title = getLinkTitle(entity, locale)


        writeLink(RELATIONSHIP_SELF, title, entityHref, locale, contentType ? contentType.name : null, writer)
        Map<Association, Object> associationMap = writeAssociationLinks(object, locale, writer, entity, metaClass)
        writer.endObject()
        associationMap
    }



    protected void writeLink(String rel, String title, String href, Locale locale, String type, writerObject) {
        JsonWriter writer = (JsonWriter)writerObject
        writer.name(rel)
            .beginObject()
            .name(HREF_ATTRIBUTE).value(href)
            .name(HREFLANG_ATTRIBUTE).value(locale.language)
        if (title) {
            writer.name(TITLE_ATTRIBUTE).value(title)
        }
        if (type) {
            writer.name(TYPE_ATTRIBUTE).value(type)
        }
        writer.endObject()

    }



    protected void writeDomainProperty(value, String propertyName, writer) {
        ((JsonWriter)writer).name(propertyName).value(gson.toJson(value))
    }
}
