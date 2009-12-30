/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.grails.test.report.junit

import grails.build.GrailsBuildListener

class JUnitReportProcessor implements GrailsBuildListener {
    
    void receiveGrailsBuildEvent(String name, Object[] args) {
        if (name == "TestProduceReports") {
            def buildBinding = args[0]
            buildBinding.with { 
                ant.junitreport(todir: "${testReportsDir}") {
                    fileset(dir: testReportsDir) {
                        include(name: "TEST-*.xml")
                    }
                    report(format: "frames", todir: "${testReportsDir}/html", *:(junitReportStyleDir ? [styledir: junitReportStyleDir] : [:]))
                }
            }
        }
    }
    
}