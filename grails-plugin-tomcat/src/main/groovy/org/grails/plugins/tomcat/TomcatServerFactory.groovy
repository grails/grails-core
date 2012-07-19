/*
 * Copyright 2012 SpringSource
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
package org.grails.plugins.tomcat

import grails.web.container.EmbeddableServer
import grails.web.container.EmbeddableServerFactory
import org.grails.plugins.tomcat.fork.ForkedTomcatServer
import org.grails.plugins.tomcat.fork.TomcatExecutionContext
import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import grails.util.Environment
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess


class TomcatServerFactory implements EmbeddableServerFactory,BuildSettingsAware {

    BuildSettings buildSettings

    @CompileStatic
    EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        final obj = buildSettings?.forkSettings?.get("run")
        if(obj) {
            return createForked(contextPath, obj)
        }
        else {
            return new InlineExplodedTomcatServer(basedir, webXml, contextPath, classLoader)
        }
    }

    @CompileStatic
    private ForkedTomcatServer createForked(String contextPath, forkConfig) {
        TomcatExecutionContext ec = new TomcatExecutionContext()
        List<File> buildDependencies = buildMinimalIsolatedClasspath()

        ec.buildDependencies = buildDependencies
        ec.runtimeDependencies = buildSettings.runtimeDependencies
        ec.providedDependencies = buildSettings.providedDependencies
        ec.contextPath = contextPath
        ec.baseDir = buildSettings.baseDir
        ec.env = Environment.current.name
        ec.grailsHome = buildSettings.grailsHome
        ec.classesDir = buildSettings.classesDir
        ec.grailsWorkDir = buildSettings.grailsWorkDir
        ec.projectWorkDir = buildSettings.projectWorkDir
        ec.projectPluginsDir = buildSettings.projectPluginsDir
        ec.testClassesDir = buildSettings.testClassesDir
        ec.resourcesDir = buildSettings.resourcesDir

        final forkedTomcat = new ForkedTomcatServer(ec)
        if(forkConfig instanceof Map)
            forkedTomcat.configure((Map)forkConfig)


        return forkedTomcat
    }


    @CompileStatic
    private List<File> buildMinimalIsolatedClasspath() {
        List<File> buildDependencies = ForkedGrailsProcess.buildMinimalIsolatedClasspath(buildSettings)
        final tomcatJars = IsolatedWarTomcatServer.findTomcatJars(buildSettings)
        buildDependencies.addAll(tomcatJars.findAll { File f -> !f.name.contains('juli')})
        return buildDependencies
    }

    EmbeddableServer createForWAR(String warPath, String contextPath) {
        return new IsolatedWarTomcatServer(warPath, contextPath)
    }
}
