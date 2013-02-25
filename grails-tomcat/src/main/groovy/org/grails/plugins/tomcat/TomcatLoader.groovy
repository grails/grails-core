/*
 * Copyright 2011 SpringSource
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
package org.grails.plugins.tomcat

import java.beans.PropertyChangeListener

import org.apache.catalina.Container
import org.apache.catalina.Lifecycle
import org.apache.catalina.LifecycleState
import org.apache.catalina.Loader
import org.apache.catalina.util.LifecycleBase
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.naming.resources.DirContextURLStreamHandler
import org.apache.naming.resources.DirContextURLStreamHandlerFactory

/**
 * A loader instance used for the embedded version of Tomcat 7.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class TomcatLoader extends LifecycleBase implements Loader {

    private static Log log = LogFactory.getLog(TomcatLoader.name)

    private static boolean first = true

    ClassLoader classLoader
    Container container
    boolean delegate
    boolean reloadable

    TomcatLoader(ClassLoader classLoader) {
        // Class loader that only searches the parent
        this.classLoader = new ParentDelegatingClassLoader(classLoader)
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {}

    void addRepository(String repository) {
        log.warn "Call to addRepository($repository) was ignored."
    }

    void backgroundProcess() {}

    String[] findRepositories() {
        log.warn "Call to findRepositories() returned null."
    }

    String getInfo() { "MyLoader/1.0" }

    boolean modified() { false }

    void removePropertyChangeListener(PropertyChangeListener listener) {}

    @Override protected void initInternal() {
        URLStreamHandlerFactory streamHandlerFactory = new DirContextURLStreamHandlerFactory()

        if (first) {
            first = false
            try {
                URL.setURLStreamHandlerFactory(streamHandlerFactory)
            } catch (Exception e) {
                // Log and continue anyway, this is not critical
                log.error("Error registering jndi stream handler", e)
            } catch (Throwable t) {
                // This is likely a dual registration
                log.info("Dual registration of jndi stream handler: " + t.getMessage())
            }
        }

        DirContextURLStreamHandler.bind(classLoader, container.getResources())
    }

    @Override protected void destroyInternal() {
        classLoader = null
    }

    @Override protected void startInternal() {
        fireLifecycleEvent(Lifecycle.START_EVENT, this)
        setState(LifecycleState.STARTING)
    }

    @Override protected void stopInternal() {
        fireLifecycleEvent(Lifecycle.STOP_EVENT, this)
        setState(LifecycleState.STOPPING)
    }
}
