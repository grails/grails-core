/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.support.proxy;

/**
 * Methods specified to proxied entities
 *
 * @author Graeme Rocher
 * @since 1.3.6
 */
public interface EntityProxyHandler extends ProxyHandler{

    /**
     * This method returns the identifier of the proxy or null if the
     * object is not a proxy
     *
     * @return The identifier of the identity
     */
    Object getProxyIdentifier(Object o);

    /**
     * Returns the proxied class without initializing the proxy
     *
     * @param o The object
     * @return The class
     */
    Class<?> getProxiedClass(Object o);
}
