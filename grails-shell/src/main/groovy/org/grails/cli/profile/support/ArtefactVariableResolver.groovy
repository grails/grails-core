package org.grails.cli.profile.support

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.commands.templates.SimpleTemplate

/*
 * Copyright 2014 original authors
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
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ArtefactVariableResolver {
    String artifactPackage, artifactName
    Map<String, String> variables = [:]

    ArtefactVariableResolver(String artifactName, String artifactPackage = null) {
        this.artifactPackage = artifactPackage
        this.artifactName = artifactName
        createVariables()
    }

    Map createVariables() {
        if(artifactPackage) {
            variables['artifact.package.name'] = artifactPackage
            variables['artifact.package.path'] = artifactPackage?.replace('.','/')
            variables['artifact.package'] = "package $artifactPackage\n".toString()
        }
        variables['artifact.name'] = artifactName
        variables['artifact.propertyName'] = GrailsNameUtils.getPropertyName(artifactName)
        return variables
    }

    File resolveFile(String pathToResolve, ExecutionContext context) {
        String destinationName = new SimpleTemplate(pathToResolve).render(variables)
        File destination = new File(context.baseDir, destinationName).absoluteFile

        if(!destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs()
        }
        return destination
    }
}
