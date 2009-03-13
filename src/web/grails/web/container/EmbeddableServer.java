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
 * An interface used to define the container implementation used by Grails during development.
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 7, 2009
 */
public interface EmbeddableServer {

    int DEFAULT_SECURE_PORT = 8443;
    int DEFAULT_PORT = 8080;
    String DEFAULT_HOST = "localhost";

    /**
     * Starts the container on the default port
     */
    void start();

    /**
     * Starts the container on the given port
     * @param port The port number
     */
    void start(int port);

    /**
     * Starts the container on the given port
     * @param host The host to start on
     * @param port The port number
     */
    void start(String host, int port);    

    /**
     * Starts a secure container running over HTTPS
     */
    void startSecure();

    /**
     * Starts a secure container running over HTTPS for the given port
     * @param port The port
     */
    void startSecure(int port);
    /**
     * Starts a secure container running over HTTPS for the given port and host.
     * @param host The server host
     * @param httpPort The port for HTTP traffic.
     * @param httpsPort The port for HTTPS traffic.
     */
    void startSecure(String host, int httpPort, int httpsPort);


    /**
     * Stops the container
     */
    void stop();

    /**
     * Typically combines the stop() and start() methods in order to restart the container
     */
    void restart();
}
