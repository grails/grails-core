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

package org.grails.cli.profile.commands.factory

import groovy.transform.CompileStatic
import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile

import java.util.regex.Pattern

/**
 * A abstract {@link CommandFactory} that reads from the file system
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
abstract class FileBasedCommandFactory<T> implements CommandFactory {

    @Override
    Collection<Command> findCommands(Profile profile) {
        def files = findCommandFiles(profile.profileDir)
        Collection<Command> commands = []
        for(File file in files) {
            String commandName = evaluateFileName(file)
            def data = readCommandFile(file)
            commands << createCommand(profile, commandName, file, data)
        }
        return commands
    }

    protected String evaluateFileName(File file) {
        file.name - getFileExtensionPattern()
    }


    protected Collection<File> findCommandFiles(File profileDir) {
        File commandsDir = new File(profileDir, "commands")
        Collection<File> commandFiles = commandsDir.listFiles().findAll { File file ->
            file.isFile() && file.name ==~ getFileNamePattern()
        }.sort(false) { File file -> file.name }
        return commandFiles
    }

    protected abstract T readCommandFile(File file)

    protected abstract Command createCommand(Profile profile, String commandName, File file, T data)

    protected abstract Pattern getFileNamePattern()

    protected abstract Pattern getFileExtensionPattern()

}
