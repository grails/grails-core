package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.io.support.GrailsIOUtils
import org.codehaus.groovy.tools.RootLoader
import org.springframework.core.io.FileSystemResource

/**
 * A Mock TagLibraryResolver that resolves the standard taglib from the GRAILS_HOME/lib directory instead of the root loader
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class MockRootLoaderTagLibraryResolver extends TagLibraryResolverImpl {

    protected RootLoader resolveRootLoader() {
        def rootLoader = new RootLoader([] as URL[], Thread.currentThread().getContextClassLoader())
        def res = new FileSystemResource(GrailsIOUtils.findJarFile(org.apache.taglibs.standard.tag.el.core.OutTag))
        rootLoader.addURL res.getURL()
        return rootLoader
    }
}
