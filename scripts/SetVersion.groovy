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
 * Gant script that manages the application version
 *
 * @author Marc Palmer
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 *
 * @since 0.5
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

target ('default': "Sets the current application version") {
    if(args != null) {
        Ant.property(name:"app.version.new", value: args)
    } else {
        Ant.property(file:"${basedir}/application.properties")
        def oldVersion = Ant.antProject.properties.'app.version'
        Ant.input(addProperty:"app.version.new", message:"Enter the new version",defaultvalue:oldVersion)
    }

    def newVersion = Ant.antProject.properties.'app.version.new'
    Ant.propertyfile(file:"${basedir}/application.properties") {
        entry(key:"app.version", value: newVersion)
    }

    event("StatusFinal", [ "Application version updated to $newVersion"])
}