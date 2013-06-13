/*
 * Copyright 2012 the original author or authors.
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
package org.grails.wrapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * GrailsWrapper class for wrapper
 *
 * @author Jeff Brown
 * @since 2.1
 */
public class GrailsWrapper {

    public static void main(final String[] args) throws Exception{
        final ResourceBundle applicationBundle = ResourceBundle.getBundle("application");
        final ResourceBundle wrapperBundle = ResourceBundle.getBundle("grails-wrapper");
        final String grailsVersion = applicationBundle.getString("app.grails.version");
        String distUrl = wrapperBundle.getString("wrapper.dist.url");
        if (distUrl == null) {
            distUrl = "http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/";
        }
        if (!distUrl.endsWith("/")) {
            distUrl += "/";
        }

        addSystemProperties(wrapperBundle);

        final File grailsHome = configureGrailsInstallation(distUrl, grailsVersion);

        System.setProperty("grails.home", grailsHome.getAbsolutePath());

        final List<String> newArgsList = new ArrayList<String>();
        for(int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if ("--main".equals(arg) && i < args.length - 1) {
                // skip --main and the following argument
                i++;
            } else if ("--conf".equals(arg) && i < args.length - 1) {
                newArgsList.add(arg);
                final File groovyStarterConf = new File(grailsHome, "conf/groovy-starter.conf");
                newArgsList.add(groovyStarterConf.getAbsolutePath());
                i++;
            } else {
                newArgsList.add(arg);
            }
        }

        final String[] newArgsArray = newArgsList.toArray(new String[0]);
        final URL[] urls = new URL[2];
        urls[0] = new File(grailsHome, "dist/grails-bootstrap-" + grailsVersion + ".jar").toURI().toURL();
        final File directoryToSearchForGroovyAllJar = new File(grailsHome, "/lib/org.codehaus.groovy");
        final File groovyJar = findGroovyAllJar(directoryToSearchForGroovyAllJar);
        if (groovyJar == null) {
            System.err.println("An error occurred locating the groovy jar under " + directoryToSearchForGroovyAllJar.getAbsolutePath());
            System.exit(-1);
        }
        final URI groovyJarUri = groovyJar.toURI();
        final URL groovyJarUrl = groovyJarUri.toURL();
        urls[1] = groovyJarUrl;
        final URLClassLoader urlClassLoader = new URLClassLoader(urls);
        final Class<?> loadClass = urlClassLoader.loadClass("org.codehaus.groovy.grails.cli.support.GrailsStarter");
        final Method mainMethod = loadClass.getMethod("main", String[].class);

        mainMethod.invoke(null, new Object[]{newArgsArray});
    }

    private static void addSystemProperties(ResourceBundle wrapperBundle) {
        String prefix = "systemProp.";
        for (String key : wrapperBundle.keySet()) {
            if (key.startsWith(prefix)) {
                String systemKey = key.substring(prefix.length());
                System.getProperties().put(systemKey, wrapperBundle.getString(key));
            }
        }
    }

    private static File findGroovyAllJar(final File directoryToSearch) {
        final File[] files = directoryToSearch.listFiles();
        for(File file : files) {
            if (file.isDirectory()) {
                return findGroovyAllJar(file);
            }
            final String fileName = file.getName();
            if (fileName.matches("groovy-all-(\\d+)(\\.\\d+)*\\.jar")) {
                return file;
            }
        }
        return null;
    }

    /**
     * @param distUrl URL to directory where the distribution zip is found
     * @param grailsVersion version of Grails to configure
     * @return a File pointing to the directory where this version of Grails is configured
     */
    private static File configureGrailsInstallation(String distUrl,
            final String grailsVersion) throws Exception {
        final String src = distUrl + "grails-" + grailsVersion + ".zip";
        final URI uri = new URI(src);

        final File grailsCacheDir =  new File(System.getProperty("user.home") + "/.grails/");
        final File wrapperDir = new File(grailsCacheDir, "wrapper");
        final File downloadFile = new File(wrapperDir, "grails-" + grailsVersion + "-download.zip");
        new RemoteFileHelper().retrieve(uri, downloadFile);
        final File installDir = new File(wrapperDir, grailsVersion);
        if (!installDir.exists()) {
            extract(downloadFile, installDir);
        }
        final File grailsHome = new File(installDir, "grails-" + grailsVersion);
        return grailsHome;
    }

    public static void extract(final File zip, final File dest) throws IOException {
        System.out.println("Extracting " + zip.getAbsolutePath() + " to " + dest.getAbsolutePath());
        Enumeration<?> entries;
        final ZipFile zipFile = new ZipFile(zip);

        entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry) entries.nextElement();

            if (entry.isDirectory()) {
                (new File(dest, entry.getName())).mkdirs();
                continue;
            }

            copy(zipFile.getInputStream(entry),
                    new BufferedOutputStream(new FileOutputStream(new File(dest, entry.getName()))));
        }
        zipFile.close();
    }

    public static void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }
}
