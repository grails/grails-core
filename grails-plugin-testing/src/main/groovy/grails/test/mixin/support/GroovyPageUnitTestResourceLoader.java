package grails.test.mixin.support;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link org.springframework.core.io.ResourceLoader} implementation
 * that loads GSP views relative to the project base directory for unit tests
 *
 * @since 1.4
 * @author Graeme Rocher
 */
public class GroovyPageUnitTestResourceLoader extends DefaultResourceLoader{

    public static final String WEB_INF_PREFIX = "/WEB-INF/grails-app/views";
    private Map<String,String> groovyPages = new ConcurrentHashMap<String, String>();

    public GroovyPageUnitTestResourceLoader(Map<String, String> groovyPages) {
        this.groovyPages = groovyPages;
    }

    @Override
    public Resource getResource(String location) {

        if (location.startsWith(WEB_INF_PREFIX)) {
            location = location.substring(WEB_INF_PREFIX.length());
        }
        if (groovyPages.containsKey(location)) {
            try {
                return new ByteArrayResource(groovyPages.get(location).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // continue
            }
        }

        BuildSettings buildSettings = BuildSettingsHolder.getSettings();
        String systemBaseDirectory = System.getProperty("base.dir");
        String basedir = systemBaseDirectory != null ? systemBaseDirectory : ".";
        if (buildSettings != null) {
            try {
                basedir = buildSettings.getBaseDir().getCanonicalPath();
            } catch (IOException e) {
                // ignore
            }
        }

        String path = basedir + File.separatorChar + GrailsResourceUtils.VIEWS_DIR_PATH + location;
        path = makeCanonical(path);
        return new FileSystemResource(path);
    }

    private String makeCanonical(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }
}
