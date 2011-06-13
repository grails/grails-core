/*
 * Copyright 2004-2005 the original author or authors.
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

/**
 * Gant script that loads the Grails interactive shell
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.tools.shell.*
import org.codehaus.groovy.grails.cli.logging.*

includeTargets << grailsScript("_GrailsBootstrap")

target ('default': "Load the Grails interactive shell") {
    depends(configureProxy, packageApp, classpath)
    shell()
}

target(shell:"The shell implementation target") {

    loadApp()
    configureApp()
    def b = new Binding(ctx: appCtx, grailsApplication: grailsApp)

    def listeners = appCtx.getBeansOfType(PersistenceContextInterceptor)
    listeners?.each { key, listener -> listener.init() }
    def shell = new Groovysh(classLoader,b, new IO(console.input, System.out, System.err))

    def watcher = new org.codehaus.groovy.grails.compiler.GrailsProjectWatcher(projectCompiler, pluginManager)
    watcher.start()

    shell.run([] as String[])
    listeners?.each { key, listener ->
        listener.flush()
        listener.destroy()
    }
}
