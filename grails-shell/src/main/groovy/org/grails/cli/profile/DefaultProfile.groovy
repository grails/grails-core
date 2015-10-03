/*
 * Copyright 2014 the original author or authors.
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
package org.grails.cli.profile

import grails.util.CosineSimilarity
import grails.util.Environment
import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.ScriptNameResolver
import org.grails.cli.interactive.completers.StringsCompleter
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.config.NavigableMap
import org.grails.io.support.PathMatchingResourcePatternResolver
import org.grails.io.support.Resource
import org.grails.io.support.StaticResourceLoader
import org.yaml.snakeyaml.Yaml;

/**
 * Simple disk based implementation of the {@link Profile} interface
 *
 * @since 3.0
 * @author Lari Hotari
 * @author Graeme Rocher
 */
@CompileStatic
class DefaultProfile extends AbstractProfile implements Profile {

    protected DefaultProfile(String name, Resource profileDir) {
        super(profileDir)
        super.name = name
    }

    @Override
    String getName() {
        super.name
    }

    public static Profile create(ProfileRepository repository, String name, Resource profileDir) {
        Profile profile = new DefaultProfile(name, profileDir)
        profile.initialize(repository)
        return profile
    }


    private void initialize(ProfileRepository repository) {
        this.profileRepository = repository
        parentProfiles = []
        Resource profileYml = profileDir.createRelative("profile.yml")
        if(profileYml.exists()) {
            profileConfig = (Map<String, Object>) new Yaml().loadAs(profileYml.getInputStream(), Map)
            def map = new NavigableMap()
            map.merge(profileConfig)
            navigableConfig = map
            String[] extendsProfiles = profileConfig.get("extends")?.toString()?.split(/\s*,\s*/)
            if(extendsProfiles) {
                parentProfiles = extendsProfiles.collect { String profileName ->
                    Profile extendsProfile = repository.getProfile(profileName)
                    if(extendsProfile==null) {
                        throw new RuntimeException("Profile $profileName not found. ${this.name} extends it.")
                    }
                    extendsProfile
                }
            }
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DefaultProfile that = (DefaultProfile) o

        if (name != that.name) return false

        return true
    }

    int hashCode() {
        return (name != null ? name.hashCode() : 0)
    }
}
