/*
 * Copyright 2011 SpringSource
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

import groovy.xml.NamespaceBuilder
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.apache.commons.io.*
/**
 * Refreshes application dependencies, installing any necessary plugins as necessary
 *
 * @author Graeme Rocher
 *
 * @since 1.2
 */

includeTargets << grailsScript("_PluginDependencies")

target(refreshDependencies:"Refreshes application dependencies, installing any necessary plugins as necessary") {
    resolveDependencies()

    if (argsMap.params) {
        // write data to file
        def f = new File(argsMap.params[0])

        f.withWriter { writer ->
            def xml = new groovy.xml.MarkupBuilder(writer)
            xml.dependencies {
                xml.build {
                   handleArtifactReport(grailsSettings.buildResolveReport.allArtifactsReports, xml)
                }
                xml.compile {
                   handleArtifactReport(grailsSettings.compileResolveReport.allArtifactsReports, xml)
                }
                xml.test {
                   handleArtifactReport(grailsSettings.testResolveReport.allArtifactsReports, xml)
                }
                xml.runtime {
                   handleArtifactReport(grailsSettings.runtimeResolveReport.allArtifactsReports, xml)
                }
                xml.provided {
                   handleArtifactReport(grailsSettings.providedResolveReport.allArtifactsReports, xml)
                }
            }
        }
    }
    event "StatusFinal", ["Dependencies refreshed."]
}

private handleArtifactReport(allReports, xml) {
    allReports.findAll { downloadReport ->
        !downloadReport.localFile.name.endsWith("-sources.jar") && !downloadReport.localFile.name.endsWith("-javadoc.jar")
    }.each { downloadReport ->
        def mrid = downloadReport.artifact.moduleRevisionId
        xml.dependency(group:mrid.organisation, name:mrid.name, version:mrid.revision) {
            xml.jar downloadReport.localFile
            def baseName = FilenameUtils.getBaseName(downloadReport.localFile.name)
            def sourceJar = allReports.find { "$baseName-sources.jar" == it.localFile?.name}
            if(sourceJar) {
                xml.source sourceJar.localFile
            }
            def javadocJar = allReports.find { "$baseName-javadoc.jar" == it.localFile?.name}
            if (javadocJar) {
                xml.javadoc javadocJar.localFile
            }
        }
    }
}
setDefaultTarget(refreshDependencies)
