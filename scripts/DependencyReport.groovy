import groovy.xml.NamespaceBuilder
import org.codehaus.groovy.grails.resolve.IvyDependencyManager

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
 * Generates an Ivy dependency report for the current Grails application 
 *
 * @author Graeme Rocher
 *
 * @since 1.2
 */

includeTargets << grailsScript("_GrailsSettings")

target(dependencyReport:"Produces a dependency report for the current Grails application") {
    // create ivy namespace
    ivy = NamespaceBuilder.newInstance(ant, 'antlib:org.apache.ivy.ant')

    String targetDir = "$projectTargetDir/dependency-report"
    ant.delete(dir:targetDir, failonerror:false)
    ant.mkdir(dir:targetDir)

    println "Obtaining dependency data..."
    IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
    for(conf in IvyDependencyManager.ALL_CONFIGURATIONS) {
        dependencyManager.resolveDependencies(conf)
    }

    def conf = args.trim() ?: 'build, compile, provided, runtime, test'
    ivy.report(organisation:grailsAppName, module:grailsAppName,todir:targetDir, conf:conf)

    println "Dependency report output to [$targetDir]"
}
setDefaultTarget(dependencyReport)