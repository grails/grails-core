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

package org.grails.cli.profile.commands

import grails.build.logging.GrailsConsole
import grails.io.IOUtils
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsNameUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.grails.build.logging.GrailsConsoleAntBuilder
import org.grails.build.parsing.CommandLine
import org.grails.cli.GrailsCli
import org.grails.cli.profile.*
import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource

/**
 * Command for creating Grails applications
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 3.0
 */
@CompileStatic
class CreateAppCommand extends ArgumentCompletingCommand implements ProfileRepositoryAware {
    private static final String GRAILS_VERSION_FALLBACK_IN_IDE_ENVIRONMENTS_FOR_RUNNING_TESTS ='3.0.0.BUILD-SNAPSHOT'
    public static final String NAME = "create-app"
    public static final String PROFILE_FLAG = "profile"
    public static final String FEATURES_FLAG = "features"
    public static final String ENCODING = System.getProperty("file.encoding") ?: "UTF-8"
    public static final String INPLACE_FLAG = "inplace"
    ProfileRepository profileRepository
    Map<String, String> variables = [:]
    String appname
    String groupname
    File targetDirectory
    List<String> binaryFileExtensions = ['png','gif','jpg','jpeg','ico','icns','pdf','zip','jar','class']

    CommandDescription description = new CommandDescription(name, "Creates an application", "create-app [NAME] --profile=web")

    CreateAppCommand() {
        populateDescription()
        description.flag(name: INPLACE_FLAG, description: "Used to create an application using the current directory")
        description.flag(name: PROFILE_FLAG, description: "The profile to use", required:false)
        description.flag(name: FEATURES_FLAG, description: "The features to use", required:false)
    }

    protected void populateDescription() {
        description.argument(name: "Application Name", description: "The name of the application to create.", required: false)
    }

    @Override
    String getName() {
        return NAME
    }

    @Override
    protected int complete(CommandLine commandLine, CommandDescription desc, List<CharSequence> candidates, int cursor) {
        def lastOption = commandLine.lastOption()
        if(lastOption != null) {
            // if value == true it means no profile is specified and only the flag is present
            def profileNames = profileRepository.allProfiles.collect() { Profile p -> p.name }
            if(lastOption.key == PROFILE_FLAG) {
                def val = lastOption.value
                if( val == true) {
                    candidates.addAll(profileNames)
                    return cursor
                }
                else if(!profileNames.contains(val)) {
                    def valStr = val.toString()

                    def candidateProfiles = profileNames.findAll { String pn ->
                        pn.startsWith(valStr)
                    }.collect() { String pn ->
                        "${pn.substring(valStr.size())} ".toString()
                    }
                    candidates.addAll candidateProfiles
                    return cursor
                }
            }
            else if(lastOption.key == FEATURES_FLAG) {
                def val = lastOption.value
                def profile = profileRepository.getProfile(commandLine.hasOption(PROFILE_FLAG) ? commandLine.optionValue(PROFILE_FLAG).toString() : getDefaultProfile())
                def featureNames = profile.features.collect() { Feature f -> f.name }
                if( val == true) {
                    candidates.addAll(featureNames)
                    return cursor
                }
                else if(!profileNames.contains(val)) {
                    def valStr = val.toString()
                    if(valStr.endsWith(',')) {
                        def specified = valStr.split(',')
                        candidates.addAll(featureNames.findAll { String f ->
                            !specified.contains(f)
                        })
                        return cursor
                    }

                    def candidatesFeatures = featureNames.findAll { String pn ->
                        pn.startsWith(valStr)
                    }.collect() { String pn ->
                        "${pn.substring(valStr.size())} ".toString()
                    }
                    candidates.addAll candidatesFeatures
                    return cursor
                }
            }
        }
        return super.complete(commandLine, desc, candidates, cursor)
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        if(profileRepository == null) throw new IllegalStateException("Property 'profileRepository' must be set")


        def mainCommandLine = executionContext.commandLine
        def profileName = evaluateProfileName(mainCommandLine)

        Profile profileInstance = profileRepository.getProfile(profileName)
        if( !validateProfile(profileInstance, profileName, executionContext)) {
            return false
        }
        List<Feature> features = evaluateFeatures(profileInstance, mainCommandLine).toList()
        if(profileInstance) {

            if( !initializeVariables(profileInstance, mainCommandLine) ) {
                return false
            }
            targetDirectory = mainCommandLine.hasOption('inplace') || GrailsCli.isInteractiveModeActive() ? new File(".").canonicalFile : new File(appname)
            File applicationYmlFile = new File(targetDirectory, "grails-app/conf/application.yml")

            def profiles = profileRepository.getProfileAndDependencies(profileInstance)
            for(Profile p : profiles) {
                String previousApplicationYml = (applicationYmlFile.isFile()) ? applicationYmlFile.getText(ENCODING) : null
                copySkeleton(profileInstance, p)

                if(applicationYmlFile.exists()) {
                    appendToYmlSubDocument(applicationYmlFile, previousApplicationYml)
                }
            }
            def ant = new GrailsConsoleAntBuilder()
            for(Feature f in features) {
                def location = f.location
                def featureConfig = location.createRelative("skeleton/grails-app/conf/application.yml")
                def featureBuild = location.createRelative("skeleton/build.gradle")

                if(applicationYmlFile.exists() && featureConfig.exists()) {
                    appendToYmlSubDocument(applicationYmlFile, featureConfig.inputStream.getText(ENCODING))
                }


                if(featureBuild.exists()) {
                    def buildFile = new File(targetDirectory, "build.gradle")
                    buildFile.text = buildFile.getText(ENCODING) + featureBuild.inputStream.getText(ENCODING)
                }

                File skeletonDir
                if(location instanceof FileSystemResource) {
                    skeletonDir = location.createRelative("skeleton").file
                }
                else {
                    File tmpDir = unzipProfile(ant, location)
                    skeletonDir = new File(tmpDir, "META-INF/grails-profile/features/$f.name/skeleton")
                }

                if(skeletonDir.exists()) {
                    copySrcToTarget(ant, skeletonDir, ['grails-app/conf/application.yml'])
                }
            }

            replaceBuildTokens(profileName, profileInstance, features, targetDirectory)
            executionContext.console.addStatus(
                "${name == 'create-plugin' ? 'Plugin' : 'Application'} created at $targetDirectory.absolutePath"
            )
            GrailsCli.tiggerAppLoad()
            return true
        }
        else {
            System.err.println "Cannot find profile $profileName"
            return false
        }
    }

    protected boolean validateProfile(Profile profileInstance, String profileName, ExecutionContext executionContext) {
        if (profileInstance == null) {
            executionContext.console.error("Profile not found for name [$profileName]")
            return false
        }
        return true
    }

    private Map<URL, File> unzippedDirectories = new LinkedHashMap<URL, File>()
    @CompileDynamic
    protected File unzipProfile(AntBuilder ant, Resource location) {

        def url = location.URL
        def tmpDir = unzippedDirectories.get(url)

        if(tmpDir == null) {
            def jarFile = IOUtils.findJarFile(url)
            tmpDir = File.createTempDir()
            tmpDir.deleteOnExit()
            ant.unzip(src: jarFile, dest: tmpDir)
            unzippedDirectories.put(url, tmpDir)
        }
        return tmpDir
    }

    @CompileDynamic
    protected void replaceBuildTokens(String profileCoords, Profile profile, List<Feature> features, File targetDirectory) {
        AntBuilder ant = new GrailsConsoleAntBuilder()
        def ln = System.getProperty("line.separator")

        def repositories = profile.repositories.collect() { String url ->
            "    maven { url \"${url}\" }".toString()
        }.unique().join(ln)

        def profileDependencies = profile.dependencies
        def dependencies = profileDependencies.findAll() { Dependency dep ->
            dep.scope != 'build'
        }
        def buildDependencies = profileDependencies.findAll() { Dependency dep ->
            dep.scope == 'build'
        }


        for(Feature f in features) {
            dependencies.addAll f.dependencies.findAll(){ Dependency dep -> dep.scope != 'build'}
            buildDependencies.addAll f.dependencies.findAll(){ Dependency dep -> dep.scope == 'build'}
        }

        if(profileCoords.contains(':')) {
            def art = new DefaultArtifact(profileCoords)
            def version = art.version ?: BuildSettings.grailsVersion
            if(version == 'LATEST') version = profile.getVersion()
            def finalArt = new DefaultArtifact(art.groupId ?: 'org.grails.profiles', art.artifactId, '', version)
            dependencies.add(new Dependency(finalArt, "profile"))
        }
        else {
            def art = new DefaultArtifact('org.grails.profiles', profile.name, '', profile.version)
            dependencies.add(new Dependency(art, "profile"))
        }
        dependencies = dependencies.unique()

        dependencies = dependencies.sort({ Dependency dep -> dep.scope }).collect() { Dependency dep ->
            String artifactStr = resolveArtifactString(dep)
            "    ${dep.scope} \"${artifactStr}\"".toString()
        }.unique().join(ln)

        def buildRepositories = profile.buildRepositories.collect() { String url ->
            "        maven { url \"${url}\" }".toString()
        }.unique().join(ln)

        buildDependencies = buildDependencies.collect() { Dependency dep ->
            String artifactStr = resolveArtifactString(dep)
            "        classpath \"${artifactStr}\"".toString()
        }.unique().join(ln)

        def buildPlugins = profile.buildPlugins.collect() { String name ->
            "apply plugin:\"$name\""
        }

        for(Feature f in features) {
            buildPlugins.addAll f.buildPlugins.collect() { String name ->
                "apply plugin:\"$name\""
            }
        }

        buildPlugins = buildPlugins.unique().join(ln)

        ant.replace(dir: targetDirectory) {
            replacefilter {
                replacetoken("@buildPlugins@")
                replacevalue(buildPlugins)
            }
            replacefilter {
                replacetoken("@dependencies@")
                replacevalue(dependencies)
            }
            replacefilter {
                replacetoken("@buildDependencies@")
                replacevalue(buildDependencies)
            }
            replacefilter {
                replacetoken("@buildRepositories@")
                replacevalue(buildRepositories)
            }
            replacefilter {
                replacetoken("@repositories@")
                replacevalue(repositories)
            }
            variables.each { k, v ->
                replacefilter {
                    replacetoken("@${k}@".toString())
                    replacevalue(v)
                }
            }
        }
    }

    protected String evaluateProfileName(CommandLine mainCommandLine) {
        mainCommandLine.optionValue('profile')?.toString() ?: getDefaultProfile()
    }

    protected Iterable<Feature> evaluateFeatures(Profile profile, CommandLine commandLine) {
        def requestedFeatures = commandLine.optionValue("features")?.toString()?.split(',')
        if(requestedFeatures) {
            def featureNames = Arrays.asList(requestedFeatures)
            return (profile.features.findAll() { Feature f -> featureNames.contains(f.name)} + profile.requiredFeatures).unique()
        }
        else {
            return (profile.defaultFeatures + profile.requiredFeatures).unique()
        }
    }

    protected String getDefaultProfile() {
        ProfileRepository.DEFAULT_PROFILE_NAME
    }


    private void appendToYmlSubDocument(File applicationYmlFile, String previousApplicationYml) {
        String newApplicationYml = applicationYmlFile.text
        if(previousApplicationYml && newApplicationYml != previousApplicationYml) {
            StringBuilder appended = new StringBuilder(previousApplicationYml.length() + newApplicationYml.length() + 30)
            if(!previousApplicationYml.startsWith("---")) {
                appended.append('---\n')
            }
            appended.append(previousApplicationYml).append("\n---\n")
            appended.append(newApplicationYml)
            applicationYmlFile.text = appended.toString()
        }
    }
    
    protected boolean initializeVariables(Profile profile, CommandLine commandLine) {
        String defaultPackage

        def args = commandLine.getRemainingArgs()
        boolean inPlace = commandLine.hasOption('inplace') || GrailsCli.isInteractiveModeActive()

        if(!args && !inPlace) {
            GrailsConsole.getInstance().error("Specify an application name or use --inplace to create an application in the current directory")
            return false
        }
        String groupAndAppName = args ? args[0] : null
        if(inPlace) {
            appname = new File(".").canonicalFile.name
            if(!groupAndAppName) {
                groupAndAppName = appname
            }
        }
        
        if(!groupAndAppName) {
            GrailsConsole.getInstance().error("Specify an application name or use --inplace to create an application in the current directory")
            return false
        }

        try {
            defaultPackage = establishGroupAndAppName(groupAndAppName)
        } catch (IllegalArgumentException e ) {
            GrailsConsole.instance.error(e.message)
            return false
        }


        variables.APPNAME = appname

        variables['grails.codegen.defaultPackage'] = defaultPackage
        variables['grails.codegen.defaultPackage.path']  = defaultPackage.replace('.', '/')

        def projectClassName = GrailsNameUtils.getNameFromScript(appname)
        variables['grails.codegen.projectClassName'] = projectClassName
        variables['grails.codegen.projectNaturalName'] = GrailsNameUtils.getNaturalName(projectClassName)
        variables['grails.codegen.projectName'] = GrailsNameUtils.getScriptName(projectClassName)
        variables['grails.profile'] = profile.name
        variables['grails.version'] = Environment.getPackage().getImplementationVersion() ?: GRAILS_VERSION_FALLBACK_IN_IDE_ENVIRONMENTS_FOR_RUNNING_TESTS
        variables['grails.app.name'] = appname
        variables['grails.app.group'] = groupname
    }

    private String establishGroupAndAppName(String groupAndAppName) {
        String defaultPackage
        List<String> parts = groupAndAppName.split(/\./) as List
        if (parts.size() == 1) {
            appname = parts[0]
            defaultPackage = createValidPackageName()
            groupname = defaultPackage
        } else {
            appname = parts[-1]
            groupname = parts[0..-2].join('.')
            defaultPackage = groupname
        }
        return defaultPackage
    }

    private String createValidPackageName() {
        String defaultPackage = appname.split(/[-]+/).collect { String token -> (token.toLowerCase().toCharArray().findAll  { char ch -> Character.isJavaIdentifierPart(ch) } as char[]) as String }.join('.')
        if(!GrailsNameUtils.isValidJavaPackage(defaultPackage)) {
            throw new IllegalArgumentException("Cannot create a valid package name for [$appname]. Please specify a name that is also a valid Java package.")
        }
        return defaultPackage
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copySkeleton(Profile profile, Profile participatingProfile) {

        def buildMergeProfileNames = profile.buildMergeProfileNames
        def excludes = profile.configuration.navigate("skeleton", "excludes") ?: []

        AntBuilder ant = new GrailsConsoleAntBuilder()

        def skeletonResource = participatingProfile.profileDir.createRelative("skeleton")
        File srcDir
        if(skeletonResource instanceof FileSystemResource) {
            srcDir = skeletonResource.file
        }
        else {
            // establish the JAR file name and extract
            def tmpDir = unzipProfile(ant, skeletonResource)
            srcDir = new File(tmpDir, "META-INF/grails-profile/skeleton")
        }
        ant.copy(file:"${srcDir}/.gitignore", todir: targetDirectory, failonerror:false)
        copySrcToTarget(ant, srcDir, excludes)


        def buildFile = new File(targetDirectory, "build.gradle")
        def srcBuildFile = new File(srcDir, "build.gradle")
        if(!buildFile.exists()) {
            if(srcBuildFile.exists()) {
                ant.copy file:srcBuildFile, tofile:buildFile
            }
        }
        else {
            if(srcBuildFile.exists() && buildMergeProfileNames.contains(participatingProfile.name)) {
                def concatFile = "${targetDirectory}/concat.gradle"
                ant.move(file:buildFile, tofile: concatFile)
                ant.concat destfile:buildFile, {
                   path {
                       pathelement location: concatFile
                       pathelement location:srcBuildFile
                   }
                }
                ant.delete(file:concatFile, failonerror: false)
            }
        }


        ant.chmod(file: "${targetDirectory}/gradlew", perm: 'u+x')
    }

    @CompileDynamic
    protected void copySrcToTarget(GrailsConsoleAntBuilder ant, File srcDir, List excludes) {
        ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
            fileSet(dir: srcDir, casesensitive: false) {
                exclude(name: '**/.gitkeep')
                for (exc in excludes) {
                    exclude name: exc
                }
                exclude name: "build.gradle"
                binaryFileExtensions.each { ext ->
                    exclude(name: "**/*.${ext}")
                }
            }
            filterset {
                variables.each { k, v ->
                    filter(token: k, value: v)
                }
            }
            mapper {
                filtermapper {
                    variables.each { k, v ->
                        replacestring(from: "@${k}@".toString(), to: v)
                    }
                }
            }
        }
        ant.copy(todir: targetDirectory, overwrite: true) {
            fileSet(dir: srcDir, casesensitive: false) {
                binaryFileExtensions.each { ext ->
                    include(name: "**/*.${ext}")
                }
                for (exc in excludes) {
                    exclude name: exc
                }
                exclude name: "build.gradle"
            }
            mapper {
                filtermapper {
                    variables.each { k, v ->
                        replacestring(from: "@${k}@".toString(), to: v)
                    }
                }
            }
        }
    }

    protected String resolveArtifactString(Dependency dep) {
        def artifact = dep.artifact
        def v = artifact.version.replace('BOM', '')

        return v ? "${artifact.groupId}:${artifact.artifactId}:${v}" : "${artifact.groupId}:${artifact.artifactId}"
    }
}
