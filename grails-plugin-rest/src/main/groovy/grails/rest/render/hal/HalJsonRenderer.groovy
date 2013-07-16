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
import grails.rest.render.util.AbstractLinkingRenderer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.binding.bindingsource.DataBindingSourceRegistry
import org.codehaus.groovy.grails.web.binding.bindingsource.HalJsonDataBindingSourceCreator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod

import javax.annotation.PostConstruct

/**
 * Renders domain instances in HAL JSON format (see http://tools.ietf.org/html/draft-kelly-json-hal-05)
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class HalJsonRenderer<T> extends AbstractLinkingRenderer<T> {
    public static final MimeType MIME_TYPE = MimeType.HAL_JSON
    public static final String LINKS_ATTRIBUTE = "_links"
    public static final String EMBEDDED_ATTRIBUTE = "_embedded"

    private static final MimeType[] DEFAULT_MIME_TYPES = [MIME_TYPE] as MimeType[]


    private Gson gson = new Gson()

    HalJsonRenderer(Class<T> targetType) {
        super(targetType, DEFAULT_MIME_TYPES)
    }

    HalJsonRenderer(Class<T> targetType, MimeType... mimeTypes) {
        super(targetType, mimeTypes)
    }

    @Autowired(required = false)
    void setGson(Gson gson) {
        this.gson = gson
    }

    @Autowired(required = false)
    DataBindingSourceRegistry dataBindingSourceRegistry

    @PostConstruct
    void initialize() {
        if (dataBindingSourceRegistry != null) {
            final thisType = getTargetType()
            final thisMimeTypes = getMimeTypes()
            final halDataBindingSourceCreator = new HalJsonDataBindingSourceCreator() {
                @Override
                Class getTargetType() {
                    thisType
                }

                @Override
                MimeType[] getMimeTypes() {
                    thisMimeTypes
                }
            }
            halDataBindingSourceCreator.gson = gson
            dataBindingSourceRegistry.addDataBindingSourceCreator(halDataBindingSourceCreator)
        }
    }

    @Override
    void renderInternal(T object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: mimeTypes[0]
        final responseWriter = context.writer
        JsonWriter writer = new JsonWriter(responseWriter)

        try {
            if (prettyPrint)
                writer.setIndent('  ')

            final clazz = object.class

            if (isDomainResource(clazz)) {
                writeDomainWithEmbeddedAndLinks(context, clazz, object, writer, context.locale, mimeType, [] as Set)
            } else if (object instanceof Collection) {
                beginLinks(writer)
                writeLinkForCurrentPath(context, mimeType, writer)
                writer.endObject()
                      .name(EMBEDDED_ATTRIBUTE)
                      .beginArray()

                final writtenObjects = [] as Set
                for(o in ((Collection)object)) {
                    if (o && isDomainResource(o.getClass())) {
                        writeDomainWithEmbeddedAndLinks(context, o.class, o, writer, context.locale, mimeType, writtenObjects)
                    }
                }
                writer.endArray()

            } else {
                beginLinks(writer)
                writeLinkForCurrentPath(context, mimeType, writer)
                writeExtraLinks(object, context.locale, writer)
                writer.endObject()
                final bean = PropertyAccessorFactory.forBeanPropertyAccess(object)
                final propertyDescriptors = bean.propertyDescriptors
                for(pd in propertyDescriptors) {
                    final propertyName = pd.name
                    if (DEFAULT_EXCLUDES.contains(propertyName)) continue
                    if (shouldIncludeProperty(context,object, propertyName)) {
                        if (pd.readMethod && pd.writeMethod) {
                            writer.name(propertyName).value(gson.toJson(bean.getPropertyValue(propertyName)))
                        }
                    }
                }
                writer.endObject()

            }
        } finally {
            writer.flush()
        }

    }

    protected void writeLinkForCurrentPath(RenderContext context, MimeType mimeType, JsonWriter writer) {
        final href = linkGenerator.link(uri: context.resourcePath, method: HttpMethod.GET.toString(), absolute: absoluteLinks)
        final resourceRef = href
        final locale = context.locale
        def link = new Link(RELATIONSHIP_SELF, href)
        link.title = getResourceTitle(resourceRef, locale)
        link.contentType = mimeType ? mimeType.name : null

        writeLink(link, locale, writer)
    }

    protected void beginLinks(JsonWriter writer) {
        writer.beginObject()
            .name(LINKS_ATTRIBUTE)
            .beginObject()
    }


    protected void writeDomainWithEmbeddedAndLinks( RenderContext context, Class clazz, Object object, JsonWriter writer, Locale locale, MimeType contentType, Set writtenObjects) {

        PersistentEntity entity = mappingContext.getPersistentEntity(clazz.name)
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        Map<Association, Object> associationMap = writeLinks(context,metaClass, object, entity, locale, contentType, writer)

        writeDomain(context, metaClass, entity, object, writer)

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
                            writeDomainWithEmbeddedAndLinks(context, associatedEntity.javaClass, value, writer, locale, null, writtenObjects)
                        }
                    }
                } else {
                    final associatedEntity = property.associatedEntity
                    if (associatedEntity) {
                        writer.beginArray()
                        for (obj in entry.value) {
                            writtenObjects << obj
                            writeDomainWithEmbeddedAndLinks(context, associatedEntity.javaClass, obj, writer, locale,null, writtenObjects)
                        }
                        writer.endArray()
                    }
                }

            }
            writer.endObject()
        }
        writer.endObject()
    }

    protected Map<Association, Object> writeLinks( RenderContext context, MetaClass metaClass, object, PersistentEntity entity, Locale locale, MimeType contentType, JsonWriter writer) {
        writer.beginObject()
        writer.name(LINKS_ATTRIBUTE)
        writer.beginObject()
        final entityHref = linkGenerator.link(resource: object, method: HttpMethod.GET.toString(), absolute: absoluteLinks)
        final title = getLinkTitle(entity, locale)


        def link = new Link(RELATIONSHIP_SELF, entityHref)
        link.contentType = contentType ? contentType.name : null
        link.title = title
        link.hreflang = locale
        writeLink(link, locale, writer)
        Map<Association, Object> associationMap = writeAssociationLinks(context,object, locale, writer, entity, metaClass)
        writer.endObject()
        associationMap
    }

    protected void writeLink(Link link, Locale locale, writerObject) {
        JsonWriter writer = (JsonWriter)writerObject
        writer.name(link.rel)
            .beginObject()
            .name(HREF_ATTRIBUTE).value(link.href)
            .name(HREFLANG_ATTRIBUTE).value((link.hreflang ?: locale).language)
        final title = link.title
        if (title) {
            writer.name(TITLE_ATTRIBUTE).value(title)
        }
        final type = link.contentType
        if (type) {
            writer.name(TYPE_ATTRIBUTE).value(type)
        }
        if (link.templated) {
            writer.name(TEMPLATED_ATTRIBUTE).value(true)
        }
        if (link.deprecated) {
            writer.name(DEPRECATED_ATTRIBUTE).value(true)
        }
        writer.endObject()

    }

    protected void writeDomainProperty(value, String propertyName, writer) {
        ((JsonWriter)writer).name(propertyName).value(gson.toJson(value))
    }
}
