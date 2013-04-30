/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.GroovyObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.Assert;

/**
 * Provides services for resolving URIs.
 *
 * Caches lookups in an internal ConcurrentMap cache.
 *
 * @author Lari Hotari , Sagire Software Oy
 */
public class DefaultGroovyPagesUriService extends GroovyPagesUriSupport {

    ConcurrentMap<TupleStringKey, String> templateURICache = new ConcurrentHashMap<TupleStringKey, String>();
    ConcurrentMap<TupleStringKey, String> deployedViewURICache = new ConcurrentHashMap<TupleStringKey, String>();
    ConcurrentMap<ControllerObjectKey, String> controllerNameCache = new ConcurrentHashMap<ControllerObjectKey, String>();
    ConcurrentMap<TupleStringKey, String> noSuffixViewURICache = new ConcurrentHashMap<TupleStringKey, String>();

    private static class TupleStringKey {
        String keyPart1;
        String keyPart2;

        public TupleStringKey(String keyPart1, String keyPart2) {
            this.keyPart1 = keyPart1;
            this.keyPart2 = keyPart2;
        }

        @Override
        public boolean equals(final Object that) {
            if (this == that) {
                return true;
            }

            if (that == null) {
                return false;
            }

            if (getClass() != that.getClass()) {
                return false;
            }

            TupleStringKey thatKey=(TupleStringKey)that;
            return new EqualsBuilder().append(keyPart1, thatKey.keyPart1)
                                      .append(keyPart2, thatKey.keyPart2)
                                      .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(keyPart1).append(keyPart2).toHashCode();
        }
    }

    private static class ControllerObjectKey {
        private long controllerHashCode;
        private String controllerClassName;

        public ControllerObjectKey(GroovyObject controller) {
            controllerHashCode = System.identityHashCode(controller.getClass());
            controllerClassName = controller.getClass().getName();
        }

        @Override
        public boolean equals(final Object that) {
            if (this == that) {
                return true;
            }
            if (that == null) {
                return false;
            }
            if (getClass() != that.getClass()) {
                return false;
            }
            ControllerObjectKey thatKey=(ControllerObjectKey)that;
            return new EqualsBuilder().append(controllerHashCode, thatKey.controllerHashCode)
                                      .append(controllerClassName, thatKey.controllerClassName)
                                      .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(controllerHashCode)
                                        .append(controllerClassName)
                                        .toHashCode();
        }
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getTemplateURI(java.lang.String, java.lang.String)
     */
    @Override
    public String getTemplateURI(String controllerName, String templateName) {
        TupleStringKey key = new TupleStringKey(controllerName, templateName);
        String uri = templateURICache.get(key);
        if (uri == null) {
            uri = super.getTemplateURI(controllerName, templateName);
            String prevuri=templateURICache.putIfAbsent(key, uri);
            if (prevuri != null) {
                return prevuri;
            }
        }
        return uri;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getDeployedViewURI(java.lang.String, java.lang.String)
     */
    @Override
    public String getDeployedViewURI(String controllerName, String viewName) {
        TupleStringKey key = new TupleStringKey(controllerName, viewName);
        String uri = deployedViewURICache.get(key);
        if (uri == null) {
            uri = super.getDeployedViewURI(controllerName, viewName);
            String prevuri = deployedViewURICache.putIfAbsent(key, uri);
            if (prevuri != null) {
                return prevuri;
            }
        }
        return uri;
    }

    @Override
    public String getLogicalControllerName(GroovyObject controller) {
        ControllerObjectKey key = new ControllerObjectKey(controller);
        String name = controllerNameCache.get(key);
        if (name == null) {
            name = super.getLogicalControllerName(controller);
            String prevname = controllerNameCache.putIfAbsent(key, name);
            if (prevname != null) {
                return prevname;
            }
        }
        return name;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getNoSuffixViewURI(groovy.lang.GroovyObject, java.lang.String)
     */
    @Override
    public String getNoSuffixViewURI(GroovyObject controller, String viewName) {
        Assert.notNull(controller, "Argument [controller] cannot be null");
        return getNoSuffixViewURI(getLogicalControllerName(controller),viewName);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getNoSuffixViewURI(java.lang.String, java.lang.String)
     */
    @Override
    public String getNoSuffixViewURI(String controllerName, String viewName) {
        TupleStringKey key = new TupleStringKey(controllerName, viewName);
        String uri = noSuffixViewURICache.get(key);
        if (uri == null) {
            uri = super.getNoSuffixViewURI(controllerName, viewName);
            String prevuri = noSuffixViewURICache.putIfAbsent(key, uri);
            if (prevuri != null) {
                return prevuri;
            }
        }
        return uri;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getTemplateURI(groovy.lang.GroovyObject, java.lang.String)
     */
    @Override
    public String getTemplateURI(GroovyObject controller, String templateName) {
        return getTemplateURI(getLogicalControllerName(controller), templateName);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#clear()
     */
    @Override
    public void clear() {
        templateURICache.clear();
        deployedViewURICache.clear();
        controllerNameCache.clear();
        noSuffixViewURICache.clear();
    }
}
