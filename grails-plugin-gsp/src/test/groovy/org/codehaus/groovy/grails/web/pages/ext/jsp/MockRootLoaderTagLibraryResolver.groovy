package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.tools.RootLoader
import org.springframework.core.io.FileSystemResource

/**
 * A Mock TagLibraryResolver that resolves the standard taglib from the GRAILS_HOME/lib directory instead of the root loader
 * 
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 9, 2008
 */
class MockRootLoaderTagLibraryResolver extends TagLibraryResolver {

    protected RootLoader resolveRootLoader() {
        def rootLoader = new RootLoader([] as URL[], Thread.currentThread().getContextClassLoader())
        def res = new FileSystemResource("lib/standard-1.1.2.jar")
        rootLoader.addURL res.getURL()
        return rootLoader
    }

}
