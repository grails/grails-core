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
package org.grails.cli.profile.git

import groovy.transform.CompileStatic
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.DefaultProfile


/**
 * An implementation of the {@link ProfileRepository} instance that uses git
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 *
 * @since 3.0
 */
@CompileStatic
class GitProfileRepository implements ProfileRepository{
    File profilesDirectory = new File(new File(System.getProperty("user.home")), ".grails/repository")
    boolean initialized = false
    long updateInterval = 10*60000L
    Map<String, Profile> profileCache=[:].asSynchronized()
    
    File getProfileDirectory(String profile) {
        File profileDirectory = new File(new File(profilesDirectory, "profiles"), profile)
        profileDirectory
    }
    
    Profile getProfile(String profileName) {
        Profile profileInstance = profileCache.get(profileName)
        if(profileInstance) return profileInstance
        if(!initialized) {
            createOrUpdateRepository()
            initialized=true
        }
        File profileDirectory = getProfileDirectory(profileName)
        if(profileDirectory.exists()) {
            profileInstance = DefaultProfile.create(this, profileName, profileDirectory)
            profileCache.put(profileName, profileInstance)
            return profileInstance
        } else {
            return null
        }
    }
    
    /**
     * Returns the given profile with all dependencies in topological order where
     * given profile is last in the order.
     * 
     * @param profile
     * @return
     */
    List<Profile> getProfileAndDependencies(Profile profile) {
        List<Profile> sortedProfiles = []
        Set<Profile> visitedProfiles = [] as Set
        visitTopologicalSort(profile, sortedProfiles, visitedProfiles)
        return sortedProfiles
    }
    
    private void visitTopologicalSort(Profile profile, List<Profile> sortedProfiles, Set<Profile> visitedProfiles) {
        if(profile != null && !visitedProfiles.contains(profile)) {
            visitedProfiles.add(profile)
            profile.getExtends().each { Profile dependentProfile ->
                visitTopologicalSort(dependentProfile, sortedProfiles, visitedProfiles);
            }
            sortedProfiles.add(profile)
        }
    }
    
    private File createOrUpdateRepository() {
        if(!profilesDirectory.exists()) {
            def parentDir = profilesDirectory.getParentFile()
            if(!parentDir.exists()) parentDir.mkdir()
            Git.cloneRepository().setURI("https://github.com/grails-profiles/grails-profile-repository")
                                 .setDirectory(new File(parentDir, "repository"))
                                .call()
        } else {
            File fetchHead = new File(profilesDirectory, ".git/FETCH_HEAD")
            if(!fetchHead.exists() || fetchHead.lastModified() < System.currentTimeMillis() - updateInterval) {
                Git git = new Git(new FileRepository(new File(profilesDirectory.parentFile, "repository")))
                git.fetch()
                git.rebase()
            }
        }
        profilesDirectory
    }

}
