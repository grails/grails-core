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

import grails.converters.XML
import grails.rest.render.RenderContext
import grails.rest.render.util.AbstractLinkingRenderer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.xml.PrettyPrintXMLStreamWriter
import org.codehaus.groovy.grails.web.xml.StreamingMarkupWriter
import org.codehaus.groovy.grails.web.xml.XMLStreamWriter
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.http.HttpMethod

/**
 * Renders domain instances in HAL XML format (see http://stateless.co/hal_specification.html)
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class HalXmlRenderer<T> extends AbstractLinkingRenderer<T> {
    public static final MimeType MIME_TYPE = MimeType.HAL_XML
    public static final String RESOURCE_TAG = "resource"
    public static final String LINK_TAG = "link"
    public static final String RELATIONSHIP_ATTRIBUTE = "rel"

    private MimeType[] mimeTypes = [MIME_TYPE] as MimeType[]

    HalXmlRenderer(Class<T> targetType) {
        super(targetType)
    }

    @Override
    MimeType[] getMimeTypes() {
        return mimeTypes
    }

    @Override
    void render(T object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: mimeTypes[0]
        context.setContentType(mimeType.name)


        final streamingWriter = new StreamingMarkupWriter(context.writer, encoding)
        XMLStreamWriter w = prettyPrint ? new PrettyPrintXMLStreamWriter(streamingWriter) : new XMLStreamWriter(streamingWriter)
        XML xml = new XML(w)


        final entity = mappingContext.getPersistentEntity(object.class.name)
        boolean isDomain = entity != null

        Set writtenObjects = []
        w.startDocument(encoding, "1.0")

        if (isDomain) {
            writeDomainWithEmbeddedAndLinks(entity, object, context, xml, writtenObjects)
        }
        else if (object instanceof Collection) {
            final locale = context.locale
            String resourceHref = linkGenerator.link(uri: context.resourcePath, method: HttpMethod.GET, absolute: absoluteLinks)
            final title = getResourceTitle(context.resourcePath, locale)
            XMLStreamWriter writer = xml.getWriter()
            startResourceTag(writer, resourceHref, locale, title)
            for(o in ((Collection)object)) {
                final currentEntity = mappingContext.getPersistentEntity(o.class.name)
                if (currentEntity) {
                    writeDomainWithEmbeddedAndLinks(currentEntity, o, context, xml, writtenObjects)
                }
            }
            writer.end()
        }


    }

    protected void writeDomainWithEmbeddedAndLinks(PersistentEntity entity, object, RenderContext context, XML xml, Set writtenObjects) {
        final locale = context.locale
        String resourceHref = linkGenerator.link(resource: object, method: HttpMethod.GET, absolute: absoluteLinks)
        final title = getLinkTitle(entity, locale)
        XMLStreamWriter writer = xml.getWriter()
        startResourceTag(writer, resourceHref, locale, title)
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        final associationMap = writeAssociationLinks(object, locale, xml, entity, metaClass)
        writeDomain(metaClass, entity, object, xml)

        if (associationMap) {
            for (entry in associationMap.entrySet()) {
                final property = entry.key
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
                            writeDomainWithEmbeddedAndLinks(associatedEntity, value, context, xml, writtenObjects)
                        }
                    }
                } else {
                    final associatedEntity = property.associatedEntity
                    if (associatedEntity) {
                        for (obj in entry.value) {
                            writtenObjects << obj
                            writeDomainWithEmbeddedAndLinks(associatedEntity, obj, context, xml, writtenObjects)
                        }
                    }
                }

            }
        }
        writer.end()
    }

    protected void startResourceTag(XMLStreamWriter writer, String resourceHref, Locale locale, String title) {
        writer.startNode(RESOURCE_TAG)
            .attribute(HREF_ATTRIBUTE, resourceHref)
            .attribute(HREFLANG_ATTRIBUTE, locale.language)

        if (title) {
            writer.attribute(TITLE_ATTRIBUTE, title)
        }
    }

    void writeLink(String rel, String title, String href, Locale locale, String contentType, writerObject) {
        XMLStreamWriter writer = ((XML) writerObject).getWriter()
        writer.startNode(LINK_TAG)
            .attribute(RELATIONSHIP_ATTRIBUTE, rel)
            .attribute(HREF_ATTRIBUTE, href)
            .attribute(HREFLANG_ATTRIBUTE, locale.language)

        if (title) {
            writer.attribute(TITLE_ATTRIBUTE, title)
        }
        if (contentType) {
            writer.attribute(TYPE_ATTRIBUTE, contentType)
        }

        writer.end()
    }

    @Override
    protected void writeDomainProperty(value, String propertyName, writerObject) {
        final xml = (XML) writerObject
        XMLStreamWriter writer = xml.getWriter()

        writer.startNode(propertyName)
        xml.convertAnother(value)
        writer.end()
    }
}
