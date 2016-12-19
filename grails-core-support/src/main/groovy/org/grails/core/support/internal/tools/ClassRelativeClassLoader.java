package org.grails.core.support.internal.tools;

import grails.io.IOUtils;
import grails.util.BuildSettings;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * A classloader that only finds resources and classes that are in the same jar as the given class
 *
 * For internal use only
 *
 * @author Graeme Rocher
 * @since 3.1.13
 */
class ClassRelativeClassLoader extends URLClassLoader {
    public ClassRelativeClassLoader(Class targetClass) {
        super(createClassLoaderUrls(targetClass), ClassLoader.getSystemClassLoader());
    }

    private static URL[] createClassLoaderUrls(Class targetClass) {
        URL root = IOUtils.findRootResource(targetClass);
        if(BuildSettings.RESOURCES_DIR != null && BuildSettings.RESOURCES_DIR.exists()) {
            try {
                return new URL[] {root, new FileSystemResource(BuildSettings.RESOURCES_DIR.getCanonicalFile()).getURL() };
            } catch (IOException e) {
                return new URL[]{root};
            }
        }
        else {
            return new URL[]{root};
        }
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if("".equals(name)) {
            final URL[] urls = getURLs();
            final int l = urls.length;
            return new Enumeration<URL>() {
                int i = 0;
                @Override
                public boolean hasMoreElements() {
                    return i < l;
                }

                @Override
                public URL nextElement() {
                    return urls[i++];
                }
            };
        }
        else {
            return findResources(name);
        }
    }


}
