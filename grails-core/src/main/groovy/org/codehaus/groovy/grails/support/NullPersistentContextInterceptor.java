/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.support;

/**
 * A dummy persistence context interceptor that does nothing.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 */
public class NullPersistentContextInterceptor implements PersistenceContextInterceptor {
    public void init() {
        // NOOP
    }

    public void destroy() {
        // NOOP
    }

    public void disconnect() {
        // NOOP
    }

    public void reconnect() {
        // NOOP
    }

    public void flush() {
        // NOOP
    }

    public void clear() {
        // NOOP
    }

    public void setReadOnly() {
        // NOOP
    }

    public void setReadWrite() {
        // NOOP
    }

    public boolean isOpen() {
        return false;
    }
}
