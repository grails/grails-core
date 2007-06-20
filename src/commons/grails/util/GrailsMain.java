/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.util;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * A main class for Grails that launches a jetty instance and runs the app
 * specified by the basedir argument
 * 
 * @author Graeme Rocher
 * @since 09-May-2006
 */
public class GrailsMain {
    private static final String TMP_WAR_LOCATION = "web-app";

    /**
     * The main routine that loads a jetty instance and launches the Grails
     * application for the specified basedir/port etc.
     * 
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
        Server server = new Server();
        try {
            Connector[] connectors = new Connector[] { new SelectChannelConnector() };
            String port = System.getProperty("server.port");
            if (StringUtils.isBlank(port)) {
                connectors[0].setPort(8080);
            } else {
                connectors[0].setPort(Integer.parseInt(port));
            }
            String basedir = System.getProperty("base.dir");
            server.setConnectors(connectors);
            String name;
            String location;
            if (StringUtils.isBlank(basedir)) {
                File current = new File(".");
                name = '/' + current.getParentFile().getName();
                location = GrailsMain.TMP_WAR_LOCATION;
            } else {
                File base = new File(basedir);
                name = '/' + base.getName();
                location = basedir + File.separator + TMP_WAR_LOCATION;
            }
            ContextHandler handler = new WebAppContext(location, name);
            handler.setClassLoader(Thread.currentThread()
                    .getContextClassLoader());
            server.setHandler(handler);
            System.out.println("Starting Grails Jetty server for location: "
                    + location);
            server.start();
            System.out.println("Grails Jetty Server Started...");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
