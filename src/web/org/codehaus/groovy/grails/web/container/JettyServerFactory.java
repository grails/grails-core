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
package org.codehaus.groovy.grails.web.container;

import grails.web.container.EmbeddableServerFactory;
import grails.web.container.EmbeddableServer;

/**
 * A factory that creates Jetty servers
 *
 * @author Graeme Rocher
 * @since 1.1
 *
 *        <p/>
 *        Created: Jan 7, 2009
 */
public class JettyServerFactory implements EmbeddableServerFactory {


    public EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        return new JettyServer(basedir, webXml, contextPath, classLoader);
    }

    public EmbeddableServer createForWAR(String warPath, String contextPath) {
        return new JettyServer(warPath, contextPath);
    }
}
