package org.grails.plugins.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.net.ServerSocket;

/**
 *  Allows the tomcat server to be killed by pinging a port one greater than the server port
 *
 *  @author Graeme Rocher
 *  @since 2.1
 */
public class TomcatKillSwitch implements Runnable {
    
    private static boolean active = true;

    private Tomcat tomcat;
    private int serverPort;

    public TomcatKillSwitch(Tomcat tomcat, int serverPort) {
        this.tomcat = tomcat;
        this.serverPort = serverPort;
    }

    public static boolean isActive() {
        return active;
    }

    public void run() {
        active = true;
        int killListenerPort = serverPort + 1;
        ServerSocket serverSocket = createKillSwitch(killListenerPort);
        if (serverSocket != null) {
            try {
                serverSocket.accept();
                try {
                    tomcat.stop();
                    tomcat.destroy();
                    active = false;
                } catch (LifecycleException e) {
                    System.err.println("Error stopping Tomcat: " + e.getMessage());
                    System.exit(1);
                }
            } catch (IOException e) {
                // just exit
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
