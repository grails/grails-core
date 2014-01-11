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
import org.grails.datastore.mapping.model.MappingFactory
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod

import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter
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

    private static class UTCDateConverter implements Converter<Date, String> {
        private final static TimeZone UtcTZ = TimeZone.getTimeZone('UTC')
        @Override
        String convert(Date source) {
            final GregorianCalendar cal = new GregorianCalendar()
            cal.setTime(source)
            cal.setTimeZone(UtcTZ)
            DatatypeConverter.printDateTime(cal)
        }
    }

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

    Converter<Date, String> dateToStringConverter = new UTCDateConverter()

    @Autowired(required = false)
    void setDateToStringConverter(Converter<Date, String> converter) {
        this.dateToStringConverter = converter
    }

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
                writeDomainWithEmbeddedAndLinks(context, clazz, object, writer, context.locale, mimeType, [] as Set,
                        new Stack())
            } else if (object instanceof Collection) {
                beginLinks(writer)
                writeLinkForCurrentPath(context, mimeType, writer)
                writer.endObject()
                writer.name(EMBEDDED_ATTRIBUTE)
                renderEmbeddedAttributes(writer, object, context, mimeType)
                writer.endObject()
            } else {
                writeSimpleObjectAndLink(object, context, writer, mimeType)
            }
        } finally {
            writer.flush()
        }

    }

    protected renderEmbeddedAttributes(JsonWriter writer, object, RenderContext context, MimeType mimeType) {
        writer.beginArray()

        final writtenObjects = [] as Set
        for(o in ((Collection)object)) {
            if (o) {
                if(isDomainResource(o.getClass())) {
                    writeDomainWithEmbeddedAndLinks(context, o.class, o, writer, context.locale, mimeType, writtenObjects
                            , new Stack())
                } else {
                    writeSimpleObjectAndLink(o, context, writer, mimeType)
                }
            }
        }
        writer.endArray()
    }

    protected writeSimpleObjectAndLink(Object o, RenderContext context, JsonWriter writer, MimeType mimeType) {
        beginLinks(writer)
        writeLinkForCurrentPath(context, mimeType, writer)
        writeExtraLinks(o, context.locale, writer)
        writer.endObject()
        writeSimpleObject(o, context, writer)
    }
    
    protected void writeSimpleObject(Object object, RenderContext context, JsonWriter writer) {
        final bean = PropertyAccessorFactory.forBeanPropertyAccess(object)
        final propertyDescriptors = bean.propertyDescriptors
        for (pd in propertyDescriptors) {
            final propertyName = pd.name
            if (DEFAULT_EXCLUDES.contains(propertyName)) continue
            if (shouldIncludeProperty(context, object, propertyName)) {
                if (pd.readMethod && pd.writeMethod) {
                    final value = bean.getPropertyValue(propertyName)
                    if (value instanceof Number) {
                        writer.name(propertyName).value((Number) value)
                    }
                    else if (value instanceof CharSequence || value instanceof Enum) {
                        writer.name(propertyName).value(value.toString())
                    }
                    else {
                        if (MappingFactory.isSimpleType(pd.getPropertyType().getName())) {
                            writer.name(propertyName).value(gson.toJson(value))
                        }
                        else {
                            writer.name(propertyName)
                                .beginObject()
                                writeSimpleObject(value, context, writer)
                        }
                    }
                }
            }
        }
        writer.endObject()
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

    protected void writeDomainWithEmbeddedAndLinks( RenderContext context, Class clazz, Object object, JsonWriter writer, Locale locale, MimeType contentType, Set writtenObjects,
                                                    Stack referenceStack) {

        PersistentEntity entity = mappingContext.getPersistentEntity(clazz.name)
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        //If the object was already serialized , simply write its link for it and return.
        if (referenceStack.contains(object)) {
            writeLinks(context,metaClass, object, entity, locale, contentType, writer, false)
            writer.endObject()
            return
        }
        //Push the current object to referenceStack for  handling circular references. Once all its fields are handled,
        //the object is removed from the stack.
        referenceStack.push object
        Map<Association, Object> associationMap = writeLinks(context,metaClass, object, entity, locale, contentType, writer, true)

        writeDomain(context, metaClass, entity, object, writer)


        if (associationMap) {
            writer.name(EMBEDDED_ATTRIBUTE)
            writer.beginObject()
            for (entry in associationMap.entrySet()) {
                final property = entry.key
                final isSingleEnded = property instanceof ToOne
                writer.name(property.name)

                if (isSingleEnded) {
                    Object value = entry.value
                    if (value != null) {
                        final associatedEntity = property.associatedEntity
                        if (associatedEntity) {
                            writtenObjects << value
                            writeDomainWithEmbeddedAndLinks(context, associatedEntity.javaClass, value, writer, locale, null, writtenObjects,
                                    referenceStack)
                        }
                    }
                } else {
                    final associatedEntity = property.associatedEntity
                    if (associatedEntity) {
                        writer.beginArray()
                        for (obj in entry.value) {
                            writtenObjects << obj
                            writeDomainWithEmbeddedAndLinks(context, associatedEntity.javaClass, obj, writer, locale,null, writtenObjects,
                                    referenceStack)
                        }
                        writer.endArray()
                    }
                }

            }
            writer.endObject()
        }
        referenceStack.pop()
        writer.endObject()
    }

    protected Map<Association, Object> writeLinks( RenderContext context, MetaClass metaClass, object, PersistentEntity entity, Locale locale, MimeType contentType, JsonWriter writer,
                                                   boolean associationLinks = true) {
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
        Map<Association, Object> associationMap = associationLinks ?
            writeAssociationLinks(context,object, locale, writer, entity, metaClass) : [:] as Map<Association,Object>
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
        final jsonWriter = (JsonWriter) writer
        if(value instanceof Number) {
            jsonWriter.name(propertyName).value((Number)value)
        }
        else if(value instanceof CharSequence || value instanceof Enum) {
            jsonWriter.name(propertyName).value(value.toString())
        }
        else if(value instanceof Date) {
            final asStringDate = dateToStringConverter.convert((Date)value)
            jsonWriter.name(propertyName).value(asStringDate)
        }
        else {
            jsonWriter.name(propertyName)
            gson.toJson(value, value.class, jsonWriter)
        }
    }
}
