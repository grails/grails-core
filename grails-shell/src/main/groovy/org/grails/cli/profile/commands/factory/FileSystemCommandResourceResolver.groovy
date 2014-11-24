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
import org.grails.cli.profile.Profile
import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource

import java.util.regex.Pattern


/**
 * A {@link CommandResourceResolver} that resolves from the file system
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class FileSystemCommandResourceResolver implements CommandResourceResolver {

    final Collection<String> matchingFileExtensions
    final Pattern fileNamePatternRegex

    FileSystemCommandResourceResolver(Collection<String> matchingFileExtensions) {
        this.matchingFileExtensions = matchingFileExtensions
        final String fileNamePattern = /^.*\.(${matchingFileExtensions.join('|')})$/
        this.fileNamePatternRegex = Pattern.compile(fileNamePattern)
    }

    @Override
    Collection<Resource> findCommandResources(Profile profile) {
        File commandsDir = new File(profile.profileDir, "commands")
        Collection<File> commandFiles = commandsDir.listFiles().findAll { File file ->
            file.isFile() && file.name ==~ fileNamePatternRegex
        }.sort(false) { File file -> file.name }
        return commandFiles.collect() { File f -> new FileSystemResource(f) }
    }
}
