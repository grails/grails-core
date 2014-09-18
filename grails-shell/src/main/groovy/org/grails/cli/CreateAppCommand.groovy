package org.grails.cli

class CreateAppCommand {
    String appname
    String profile

    void run() {
        File profilesDirectory = createOrUpdateRepository()
        copySkeleton(profilesDirectory)
    }

    private copySkeleton(File profilesDirectory) {
        AntBuilder ant = new AntBuilder()
        ant.copy(todir: new File(appname)) {
            fileSet(dir: new File(new File(new File(profilesDirectory, "profiles"), profile), "skeleton")) {
                exclude(name: '**/.gitkeep')
            }
            filterset { filter(token:'APPNAME', value:appname) }
            mapper {
                filtermapper {
                    replacestring(from:'APPNAME', to:appname)
                }
            }
        }
    }

    private File createOrUpdateRepository() {
        File profilesDirectory = new File(new File(System.getProperty("user.home")), ".grails/repository")
        if(!profilesDirectory.exists()) {
            def parentDir = profilesDirectory.getParentFile()
            if(!parentDir.exists()) parentDir.mkdir()
            executeGitCommand(parentDir, "clone","https://github.com/grails-profiles/grails-profile-repository", "repository")
            println "Done."
        } else {
            File fetchHead = new File(profilesDirectory, ".git/FETCH_HEAD")
            if(!fetchHead.exists() || fetchHead.lastModified() < System.currentTimeMillis() - 10*60000) {
                executeGitCommand(profilesDirectory, "fetch")
                executeGitCommand(profilesDirectory, "rebase")
                println "Done fetch and rebase."
            }
        }
        profilesDirectory
    }

    private void executeGitCommand(File directory, String... commandArgs) {
        List fullCommand = ["git"]+ (commandArgs as List)
        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand).directory(directory).redirectErrorStream(true)
        Process process = processBuilder.start()
        println process.text
        def exitcode = process.waitFor()
        if(exitcode != 0) {
            throw new RuntimeException("Cannot execute git command ${commandArgs as List} exitcode $exitcode")
        }
    }
}
