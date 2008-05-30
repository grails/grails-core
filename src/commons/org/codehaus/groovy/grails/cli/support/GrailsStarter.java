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
package org.codehaus.groovy.grails.cli.support;

import org.codehaus.groovy.tools.LoaderConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Nov 29, 2007
 */
public class GrailsStarter {
    static void printUsage() {
        System.out.println("possible programs are 'groovyc','groovy','console', and 'groovysh'");
        System.exit(1);
    }


    public static void rootLoader(String args[]) {
        String conf = System.getProperty("groovy.starter.conf",null);
        LoaderConfiguration lc = new LoaderConfiguration();

        // evaluate parameters
        boolean hadMain=false, hadConf=false, hadCP=false;
        int argsOffset = 0;
        while (args.length-argsOffset>0 && !(hadMain && hadConf && hadCP)) {
            if (args[argsOffset].equals("--classpath")) {
                if (hadCP) break;
                if (args.length==argsOffset+1) {
                    exit("classpath parameter needs argument");
                }
                lc.addClassPath(args[argsOffset+1]);
                argsOffset+=2;
            } else if (args[argsOffset].equals("--main")) {
                if (hadMain) break;
                if (args.length==argsOffset+1) {
                    exit("main parameter needs argument");
                }
                lc.setMainClass(args[argsOffset+1]);
                argsOffset+=2;
            } else if (args[argsOffset].equals("--conf")) {
                if (hadConf) break;
                if (args.length==argsOffset+1) {
                    exit("conf parameter needs argument");
                }
                conf=args[argsOffset+1];
                argsOffset+=2;
            } else {
                break;
            }
        }

        // we need to know the class we want to start
        if (lc.getMainClass()==null && conf==null) {
            exit("no configuration file or main class specified");
        }

        // copy arguments for main class
        String[] newArgs = new String[args.length-argsOffset];
        for (int i=0; i<newArgs.length; i++) {
            newArgs[i] = args[i+argsOffset];
        }
        // load configuration file
        if (conf!=null) {
            try {
                lc.configure(new FileInputStream(conf));
            } catch (Exception e) {
                System.err.println("exception while configuring main class loader:");
                exit(e);
            }
        }
        // create loader and execute main class
        GrailsRootLoader loader = new GrailsRootLoader(lc);

        String javaVersion = System.getProperty("java.version");
        String grailsHome = System.getProperty("grails.home");

        if(javaVersion != null && grailsHome != null) {
            javaVersion = javaVersion.substring(0,3);
            File vmConfig = new File(grailsHome +"/conf/groovy-starter-java-"+javaVersion+".conf");
            if(vmConfig.exists()) {
                InputStream in = null;
                try {
                    in = new FileInputStream(vmConfig);
                    LoaderConfiguration vmLoaderConfig = new LoaderConfiguration();
                    vmLoaderConfig.setRequireMain(false);
                    vmLoaderConfig.configure(in);
                    URL[] vmSpecificClassPath = vmLoaderConfig.getClassPathUrls();
                    for (int i = 0; i < vmSpecificClassPath.length; i++) {
                        URL url = vmSpecificClassPath[i];
                        loader.addURL(url);
                    }
                } catch (IOException e) {
                    System.out.println("WARNING: I/O error reading VM specific classpath ["+vmConfig+"]: " + e.getMessage() );
                }
                finally {
                    try {
                        if(in != null) in.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

        }

        Method m=null;
        try {
            Class c = loader.loadClass(lc.getMainClass());
            m = c.getMethod("main", new Class[]{String[].class});
        } catch (ClassNotFoundException e1) {
            exit(e1);
        } catch (SecurityException e2) {
            exit(e2);
        } catch (NoSuchMethodException e2) {
            exit(e2);
        }
        try {
            m.invoke(null, new Object[]{newArgs});
        } catch (IllegalArgumentException e3) {
            exit(e3);
        } catch (IllegalAccessException e3) {
            exit(e3);
        } catch (InvocationTargetException e3) {
            exit(e3);
        }
    }

    private static void exit(Exception e) {
        e.printStackTrace();
        System.exit(1);
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    // after migration from classworlds to the rootloader rename
    // the rootLoader method to main and remove this method as
    // well as the classworlds method
    public static void main(String args[]) {
        try {
            rootLoader(args);
        } catch (Throwable t) {
            System.out.println("Error starting Grails: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}

