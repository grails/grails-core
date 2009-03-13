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
package grails.web.container;

/**
 * An interface for server vendors to implement in order to provide the ability to embed a container within
 * Grails' run-app command. 
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 7, 2009
 */
public interface EmbeddableServerFactory {


    /**
     * Creates an inline server for the the given base directory, web.xml file and class loader
     *
     * @param basedir The base directory
     * @param webXml The web.xml location
     * @param classLoader The class loader to use
     * @param contextPath The context path of the application
     * 
     * @return The EmbeddableServer instance
     */
    EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader);

    /**
     * Creates a EmbeddableServer instance for the given WAR file and context path
     *
     * @param warPath The path to the WAR
     * @param contextPath The context path
     * @return The EmbeddableServer instance
     */
    EmbeddableServer createForWAR(String warPath, String contextPath);
}
