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
import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter
import grails.rest.Resource
import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import groovy.transform.CompileStatic
import org.apache.commons.beanutils.PropertyUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.http.HttpMethod

/**
 * Renders domain instances in HAL JSON format (see http://tools.ietf.org/html/draft-kelly-json-hal-05)
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class HalDomainClassJsonRenderer<T> implements Renderer<T> {
    public static final MimeType MIME_TYPE = new MimeType("application/hal+json", "json")
    public static final String LINKS_ATTRIBUTE = "_links"
    public static final String SELF_ATTRIBUTE = "self"
    public static final String EMBEDDED_ATTRIBUTE = "_embedded"
    public static final String HREF_ATTRIBUTE = "href"
    public static final String TITLE_ATTRIBUTE = "title"
    public static final String HREFLANG_ATTRIBUTE = "hreflang"

    final Class<T> targetType
    private MimeType[] mimeTypes = [MIME_TYPE] as MimeType[]

    Gson gson = new Gson()

    List<String> includes
    List<String> excludes = [GrailsDomainClassProperty.ATTACHED, GrailsDomainClassProperty.VERSION, GrailsDomainClassProperty.ERRORS, "metaClass", "class", "properties"]

    @Autowired
    MessageSource messageSource

    @Autowired
    LinkGenerator linkGenerator

    @Autowired
    MappingContext mappingContext

    @Autowired(required = false)
    ProxyHandler proxyHandler = new DefaultProxyHandler()

    HalDomainClassJsonRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    HalDomainClassJsonRenderer(Class<T> targetType, MimeType... mimeTypes) {
        this.targetType = targetType
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


        final clazz = object.class

        if (DomainClassArtefactHandler.isDomainClass(clazz) && clazz.getAnnotation(Resource)) {
            writeDomainWithEmbeddedAndLinks(clazz, object, writer, context.locale)
        }
        else if (object instanceof Collection) {

        }
    }

    protected void writeDomainWithEmbeddedAndLinks(Class<T> clazz, T object, JsonWriter writer, Locale locale) {

        PersistentEntity entity = mappingContext.getPersistentEntity(clazz.name)
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        writer.beginObject()
        writer.name(LINKS_ATTRIBUTE)

        final entityHref = linkGenerator.link(resource: object, method: HttpMethod.GET.toString())
        final title = getLinkTitle(entity, locale)
        writer.beginObject()
        writeLink(SELF_ATTRIBUTE,title,entityHref, locale, writer)


        Map<Association, Object> associationMap = [:]
        for(Association a in entity.associations) {
            final propertyName = a.name
            final associatedEntity = a.associatedEntity
            if (!associatedEntity) continue
            if (proxyHandler.isInitialized(object, propertyName)) {
                if (a instanceof ToOne) {
                    final value = proxyHandler.unwrapIfProxy(metaClass.getProperty(object, propertyName))
                    if (a instanceof Embedded) {
                        // no links for embedded
                        associationMap[a] = value
                    }
                    else if (value != null) {
                        final href = linkGenerator.link(resource: value,method: HttpMethod.GET)
                        final associationTitle = getLinkTitle(associatedEntity, locale)
                        writeLink(propertyName, associationTitle, href, locale,writer)
                        associationMap[a] = value
                    }
                }
                else {
                    // TODO: handle one-to-many
                }

            }  else if ((a instanceof ToOne) && (proxyHandler instanceof EntityProxyHandler)) {
                if (associatedEntity) {
                    final proxy = PropertyUtils.getProperty(object, propertyName)
                    final id = proxyHandler.getProxyIdentifier(proxy)
                    final href = linkGenerator.link(resource: associatedEntity.decapitalizedName, id: id, method: HttpMethod.GET)
                    final associationTitle = getLinkTitle(associatedEntity, locale)
                    writeLink(propertyName, associationTitle, href, locale,writer)
                }

            }
        }
        writer.endObject()

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
                    if (value != null) {
                        final associatedEntity = ((Association) property).associatedEntity
                        if (associatedEntity) {
                            writeDomainWithEmbeddedAndLinks(associatedEntity.javaClass, (T)value, writer, locale)
                        }
                    }
                } else {
                    // TODO: Handle one-to-many

                }

            }
            writer.endObject()
        }
        writer.endObject()
    }

    protected String getLinkTitle(PersistentEntity entity, Locale locale) {
        final propertyName = entity.decapitalizedName
        messageSource.getMessage("resource.${propertyName}.href.title", [propertyName, entity.name] as Object[], "", locale)
    }

    protected void writeLink(String propertyName, String title, String href, Locale locale, JsonWriter writer) {
        writer.name(propertyName)
              .beginObject()
              .name(HREF_ATTRIBUTE).value(href)
              .name(HREFLANG_ATTRIBUTE).value(locale.language)
        if (title) {
            writer.name(TITLE_ATTRIBUTE).value(title)
        }
        writer.endObject()

    }

    /**
     * Writes a domain instance
     *
     * @param clazz The class
     * @param object The object
     * @param writer The writer
     * @return Any associations embedded within the object
     */
    protected void writeDomain(MetaClass metaClass, PersistentEntity entity, Object object, JsonWriter writer) {

        if (entity) {
            for (PersistentProperty p in entity.persistentProperties) {
                if (includes != null && !includes.contains(p.name)) continue;
                final propertyName = p.name
                if (!(p instanceof Association)) {
                    final value = metaClass.getProperty(object,propertyName)
                    if (value != null) {
                        writer.name(propertyName).value(gson.toJson(value))
                    }
                }
            }
        }
    }
}
