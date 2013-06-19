includeTargets << grailsScript("_GrailsInit")

USAGE = """
    wrapper [--wrapperDir=dir] [--distributionUrl=url]

where
    --wrapperDir = Directory where wrapper support files are installed relative to project root
    --distributationUrl = URL to the directory where the release may be downloaded from if necessary

examples
    grails wrapper --wrapperDir=grailsWrapper
    grails wrapper --wrapperDir=grailsWrapper --distributionUrl=http://dist.springframework.org.s3.amazonaws.com/milestone/GRAILS/

optional argument default values
    wrapperDir = 'wrapper'
    distributionUrl = 'http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/'

"""

target ('default': "Installs the Grails wrapper") {
    depends(checkVersion, parseArguments)
    event 'InstallWrapperStart', [ 'Installing Wrapper...' ]

    grailsDistUrl =  argsMap.distributionUrl ?: 'http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/'
    grailsWrapperDir = argsMap.wrapperDir ?: 'wrapper'

    targetDir = "${basedir}/${grailsWrapperDir}"

    supportFiles = []
    new File("${grailsHome}/dist/").eachFileMatch( groovy.io.FileType.FILES, { it ==~ /grails-wrapper-support.*/ }) {
        supportFiles << it
    }
    if (supportFiles.size() != 1) {
        if (supportFiles.size() == 0) {
            event("StatusError", ["An error occurred locating the grails-wrapper-support jar file"])
        } else {
            event("StatusError", ["Multiple grails-wrapper-support jar files were found ${supportFiles.absolutePath}"])
        }
        exit 1
    }
    supportFile = supportFiles[0]
    ant.unjar(dest: targetDir, src: supportFile.absolutePath, overwrite: true) {
        patternset {
            exclude(name: "META-INF/**")
        }
    }
    ant.move(todir: basedir) {
        fileset(dir: targetDir) {
            include(name: 'grailsw*')
        }
    }
    ant.replace(dir: targetDir, includes: '*.properties', token: '@distributationUrl@', value: grailsDistUrl)
    ant.replace(dir: basedir, includes: 'grailsw*', token: '@wrapperDir@', value: grailsWrapperDir)
    ant.chmod(file: 'grailsw', perm: 'u+x')

    springloadedFiles = []
    new File("${grailsHome}/lib/org.springsource.springloaded/springloaded-core/jars/").eachFileMatch( groovy.io.FileType.FILES, { it ==~ /springloaded-core-.*/ }) {
        springloadedFiles << it
    }

    springloadedFiles = springloadedFiles.findAll { !it.name.contains('sources') &&  !it.name.contains('javadoc')}

    if (springloadedFiles.size() != 1) {
        if (springloadedFiles.size() == 0) {
            event("StatusError", ["An error occurred locating the springloaded-core jar file"])
        } else {
            event("StatusError", ["Multiple springloaded-core jar files were found ${springloadedFiles.absolutePath}"])
        }
        exit 1
    }

    springloadedFile = springloadedFiles[0]

    ant.copy(todir: targetDir, file: springloadedFile.absolutePath, overwrite: true)

    event("StatusUpdate", [ "Wrapper installed successfully"])
    event 'InstallWrapperEnd', [ 'Finished Installing Wrapper.' ]
}
