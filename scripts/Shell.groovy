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

import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.codehaus.groovy.grails.cli.support.*

includeTargets << grailsScript("_GrailsBootstrap")

target ('default': "Load the Grails interactive shell") {
    depends(configureProxy, enableExpandoMetaClass, packageApp, classpath, shell)
}

target(shell:"The shell implementation target") {
    depends(loadApp, configureApp)

    

    def listeners = appCtx.getBeansOfType(PersistenceContextInterceptor)
    listeners?.each { key, listener -> listener.init() }

    def newDependencyManager = grailsSettings.dependencyManager.createCopy(grailsSettings)
    newDependencyManager.parseDependencies {
        dependencies {
            compile 'org.codehaus.groovy:groovy-groovysh:2.2.0-rc-2'
        }
    }
    def report = newDependencyManager.resolve()
    def urls = report.jarFiles.collect { it.toURI().toURL() } 
    def classLoader = new ChildFirstURLClassLoader(classLoader)
    for(url in urls) {
        classLoader.addURL(url)
    }
    def binding = classLoader.loadClass('groovy.lang.Binding').newInstance([ctx: appCtx, grailsApplication: grailsApp])
    def shellIO = classLoader.loadClass('org.codehaus.groovy.tools.shell.IO').newInstance(grailsConsole.input, System.out, System.err)
    def shell = classLoader.loadClass('org.codehaus.groovy.tools.shell.Groovysh').newInstance(classLoader, binding, shellIO)

    // def shell = new Groovysh(classLoader,b, new IO(grailsConsole.input, System.out, System.err))

    new GrailsProjectWatcher(projectCompiler, pluginManager).start()

    shell.run([] as String[])
    listeners?.each { key, listener ->
        listener.flush()
        listener.destroy()
    }
}
