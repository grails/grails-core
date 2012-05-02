includeTargets << grailsScript("_GrailsInit")

USAGE = """
    wrapper [--wrapperVersion=version] [--wrapperDir=dir] [--distributionUrl=url]

where
    --wrapperVersion = The version of Grails that the wrapper should use
    --wrapperDir = Directory where wrapper support files are installed relative to project root
    --distributationUrl = URL to the directory where the release may be downloaded from if necessary
    
examples
    grails wrapper --wrapperVersion=2.0.3
    grails wrapper --wrapperDir=grailsWrapper --wrapperVersion=2.0.3
    grails wrapper --wrapperVersion=2.0.0.RC1 --distributionUrl=http://dist.springframework.org.s3.amazonaws.com/milestone/GRAILS/
    
optional argument default values
    wrapperVersion = the version of Grails that the wrapper is being generated with ($grailsVersion)
    wrapperDir = 'wrapper'
    distributionUrl = 'http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/'
    
"""

target ('default': "Installs the Grails wrapper") {
    depends(checkVersion, parseArguments)
    event 'InstallWrapperStart', [ 'Installing Wrapper...' ]
    
    grailsWrapperVersion = argsMap.wrapperVersion ?: grailsVersion
    grailsDistUrl =  argsMap.distributionUrl ?: 'http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/'
    grailsWrapperDir = argsMap.wrapperDir ?: 'wrapper'
    
    targetDir = "${basedir}/${grailsWrapperDir}"

    supportFiles = []
    new File("${grailsHome}/dist/").eachFileMatch( groovy.io.FileType.FILES, { it ==~ /grails-wrapper-support.*/ }) {
        supportFiles << it
    }
    if(supportFiles.size() != 1) {
        if(supportFiles.size() == 0) {
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
    ant.replace(dir: targetDir, includes: '*.properties', token: '@wrapperVersion@', value: grailsWrapperVersion)
    ant.replace(dir: targetDir, includes: '*.properties', token: '@distributationUrl@', value: grailsDistUrl)
    ant.replace(dir: basedir, includes: 'grailsw*', token: '@wrapperDir@', value: grailsWrapperDir)
    ant.chmod(file: 'grailsw', perm: 'u+x')
    
    springloadedFiles = []
    new File("${grailsHome}/lib/com.springsource.springloaded/springloaded-core/jars/").eachFileMatch( groovy.io.FileType.FILES, { it ==~ /springloaded-core-.*/ }) {
        springloadedFiles << it
    }
    if(springloadedFiles.size() != 1) {
        if(springloadedFiles.size() == 0) {
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
