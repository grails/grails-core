package org.grails.io.support;

import junit.framework.TestCase;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.io.support.Resource;
import org.grails.io.support.UrlResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class GrailsResourceUtilsTests extends TestCase {

    private static final String TEST_URL = "file:///test/grails/app/grails-app/domain/Test.groovy";
    private static final String TEST_PACKAGE_URL = "file:///test/grails/app/grails-app/domain/mycompany/Test.groovy";
    private static final String TEST_CONTROLLER_URL = "file:///test/grails/app/grails-app/controllers/TestController.groovy";
    private static final String TEST_PLUGIN_CTRL = "file:///test/grails/app/plugins/myplugin/grails-app/controllers/TestController.groovy";

    private static final String WEBINF_CONTROLLER = "file:///test/grails/app/WEB-INF/grails-app/controllers/TestController.groovy";
    private static final String WEBINF_PLUGIN_CTRL = "file:///test/grails/app/WEB-INF/plugins/myplugin/grails-app/controllers/TestController.groovy";

    private static final String UNIT_TESTS_URL = "file:///test/grails/app/grails-tests/SomeTests.groovy";

    public void testGetArtifactDirectory() {
        assertEquals("controllers", GrailsResourceUtils.getArtefactDirectory(TEST_CONTROLLER_URL));
        assertEquals("domain", GrailsResourceUtils.getArtefactDirectory(TEST_PACKAGE_URL));
    }

    public void testJavaAndGroovySources() {
        assertEquals("mycompany.Test", GrailsResourceUtils.getClassName(TEST_PACKAGE_URL));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/domain/mycompany/Test.java").getPath()));
        assertEquals("Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blahblah/Test.java").getPath()));
        assertEquals("Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah-blah/Test.java").getPath()));
        assertEquals("Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah--blah/Test.java").getPath()));
        assertEquals("Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah_blah/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blahblah/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah-blah/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah--blah/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah_blah/mycompany/Test.java").getPath()));

        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/groovy/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/groovy/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/java/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/java/mycompany/Test.java").getPath()));

        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/groovy/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/groovy/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/java/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/java/mycompany/Test.java").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/groovy/mycompany/Test.groovy").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/groovy/mycompany/Test.groovy").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/java/mycompany/Test.groovy").getPath()));
        assertEquals("mycompany.Test",  GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/java/mycompany/Test.groovy").getPath()));
    }

    public void testIsDomainClass() throws Exception {
        URL testUrl = new URL(TEST_URL);
        assertTrue(GrailsResourceUtils.isDomainClass(testUrl));
    }

    public void testGetPathFromRoot() throws Exception {
        assertEquals("mycompany/Test.groovy", GrailsResourceUtils.getPathFromRoot(TEST_PACKAGE_URL));
        assertEquals("Test.groovy", GrailsResourceUtils.getPathFromRoot(TEST_URL));
    }

    public void testGetClassNameResource() throws Exception {
        Resource r = new UrlResource(new URL(TEST_URL));
        assertEquals("Test", GrailsResourceUtils.getClassName(r));
    }

    public void testGetClassNameString() {
        assertEquals("Test", GrailsResourceUtils.getClassName(TEST_URL));
    }

    public void testIsGrailsPath() {
        assertTrue(GrailsResourceUtils.isGrailsPath(TEST_URL));
    }

    public void testIsTestPath() {
        assertTrue(GrailsResourceUtils.isGrailsPath(UNIT_TESTS_URL));
    }

    public void testGetTestNameResource() throws Exception {
        Resource r = new UrlResource(new URL(UNIT_TESTS_URL));
        assertEquals("SomeTests", GrailsResourceUtils.getClassName(r));
    }

    public void testGetTestNameString() {
        assertEquals("SomeTests", GrailsResourceUtils.getClassName(UNIT_TESTS_URL));
    }

    public void testGetViewsDirForURL() throws Exception {
        Resource viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_CONTROLLER_URL));
        assertEquals(toFileUrl("/test/grails/app/grails-app/views"), viewsDir.getURL().toString());

        viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_URL));
        assertEquals(toFileUrl("/test/grails/app/grails-app/views"), viewsDir.getURL().toString());
    }

    public void testGetAppDir() throws Exception {
        Resource appDir = GrailsResourceUtils.getAppDir(new UrlResource(TEST_CONTROLLER_URL));
        assertEquals(toFileUrl("/test/grails/app/grails-app"), appDir.getURL().toString());

        appDir = GrailsResourceUtils.getAppDir(new UrlResource(TEST_URL));
        assertEquals(toFileUrl("/test/grails/app/grails-app"), appDir.getURL().toString());
    }

    public void testGetDirWithinWebInf() throws Exception {
        Resource viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_CONTROLLER_URL));
        Resource pluginViews = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_PLUGIN_CTRL));

        Resource webInfViews = GrailsResourceUtils.getViewsDir(new UrlResource(WEBINF_CONTROLLER));
        Resource webInfPluginViews = GrailsResourceUtils.getViewsDir(new UrlResource(WEBINF_PLUGIN_CTRL));

        assertEquals(toFileUrl("/test/grails/app/grails-app/views"), viewsDir.getURL().toString());
        assertEquals(toFileUrl("/test/grails/app/plugins/myplugin/grails-app/views"),
                pluginViews.getURL().toString());
        assertEquals(toFileUrl("/test/grails/app/WEB-INF/grails-app/views"),
                webInfViews.getURL().toString());
        assertEquals(toFileUrl("/test/grails/app/WEB-INF/plugins/myplugin/grails-app/views"),
                webInfPluginViews.getURL().toString());

        assertEquals("/WEB-INF/grails-app/views", GrailsResourceUtils.getRelativeInsideWebInf(webInfViews));
        assertEquals("/WEB-INF/plugins/myplugin/grails-app/views",
                GrailsResourceUtils.getRelativeInsideWebInf(webInfPluginViews));

        assertEquals("/WEB-INF/plugins/myplugin/grails-app/views",
                GrailsResourceUtils.getRelativeInsideWebInf(pluginViews));
        assertEquals("/WEB-INF/grails-app/views", GrailsResourceUtils.getRelativeInsideWebInf(viewsDir));
    }

    public void testGetPluginContextPath() throws Exception {
        MockServletContext servletContext = new MockServletContext("/myapp");
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/myapp");

        assertEquals("", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(TEST_CONTROLLER_URL), null));
        assertEquals("plugins/myplugin", GrailsResourceUtils.getStaticResourcePathForResource(
                new UrlResource(TEST_PLUGIN_CTRL), null));
        assertEquals("", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_CONTROLLER), null));
        assertEquals("plugins/myplugin", GrailsResourceUtils.getStaticResourcePathForResource(
                new UrlResource(WEBINF_PLUGIN_CTRL), null));
        assertEquals("/myapp", GrailsResourceUtils.getStaticResourcePathForResource(
                new UrlResource(WEBINF_CONTROLLER), request.getContextPath()));
        assertEquals("/myapp/plugins/myplugin", GrailsResourceUtils.getStaticResourcePathForResource(
                new UrlResource(WEBINF_PLUGIN_CTRL), request.getContextPath()));
    }

    public void testAppendPiecesForUri() {
        assertEquals("", GrailsResourceUtils.appendPiecesForUri(""));
        assertEquals("/alpha/beta/gamma", GrailsResourceUtils.appendPiecesForUri("/alpha", "/beta", "/gamma"));
        assertEquals("/alpha/beta/gamma", GrailsResourceUtils.appendPiecesForUri("/alpha/", "/beta/", "/gamma"));
        assertEquals("/alpha/beta/gamma/", GrailsResourceUtils.appendPiecesForUri("/alpha/", "/beta/", "/gamma/"));
        assertEquals("alpha/beta/gamma", GrailsResourceUtils.appendPiecesForUri("alpha", "beta", "gamma"));
    }

    private String toFileUrl(String path) {
        if (path == null) return path;
        String url = null;
        try {
            url = new File(path).toURI().toURL().toString();
        } catch (MalformedURLException e) {
            url = path;
        }
        return url;
    }
}
