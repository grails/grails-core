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
package org.grails.cli.gradle

import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.grails.gradle.plugin.model.GrailsClasspath

/**
 * Gets the EclipseProject which helps obtain the classpath necessary
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class ClasspathBuildAction implements BuildAction<GrailsClasspath>, Serializable {
    @Override
    GrailsClasspath execute(BuildController controller) {
        controller.getModel(GrailsClasspath)
    }
}
