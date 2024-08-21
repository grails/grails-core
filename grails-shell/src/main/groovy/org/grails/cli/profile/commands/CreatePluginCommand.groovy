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

package org.grails.cli.profile.commands

import groovy.transform.CompileStatic
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
/**
 * A command for creating a plugin
 *
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class CreatePluginCommand extends CreateAppCommand {

    public static final String NAME = "create-plugin"

    CreatePluginCommand() {
        description.description = "Creates a plugin"
        description.usage = "create-plugin [NAME]"
    }

    @Override
    protected void populateDescription() {
        description.argument(name: "Plugin Name", description: "The name of the plugin to create.", required: false)
    }

    @Override
    String getName() { NAME }

    @Override
    protected String getDefaultProfile() { "web-plugin" }

    protected boolean validateProfile(Profile profileInstance, String profileName, ExecutionContext executionContext) {
        def pluginProfile = profileInstance.extends.find() { Profile parent -> parent.name == 'plugin' }
        if(profileName != 'plugin' && pluginProfile == null) {
            executionContext.console.error("No valid plugin profile found for name [$profileName]")
            return false
        }
        else {
            return super.validateProfile(profileInstance, profileName)
        }
    }
}
