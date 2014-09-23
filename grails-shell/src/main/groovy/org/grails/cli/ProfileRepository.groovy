package org.grails.cli

import groovy.transform.CompileStatic

@CompileStatic
class ProfileRepository {
    File profilesDirectory = new File(new File(System.getProperty("user.home")), ".grails/repository")
    boolean initialized = false
    long updateInterval = 10*60000L
    
    File getProfileDirectory(String profile) {
        File profileDirectory = new File(new File(profilesDirectory, "profiles"), profile)
        profileDirectory
    }
    
    Profile getProfile(String profile) {
        if(!initialized) {
            createOrUpdateRepository()
            initialized=true
        }
        File profileDirectory = getProfileDirectory(profile)
        if(profileDirectory.exists()) {
            return new SimpleProfile(profile, profileDirectory)
        } else {
            return null
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
