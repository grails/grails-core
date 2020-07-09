package org.grails.gradle.plugin.web.gsp

import org.gradle.api.tasks.compile.GroovyForkOptions

/**
* Presents the Compile Options used by the {@llink GroovyPageForkCompileTask}
*
* @author David Estes
* @since 4.0
*/
class GspCompileOptions {
	String encoding = "UTF-8"
    GroovyForkOptions forkOptions = new GroovyForkOptions()
}