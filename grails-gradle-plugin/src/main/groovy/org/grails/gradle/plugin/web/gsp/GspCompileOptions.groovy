package org.grails.gradle.plugin.web.gsp

import org.gradle.api.tasks.compile.GroovyForkOptions

class GspCompileOptions {
	String encoding = "UTF-8"
    GroovyForkOptions forkOptions = new GroovyForkOptions()
}