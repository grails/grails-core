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

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import org.grails.cli.profile.Command
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware

/**
 * Lists the available {@link org.grails.cli.profile.Profile} instances 
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ListProfilesCommand implements Command, ProfileRepositoryAware {

    final String name = "list-profiles"
    final CommandDescription description = new CommandDescription(name, "Lists the available profiles", "grails list-profiles")

    ProfileRepository profileRepository

    @Override
    boolean handle(ExecutionContext executionContext) {
        def allProfiles = profileRepository.allProfiles
        def console = executionContext.console
        console.addStatus("Available Profiles")
        console.log('--------------------')
        for(Profile p in allProfiles) {
            console.log("* $p.name - ${p.description}")
        }

        return true
    }
}
