/*
 * Copyright 2004-2006 Graeme Rocher
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
package grails.persistence.support;

/**
 * Sets up the persistent context before and after a Grails operation is invoked.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public interface PersistenceContextInterceptor {

    /**
     * Called to intialisation the persistent context.
     */
    void init();

    /**
     * Called to finalize the persistent context.
     */
    void destroy();

    /**
     * Disconnects the persistence context.
     */
    void disconnect();

    /**
     * Reconnects the persistence context.
     */
    void reconnect();

    /**
     *  Flushes any pending changes to the DB.
     */
    void flush();

    /**
     * Clear any pending changes.
     */
    void clear();

    /**
     * Sets the persistence context to read-only mode.
     */
    void setReadOnly();

    /**
     * Sets the persistence context to read-write mode.
     */
    void setReadWrite();

    /**
     * Checks whether the persistence context is open.
     * @return Returns whether the persistence context is open
     */
    boolean isOpen();
}
