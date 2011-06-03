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

package org.codehaus.groovy.grails.compiler

import grails.util.BuildSettings

/**
 * Encapsulates the logic to package a project ready for execution
 *
 * TODO: This class is a work-in-progress port from the code in the script _GrailsPackage.groovy
 *
 * @since 1.4
 * @author Graeme Rocher
 */
class GrailsProjectPackager {


    GrailsProjectCompiler projectCompiler
    BuildSettings buildSettings
    private AntBuilder ant

    GrailsProjectPackager(GrailsProjectCompiler compiler) {
        this.projectCompiler = compiler
        buildSettings = compiler.buildSettings
        ant = compiler.ant
    }

    AntBuilder getAnt() {
       if(this.ant == null) {
           this.ant = new AntBuilder()
       }
       return ant
    }
    /**
     * Packages any config files such as Hibernate config, XML files etc.
     * to the projects resources directory
     *
     * @param from Where to package from
     */
    void packageConfigFiles(String from) {
        def targetPath = buildSettings.resourcesDir.path
        def dir = new File(from, "grails-app/conf")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path) {
                    exclude(name:"**/*.groovy")
                    exclude(name:"**/log4j*")
                    exclude(name:"hibernate/**/*")
                    exclude(name:"spring/**/*")
                }
            }
        }

        dir = new File(dir, "hibernate")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path, includes:"**/*")
            }
        }

        dir = new File(from, "src/groovy")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path) {
                    exclude(name:"**/*.groovy")
                    exclude(name:"**/*.java")
                }
            }
        }

        dir = new File(from, "src/java")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path) {
                    exclude(name:"**/*.java")
                }
            }
        }
    }
}
