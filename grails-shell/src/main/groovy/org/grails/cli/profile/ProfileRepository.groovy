package org.grails.cli.profile

import java.util.List;
import java.util.Set;

import groovy.transform.CompileStatic

import org.grails.cli.profile.simple.SimpleProfile

@CompileStatic
class ProfileRepository {
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
            profileInstance = SimpleProfile.create(this, profileName, profileDirectory)
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
            executeGitCommand(parentDir, "clone","https://github.com/grails-profiles/grails-profile-repository", "repository")
        } else {
            File fetchHead = new File(profilesDirectory, ".git/FETCH_HEAD")
            if(!fetchHead.exists() || fetchHead.lastModified() < System.currentTimeMillis() - updateInterval) {
                executeGitCommand(profilesDirectory, "fetch")
                executeGitCommand(profilesDirectory, "rebase")
            }
        }
        profilesDirectory
    }

    private void executeGitCommand(File directory, String... commandArgs) {
        List fullCommand = ["git"]+ (commandArgs as List)
        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand).directory(directory).inheritIO()
        Process process = processBuilder.start()
        def exitcode = process.waitFor()
        if(exitcode != 0) {
            throw new RuntimeException("Cannot execute git command ${commandArgs as List} exitcode $exitcode")
        }
    }
}
