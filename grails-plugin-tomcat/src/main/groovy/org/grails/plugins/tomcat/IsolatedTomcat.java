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
package org.grails.plugins.tomcat;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;

/**
 * An isolated version of Tomcat used to run Grails applications with run-war.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class IsolatedTomcat {

    /**
     * @param args Command line arguments
     */
    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("Usage: IsolatedTomcat [tomcat_path] [war_path] [context_path] [host] [httpPort] [httpsPort] [keystorePath] [keystorePassword]");
            System.exit(1);
        }

        String tomcatDir = args[0];
        String warPath = args[1];
        String contextPath = args[2];
        String host = "localhost";
        if (args.length>3) host = args[3];
        int port = argToNumber(args, 4, 8080);
        int httpsPort = argToNumber(args, 5, 0);

        String keystorePath = "";
        String keystorePassword = "";
        if (httpsPort >= 0) {
            keystorePath = args[6];
            keystorePassword = args[7];
        }

        final Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);

        if (Boolean.getBoolean("tomcat.nio")) {
            System.out.println("Enabling Tomcat NIO Connector");
            Connector connector = new Connector(Http11NioProtocol.class.getName());
            connector.setPort(port);
            tomcat.getService().addConnector(connector);
            tomcat.setConnector(connector);
        }

        tomcat.setBaseDir(tomcatDir);
        try {
            tomcat.addWebapp(contextPath, warPath);
        } catch (ServletException e) {
            e.printStackTrace();
            System.err.println("Error loading Tomcat: " + e.getMessage());
            System.exit(1);
        }
        tomcat.enableNaming();

        final Connector connector = tomcat.getConnector();

        // Only bind to host name if we aren't using the default
        if (!host.equals("localhost")) {
            connector.setAttribute("address", host);
        }

        connector.setURIEncoding("UTF-8");

        if (httpsPort >= 0) {
            Connector sslConnector;
            try {
                sslConnector = new Connector();
            } catch (Exception e) {
                throw new RuntimeException("Couldn't create HTTPS connector", e);
            }

            sslConnector.setScheme("https");
            sslConnector.setSecure(true);
            sslConnector.setPort(httpsPort);
            sslConnector.setProperty("SSLEnabled", "true");
            sslConnector.setAttribute("keystoreFile", keystorePath);
            sslConnector.setAttribute("keystorePass", keystorePassword);
            sslConnector.setURIEncoding("UTF-8");

            if (!host.equals("localhost")) {
                sslConnector.setAttribute("address", host);
            }

            tomcat.getService().addConnector(sslConnector);
        }

        try {
            tomcat.start();
            final int serverPort = tomcat.getConnector().getLocalPort();
            startKillSwitch(tomcat, serverPort);
            String message = "Server running. Browse to http://"+(host != null ? host : "localhost")+":"+serverPort+contextPath+getSslMessage(tomcat, host, contextPath);
            System.out.println(message);
        } catch (LifecycleException e) {
            e.printStackTrace(System.err);
            System.err.println("Error loading Tomcat: " + e.getMessage());
            System.exit(1);
        }
    }

    public static String getSslMessage(Tomcat tomcat, String host, String contextPath) {
        Connector[] connectors = tomcat.getService().findConnectors();
        Connector sslConnector = null;
        for(Connector c : connectors) {
            if(c.getScheme().equals("https")) {
                sslConnector = c;
                break;
            }
        }
        if (sslConnector != null) {
            return " or https://"+(host != null ? host : "localhost")+":"+sslConnector.getLocalPort()+contextPath;
        } else {
            return "";
        }
    }

    public static void startKillSwitch(final Tomcat tomcat, final int serverPort) {
        new Thread(new TomcatKillSwitch(tomcat, serverPort)).start();
    }

    private static int argToNumber(String[] args, int i, int orDefault) {
        if (args.length > i) {
            try {
                return Integer.parseInt(args[i]);
            } catch (NumberFormatException e) {
                return orDefault;
            }
        }
        return orDefault;
    }
}
