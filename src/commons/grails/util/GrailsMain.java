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
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;



/**
 *  A main class for Grails that launches a jetty instance and runs the 
 *  app specified by the basedir argument
 *
 * @author Graeme Rocher
 * @since 09-May-2006
 */
public class GrailsMain {

	/**
	 * The main routine that loads a jetty instance and launches the Grails
	 * application for the specified basedir/port etc.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server server = new Server();

		SocketListener listener = new SocketListener();
		String port = System.getProperty("server.port");
		if(StringUtils.isBlank(port)) {
			listener.setPort(8080);
		}
		else {			
			listener.setPort(Integer.parseInt(port));
		}
		server.addListener(listener);
		
		try {
			String basedir = System.getProperty("base.dir");
			if(StringUtils.isBlank(basedir)) {
				File current = new File(".");				
				server.addWebApplication('/'+current.getParentFile().getName(),
										 "tmp/war");
			}
			else {
				File base = new File(basedir);
				
				server.addWebApplication('/'+base.getName(), 
										basedir+"/tmp/war");				
			}
			server.start();
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
