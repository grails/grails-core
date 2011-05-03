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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * An isolated version of Tomcat used to run Grails applications with run-war
 *
 * @author Graeme Rocher
 * @since 1.4
 */

public class IsolatedTomcat {

	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {

		if(args.length < 3) {
			System.err.println("Usage: IsolatedTomcat [tomcat_path] [war_path] [context_path] [host] [port]");
			System.exit(1);
		}
		else {
			String tomcatDir = args[0];
			String warPath = args[1];
			String contextPath = args[2];
			String host = "localhost";
			if(args.length>3) host = args[3];
			int port = 8080;
			try {
				if(args.length>4) port = Integer.parseInt(args[4]);
			} catch (NumberFormatException e) {
				// ignore
			}

			final Tomcat tomcat = new Tomcat();
			tomcat.setPort(port);

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
			connector.setAttribute("address", host);
			connector.setURIEncoding("UTF-8");

			final int serverPort = port;
			new Thread(new Runnable() {
				public void run() {
					int killListenerPort = serverPort + 1;
					ServerSocket serverSocket;

					serverSocket= createKillSwitch(killListenerPort);

					if(serverSocket!=null) {
						try {
							serverSocket.accept();
							try {
								tomcat.stop();
							} catch (LifecycleException e) {
								System.err.println("Error stopping Tomcat: " + e.getMessage());
								System.exit(1);
							}
						} catch (IOException e) {
							// just exit
						}
					}

				}
			}).start();

			try {
				tomcat.start();
				String message = "Server running. Browse to http://"+(host != null ? host : "localhost")+":"+port+contextPath;
				System.out.println(message);
			} catch (LifecycleException e) {
				e.printStackTrace();
				System.err.println("Error loading Tomcat: " + e.getMessage());
				System.exit(1);
			}



		}
	}

	private static ServerSocket createKillSwitch(int killListenerPort) {
		try {
			return new ServerSocket(killListenerPort);
		} catch (IOException e) {
			return null;
		}
	}

}
