package org.codehaus.groovy.grails.commons;

import junit.framework.TestCase;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import java.net.URL;

public class GrailsResourceUtilsTests extends TestCase {

	private static final String TEST_URL = "file:///test/grails/app/grails-app/domain/Test.groovy";
    private static final String TEST_PACKAGE_URL = "file:///test/grails/app/grails-app/domain/mycompany/Test.groovy";
    private static final String TEST_CONTROLLER_URL = "file:///test/grails/app/grails-app/controllers/TestController.groovy";
    private static final String TEST_PLUGIN_CTRL = "file:///test/grails/app/plugins/myplugin/grails-app/controllers/TestController.groovy";

    private static final String WEBINF_CONTROLLER = "file:///test/grails/app/WEB-INF/grails-app/controllers/TestController.groovy";
    private static final String WEBINF_PLUGIN_CTRL = "file:///test/grails/app/WEB-INF/plugins/myplugin/grails-app/controllers/TestController.groovy";

    private static final String UNIT_TESTS_URL = "file:///test/grails/app/grails-tests/SomeTests.groovy";

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testIsDomainClass() throws Exception {

        URL testUrl = new URL(TEST_URL);

		assertTrue(GrailsResourceUtils.isDomainClass(testUrl));
	}

    public void testGetPathFromRoot() throws Exception {
        assertEquals("Test.groovy", GrailsResourceUtils.getPathFromRoot(TEST_URL));
        assertEquals("mycompany/Test.groovy", GrailsResourceUtils.getPathFromRoot(TEST_PACKAGE_URL));
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

        assertEquals("file:/test/grails/app/grails-app/views", viewsDir.getURL().toString());

        viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_URL));

        assertEquals("file:/test/grails/app/grails-app/views", viewsDir.getURL().toString());
    }

    public void testGetAppDir() throws Exception {
        Resource appDir = GrailsResourceUtils.getAppDir(new UrlResource(TEST_CONTROLLER_URL));

        assertEquals("file:/test/grails/app/grails-app", appDir.getURL().toString());

        appDir = GrailsResourceUtils.getAppDir(new UrlResource(TEST_URL));

        assertEquals("file:/test/grails/app/grails-app", appDir.getURL().toString());

    }

    public void testGetDirWithinWebInf() throws Exception {
        Resource viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_CONTROLLER_URL));
        Resource pluginViews = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_PLUGIN_CTRL));

        Resource webInfViews = GrailsResourceUtils.getViewsDir(new UrlResource(WEBINF_CONTROLLER));
        Resource webInfPluginViews = GrailsResourceUtils.getViewsDir(new UrlResource(WEBINF_PLUGIN_CTRL));

        assertEquals("file:/test/grails/app/grails-app/views", viewsDir.getURL().toString());
        assertEquals("file:/test/grails/app/plugins/myplugin/grails-app/views", pluginViews.getURL().toString());
        assertEquals("file:/test/grails/app/WEB-INF/grails-app/views", webInfViews.getURL().toString());
        assertEquals("file:/test/grails/app/WEB-INF/plugins/myplugin/grails-app/views", webInfPluginViews.getURL().toString());

        assertEquals("/WEB-INF/grails-app/views", GrailsResourceUtils.getRelativeInsideWebInf(webInfViews));
        assertEquals("/WEB-INF/plugins/myplugin/grails-app/views", GrailsResourceUtils.getRelativeInsideWebInf(webInfPluginViews));

        assertEquals("/WEB-INF/plugins/myplugin/grails-app/views", GrailsResourceUtils.getRelativeInsideWebInf(pluginViews));
        assertEquals("/WEB-INF/grails-app/views", GrailsResourceUtils.getRelativeInsideWebInf(viewsDir));

    }

    public void testGetPluginContextPath() throws Exception {
        MockServletContext servletContext = new MockServletContext("/myapp");
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/myapp");

        assertEquals("", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(TEST_CONTROLLER_URL), null));
        assertEquals("plugins/myplugin", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(TEST_PLUGIN_CTRL), null));
        assertEquals("", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_CONTROLLER), null));
        assertEquals("plugins/myplugin", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_PLUGIN_CTRL), null));
        assertEquals("/myapp", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_CONTROLLER), request.getContextPath()));
        assertEquals("/myapp/plugins/myplugin", GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_PLUGIN_CTRL), request.getContextPath()));
    }

}
