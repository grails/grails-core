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
package grails.rest.render.util

import grails.rest.Link
import grails.rest.Resource
import grails.rest.render.Renderer
import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.commons.beanutils.PropertyUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.http.HttpMethod

/**
 * Abstract base class for HAL renderers
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
abstract class AbstractLinkingRenderer<T> implements Renderer<T> {

    public static final String RELATIONSHIP_SELF = "self"
    public static final String HREF_ATTRIBUTE = "href"
    public static final String TITLE_ATTRIBUTE = "title"
    public static final String HREFLANG_ATTRIBUTE = "hreflang"
    public static final String TYPE_ATTRIBUTE = "type"

    @Autowired
    MessageSource messageSource

    @Autowired
    LinkGenerator linkGenerator

    @Autowired
    MappingContext mappingContext

    @Autowired(required = false)
    ProxyHandler proxyHandler = new DefaultProxyHandler()

    final Class<T> targetType

    boolean prettyPrint = Environment.isDevelopmentMode()
    boolean absoluteLinks = true
    List<String> includes
    String encoding = "UTF-8"

    AbstractLinkingRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    protected boolean isDomainResource(Class clazz) {
        DomainClassArtefactHandler.isDomainClass(clazz)
    }

    protected String getLinkTitle(PersistentEntity entity, Locale locale) {
        final propertyName = entity.decapitalizedName
        messageSource.getMessage("resource.${propertyName}.href.title", [propertyName, entity.name] as Object[], "", locale)
    }

    protected String getResourceTitle(String uri, Locale locale) {
        if (uri.startsWith('/')) uri = uri.substring(1)
        if (uri.endsWith('/')) uri = uri.substring(0, uri.length()-1)
        uri = uri.replace('/', '.')
        messageSource.getMessage("resource.${uri}.href.title", [uri] as Object[], "", locale)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<Link> getLinksForObject(def object) {
        if (object.respondsTo('links')) {
            return object.links()
        }
        return Collections.emptyList()
    }

    protected Map<Association, Object> writeAssociationLinks(object, Locale locale, writer, PersistentEntity entity, MetaClass metaClass) {
        final extraLinks = getLinksForObject(object)
        for (Link l in extraLinks) {
            writeLink(l.rel, l.title, l.href, l.hreflang ?: locale, l.contentType, writer)
        }


        Map<Association, Object> associationMap = [:]
        for (Association a in entity.associations) {
            final propertyName = a.name
            if (includes != null && !includes.contains(a.name)) {
                continue
            }
            final associatedEntity = a.associatedEntity
            if (!associatedEntity) {
                continue
            }
            if (proxyHandler.isInitialized(object, propertyName)) {
                if (a instanceof ToOne) {
                    final value = proxyHandler.unwrapIfProxy(metaClass.getProperty(object, propertyName))
                    if (a instanceof Embedded) {
                        // no links for embedded
                        associationMap[a] = value
                    } else if (value != null) {
                        final href = linkGenerator.link(resource: value, method: HttpMethod.GET, absolute: absoluteLinks)
                        final associationTitle = getLinkTitle(associatedEntity, locale)
                        writeLink(propertyName, associationTitle, href, locale, null, writer)
                        associationMap[a] = value
                    }
                } else if (!(a instanceof Basic)) {
                    associationMap[a] = metaClass.getProperty(object, propertyName)
                }

            } else if ((a instanceof ToOne) && (proxyHandler instanceof EntityProxyHandler)) {
                if (associatedEntity) {
                    final proxy = PropertyUtils.getProperty(object, propertyName)
                    final id = proxyHandler.getProxyIdentifier(proxy)
                    final href = linkGenerator.link(resource: associatedEntity.decapitalizedName, id: id, method: HttpMethod.GET, absolute: absoluteLinks)
                    final associationTitle = getLinkTitle(associatedEntity, locale)
                    writeLink(propertyName, associationTitle, href, locale, null, writer)
                }
            }
        }
        associationMap
    }

    /**
     * Writes a domain instance
     *
     * @param clazz The class
     * @param object The object
     * @param writer The writer
     * @return Any associations embedded within the object
     */
    protected void writeDomain(MetaClass metaClass, PersistentEntity entity, Object object, writer) {

        if (entity) {
            for (PersistentProperty p in entity.persistentProperties) {
                if (includes != null && !includes.contains(p.name)) {
                    continue
                }
                final propertyName = p.name
                if ((p instanceof Basic) || !(p instanceof Association)) {
                    final value = metaClass.getProperty(object, propertyName)
                    if (value != null) {
                        writeDomainProperty(value, propertyName, writer)
                    }
                }
            }
        }
    }

    protected abstract void writeLink(String rel, String title, String href, Locale locale, String type, writerObject)
    protected abstract  void writeDomainProperty(value, String propertyName, writer)
}
