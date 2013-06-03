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
package org.codehaus.groovy.grails.cli.interactive.completors

import grails.util.BuildSettings
import grails.util.PluginBuildSettings
import jline.SimpleCompletor

import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

/**
 * A completor that completes
 */
abstract class ClassNameCompletor extends SimpleCompletor implements BuildSettingsAware {

    BuildSettings buildSettings

    ClassNameCompletor() {
        super("")
    }

    /**
     * @return The command name doing the completion
     */
    abstract String getCommandName()

    @Override
    int complete(String buffer, int cursor, List clist) {

        PluginBuildSettings pluginSettings = GrailsPluginUtils.getPluginBuildSettings(buildSettings)

        final resources = pluginSettings.getArtefactResourcesForOne(buildSettings.baseDir.absolutePath)
        def classNames = []
        resources.each { Resource r ->
            if (shouldInclude(r)) {
                classNames << "${commandName} ${GrailsResourceUtils.getClassName(r)}"
            }
        }

        setCandidateStrings classNames as String[]

        return super.complete(buffer, cursor, clist)
    }

    boolean shouldInclude(Resource res) { true }
}
