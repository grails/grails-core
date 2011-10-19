/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.context;

import org.codehaus.groovy.grails.lifecycle.ShutdownOperations;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a reference to the ServletContext.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class ServletContextHolder {

    private static Map<Integer, ServletContext> servletContexts = new ConcurrentHashMap<Integer, ServletContext>();

    static {
        ShutdownOperations.addOperation(new Runnable() {
            public void run() {
                clearServletContext();
            }
        });
    }

    public static void clearServletContext() {
        servletContexts.clear();
    }

    /**
     * @return The ServletContext instance
     */
    @Deprecated
    public static ServletContext getServletContext() {
        int id = getClassLoaderId();
        ServletContext servletContext = servletContexts.get(id);
        if(servletContext == null) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            if(webRequest != null) {
                return webRequest.getServletContext();
            }
            else {
                int thisId = System.identityHashCode(ServletContextHolder.class.getClassLoader());
                return servletContexts.get(thisId);
            }
        }
        return servletContext;
    }

    /**
     * @param servletContext The ServletContext instance
     */
    public static void setServletContext(ServletContext servletContext) {
        int id = getClassLoaderId();
        int thisClassLoaderId = System.identityHashCode(ServletContextHolder.class.getClassLoader());
        if(servletContext != null) {
            servletContexts.put(id, servletContext);
            servletContexts.put(thisClassLoaderId, servletContext);
        }
        else {
            servletContexts.remove(id);
            servletContexts.remove(thisClassLoaderId);
        }
    }

    private static int getClassLoaderId() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return System.identityHashCode(contextClassLoader);
    }

}
