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

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import groovy.transform.CompileStatic

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.grails.cli.profile.*


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
    String originUri = "https://github.com/grails/grails-profile-repository"
    String gitBranch = 'master'
    // use fixed git revision, used in unit tests
    String gitRevision
    ResetType gitRevisionResetMode = ResetType.HARD
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

    @Override
    List<Profile> getAllProfiles() {
        def allDirectories = new File(profilesDirectory, "profiles").listFiles()?.findAll() { File f -> f.isDirectory() && !f.isHidden() && !f.name.startsWith('.') }
        return allDirectories.collect() { File f -> getProfile(f.name) }
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
    
    public File createOrUpdateRepository() {
        if(!profilesDirectory.exists()) {
            def parentDir = profilesDirectory.getParentFile()
            if(!parentDir.exists()) parentDir.mkdir()
            Git.cloneRepository().setURI(originUri)
                                 .setDirectory(profilesDirectory)
                                 .setBranch(gitBranch)
                                .call()
        } else {
            boolean hasGitRevision = gitRevision
            fetchAndRebaseIfExpired(hasGitRevision)
        }
        if (gitRevision) {
            Git git = Git.open(profilesDirectory)
            git.reset().setRef(gitRevision).setMode(gitRevisionResetMode).call()
        }
        else {
            checkoutTagForRelease()

        }
        profilesDirectory
    }

    public void checkoutTagForRelease() {
        def grailsVersion = BuildSettings.package.implementationVersion
        // if this is not a snapshot version then checkout the tag for this release, otherwise use master
        if (grailsVersion != null && !grailsVersion.endsWith('-SNAPSHOT')) {
            try {
                def git = Git.open(profilesDirectory)
                git.checkout().setName("v$grailsVersion").call()
            } catch (Throwable e) {
                GrailsConsole.getInstance().error("Could not checkout tag for Grails release [$grailsVersion]: " + e.message, e)
            }
        }
    }

    public void fetchAndRebaseIfExpired(boolean forceUpdate = false) {
        File fetchHead = new File(profilesDirectory, ".git/FETCH_HEAD")
        if(forceUpdate || !fetchHead.exists() || fetchHead.lastModified() < System.currentTimeMillis() - updateInterval) {
            try {
                Git git = Git.open(profilesDirectory)
                git.fetch().call()
            } catch (Exception e) {
                GrailsConsole.getInstance().error("Problem updating profiles from origin git repository", e)
            }
        }
    }

}
