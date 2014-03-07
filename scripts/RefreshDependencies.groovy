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

import groovy.xml.MarkupBuilder


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

        f.withWriter('UTF-8') { writer ->
            def xml = new MarkupBuilder(writer)
            xml.dependencies {
                xml.build {
                   if(grailsSettings.buildResolveReport) {
                       handleArtifactReport(grailsSettings.buildResolveReport.resolvedArtifacts, xml)
                   }
                }
                xml.compile {
                   if(grailsSettings.compileResolveReport) {
                       handleArtifactReport(grailsSettings.compileResolveReport.resolvedArtifacts, xml)
                   }
                }
                xml.test {
                   if(grailsSettings.testResolveReport) {
                       handleArtifactReport(grailsSettings.testResolveReport.resolvedArtifacts, xml)
                   }
                }
                xml.runtime {
                   if(grailsSettings.runtimeResolveReport) {
                       handleArtifactReport(grailsSettings.runtimeResolveReport.resolvedArtifacts, xml)
                   }
                }
                xml.provided {
                    if(grailsSettings.providedResolveReport) {
                       handleArtifactReport(grailsSettings.providedResolveReport.resolvedArtifacts, xml)
                    }
                }
            }
        }
    }
    event "StatusFinal", ["Dependencies refreshed."]
}

private handleArtifactReport(allReports, xml) {
    allReports.findAll { downloadReport ->
        !downloadReport.file.name.endsWith("-sources.jar") && !downloadReport.file.name.endsWith("-javadoc.jar")
    }.each { downloadReport ->
        def mrid = downloadReport.dependency
        xml.dependency(group:mrid.group, name:mrid.name, version:mrid.version) {
            xml.jar downloadReport.file


            def fileName = downloadReport.file.name
            def baseName = fileName.substring(0, fileName.lastIndexOf('.'))
            def sourceJar = allReports.find { "$baseName-sources.jar" == it.file?.name}
            if (sourceJar) {
                xml.source sourceJar.file
            }
            def javadocJar = allReports.find { "$baseName-javadoc.jar" == it.file?.name}
            if (javadocJar) {
                xml.javadoc javadocJar.file
            }
        }
    }
}

setDefaultTarget(refreshDependencies)
