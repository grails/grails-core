/*
 * Copyright 2013 SpringSource.
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
 * Installs app and plugin templates.
 *
 * @author Burt Beckwith
 *
 * @since 2.3
 */

includeTargets << grailsScript('_GrailsInit')

target(installAppTemplates: 'Installs app and plugin templates') {
    depends(parseArguments)

    File destination = new File(grailsSettings.grailsWorkDir, 'app-templates')
    ant.mkdir dir: destination.path

    copyTemplateJar 'grails-shared-files', destination
    copyTemplateJar 'grails-app-files', destination
    copyTemplateJar 'grails-plugin-files', destination
    copyTemplateJar 'grails-integration-files', destination

    event('StatusUpdate', ["Application templates installed to $destination"])
}

void copyTemplateJar(String name, File destination) {

    File jar = new File(destination, "${name}.jar")
    if (jar.exists() && !confirmInput("Overwrite existing $name? ", "overwrite.templates-$name")) {
        return
    }

    ant.copy(todir: destination) {
        javaresource(name: jar.name)
    }
}

setDefaultTarget 'installAppTemplates'
