/*
 * Copyright 2004-2005 the original author or authors.
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

import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.io.*
import org.tmatesoft.svn.core.*
import org.tmatesoft.svn.core.auth.*
import org.tmatesoft.svn.core.wc.*
import org.codehaus.groovy.grails.documentation.MetadataGeneratingMetaClassCreationHandle
import org.codehaus.groovy.grails.plugins.publishing.DefaultPluginPublisher
import org.springframework.core.io.FileSystemResource
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

/**
 * Gant script that handles releasing plugins to a plugin repository.
 *
 * @author Graeme Rocher
 */

includeTargets << grailsScript("_GrailsPluginDev")
includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsDocs")

authManager = null
commitMessage = null
trunk = null
latestRelease = null
versionedRelease = null
skipLatest = false

CORE_PLUGIN_DIST = "http://svn.codehaus.org/grails/trunk/grails-plugins"
CORE_PUBLISH_URL = "https://svn.codehaus.org/grails/trunk/grails-plugins"
DEFAULT_PLUGIN_DIST = "http://plugins.grails.org"
DEFAULT_PUBLISH_URL = "https://svn.codehaus.org/grails-plugins"

// setup default plugin respositories for discovery
pluginDiscoveryRepositories = [core:CORE_PLUGIN_DIST, default:DEFAULT_PLUGIN_DIST]
if (grailsSettings?.config?.grails?.plugin?.repos?.discovery) {
    pluginDiscoveryRepositories.putAll(grailsSettings?.config?.grails?.plugin?.repos?.discovery)
}

// setup default plugin respositories for publishing
pluginDistributionRepositories = [core:CORE_PUBLISH_URL, default:DEFAULT_PUBLISH_URL]
if (grailsSettings?.config?.grails?.plugin?.repos?.distribution) {
    pluginDistributionRepositories.putAll(grailsSettings?.config?.grails?.plugin?.repos?.distribution)
}

KEY_URL = "url"
KEY_USER_NAME = "user"
KEY_USER_PASS = "pswd"

/**
 * authentication manager Map
 * EG : INIT CORE OR DEFAULT DISTRIBUTION REPOSITORY
 * aMap = [url:CORE_PUBLISH_URL,user:"auser",pswd:"apswd"]
 * ALLOWS ANT PROPERTIES DEFINITION
 * aAuthManager = getAuthenticationManager("core", "distribution", aMap)
 * authManagerMap.put("distribution.core", aAuthManager)
 */
authManagerMap = [:]

/**
 * Break down url into separate authentication components from url
 * @param url to be parsed
 * @return broken down url in these parts (if exist) url, user and pswd
 */
public Map tokenizeUrl(String url) throws SVNException {
    SVNURL aURL= SVNURL.parseURIDecoded(url)
    def aHashMap  = [:]

    def userInfo = aURL.userInfo
    if (userInfo) {
        def userInfoArray = userInfo.split(":")
        aHashMap[KEY_USER_NAME] = userInfoArray[0]
        if (userInfoArray.length>1) {
            aHashMap[KEY_USER_PASS] = userInfoArray[1]
        }
    }
    if (aURL.port == 443) {
        aHashMap[KEY_URL] = "${aURL.protocol}://${aURL.host}${aURL.path}".toString()
    }
    else {
        aHashMap[KEY_URL] = "${aURL.protocol}://${aURL.host}:${aURL.port}${aURL.path}".toString()
    }

    return aHashMap
}

//init SVN Kit one time
// support file based SVN
FSRepositoryFactory.setup()
// support the server http/https
DAVRepositoryFactory.setup()
// support svn protocol
SVNRepositoryFactoryImpl.setup()

/**
 * Replace the url with authentication by url without in discovery and distribution
 * repository and setup authentication instance in authManagerMap.
 * if an url like :
 * {protocol}://{user:password}@url is defined the promt does not occur.
 * Else the user is prompted for user and password values.
 * The repos "core" and "default" are ignored (see above for default configuration)
 * For all other repo, a defultAuthenticationManager is created.
 */
public Map configureAuth(Map repoMap,String repoType) {
    repoMapTmp = [:]
    repoMap.each {
        ISVNAuthenticationManager aAuthManager = SVNWCUtil.createDefaultAuthenticationManager()
        if ("core" == it.key || "default" == it.key) {
            repoMapTmp[it.key] = it.value
            if (!isSecureUrl(it.value)) {
                authManagerMap[repoType+"."+it.key] = aAuthManager
            }
        // else no authentication manager provides to authManagerMap : case of CORE_PUBLISH_URL and DEFAULT_PUBLISH_URL
        }
        else {
            if (isSecureUrl(it.value)) {
                event "StatusUpdate", ["Authentication for svn repo at ${it.key} ${repoType} is required."]
                aMap = tokenizeUrl(it.value)
                repoMapTmp[it.key] = aMap[KEY_URL]
                aAuthManager = getAuthenticationManager(it.key, repoType, aMap)
                authManagerMap[repoType + "." + it.key] = aAuthManager
            }
            else {
                repoMapTmp[it.key] = it.value
                event "StatusUpdate", ["No authentication for svn repo at ${it.key}"]
                authManagerMap[repoType + "." + it.key] = aAuthManager
            }
        }
    }

    return repoMapTmp
}

//configure authentication for discovery repository
pluginDiscoveryRepositories = configureAuth(pluginDiscoveryRepositories, "discovery")

//configure authentication for distribution repository
pluginDistributionRepositories = configureAuth(pluginDistributionRepositories, "distribution")

/**
 * Provide an authentication manager object.
 * This method tries to use the configuration in settings.groovy
 * (see comment of configureAuth for configuration pattern). If
 * there's no configuration the user is prompted.
 * @param repoKey
 * @param repoType discovery or distribution
 * @param tokenizeUrl the broken down url
 * @return an ISVNAuthenticationManager impl instance building
 */
private ISVNAuthenticationManager getAuthenticationManager(String repoKey, String repoType, Map tokenizeUrl) {
    ISVNAuthenticationManager aAuthManager
    usr = "user.svn.username.${repoType}.${repoKey}".toString()
    psw = "user.svn.password.${repoType}.${repoKey}".toString()
    if (tokenizeUrl[KEY_USER_NAME]) {
        ant.antProject.setNewProperty usr, ""+tokenizeUrl[KEY_USER_NAME]
        String pswd = tokenizeUrl[KEY_USER_PASS] ?: ""
        ant.antProject.setNewProperty(psw, pswd)
    }
    //If no provided info, the user have to be prompt
    ant.input(message:"Please enter your SVN username:", addproperty:usr)
    ant.input(message:"Please enter your SVN password:", addproperty:psw)
    def username = ant.antProject.getProperty(usr)
    def password = ant.antProject.getProperty(psw)
    authManager = SVNWCUtil.createDefaultAuthenticationManager(username , password)
    //Test connection
    aUrl = tokenizeUrl[KEY_URL]
    def svnUrl = SVNURL.parseURIEncoded(aUrl)
    def repo = SVNRepositoryFactory.create(svnUrl, null)
    repo.authenticationManager = authManager
    try {
        repo.testConnection()
        //only if it works...
        repo.closeSession()
    }
    catch (SVNAuthenticationException ex) {
        //GRAVE BAD CONFIGURATION :  EXITING
        event("StatusError",["Bad authentication configuration for $aUrl"])
        exit(1)
    }
    return authManager
}

/**
 * Get an authenticationManager object starting from url
 * @param url of SVN repo
 * @param repoType discovery or distribution
 * @return ISVNAuthenticationManager for SVN connection on on repo with auth
 **/
private ISVNAuthenticationManager getAuthFromUrl(url, repoType) {
    keyValue = repoType + "." + pluginDiscoveryRepositories.find { url.startsWith(it.value) }?.key
    return authManagerMap[keyValue]
}

configureRepository =  { targetRepoURL, String alias = "default" ->
    repositoryName = alias
    pluginsList = null
    pluginsListFile = new File(grailsSettings.grailsWorkDir, "plugins-list-${alias}.xml")

    def namedPluginSVN = pluginDistributionRepositories.find { it.key == alias }?.value
    if (namedPluginSVN) {
        pluginSVN = namedPluginSVN
    }
    else {
        pluginSVN = DEFAULT_PUBLISH_URL
    }
    pluginDistURL = targetRepoURL
    pluginBinaryDistURL = "$targetRepoURL/dist"
    remotePluginList = "$targetRepoURL/.plugin-meta/plugins-list.xml"
}

configureRepository(DEFAULT_PLUGIN_DIST)

configureRepositoryForName = { String targetRepository, type="discovery" ->
    // Works around a bug in Groovy 1.5.6's DOMCategory that means get on Object returns null. Change to "pluginDiscoveryRepositories.targetRepository" when upgrading
    def targetRepoURL = pluginDiscoveryRepositories.find { it.key == targetRepository }?.value

    if (targetRepoURL) {
        configureRepository(targetRepoURL, targetRepository)
    }
    else {
        println "No repository configured for name ${targetRepository}. Set the 'grails.plugin.repos.${type}.${targetRepository}' variable to the location of the repository."
        exit(1)
    }
}

target ('default': "A target for plug-in developers that uploads and commits the current plug-in as the latest revision. The command will prompt for your SVN login details.") {
    releasePlugin()
}

target(processAuth:"Prompts user for login details to create authentication manager") {
    if (!authManager) {
        def authKey = argsMap.repository ? "distribution.${argsMap.repository}".toString() : "distribution.default"
        if (authManagerMap[authKey]) {
            authManager = authManagerMap[authKey]
        }
        else {
            usr = "user.svn.username.distribution"
            psw = "user.svn.password.distribution"
            if (argsMap.repository) {
                usr = usr + "." + argsMap.repository
                psw = psw + "." + argsMap.repository
            }
            else {
                usr = usr + ".default"
                psw = psw + ".default"
            }

            def (username, password) = [argsMap.username, argsMap.password]

            if (!username) {
                ant.input(message:"Please enter your SVN username:", addproperty:usr)
                username = ant.antProject.getProperty(usr)
            }

            if (!password) {
                ant.input(message:"Please enter your SVN password:", addproperty:psw)
                password = ant.antProject.getProperty(psw)
            }

            authManager = SVNWCUtil.createDefaultAuthenticationManager(username , password)
            authManagerMap.put(authKey,authManager)
        }
    }
}

target(checkLicense:"Checks the license file for the plugin exists") {
    if (!(new File("${basedir}/LICENSE").exists()) && !(new File("${basedir}/LICENSE.txt").exists())) {
        println "No LICENSE.txt file for plugin found. Please provide a license file containing the appropriate software licensing information (eg. Apache 2.0, GPL etc.)"
        exit(1)
    }
}

target(releasePlugin: "The implementation target") {
    depends(parseArguments,checkLicense)

    if (argsMap.skipMetadata != true) {
        println "Generating plugin project behavior metadata..."
        try {
            MetadataGeneratingMetaClassCreationHandle.enable()
            packageApp()
            loadApp()
            configureApp()
            MetadataGeneratingMetaClassCreationHandle.disable()
        }
        catch (e) {
            println "There was an error generating project behavior metadata: [${e.message}]"
        }
    }
    packagePlugin()
    if (argsMap.skipDocs != true) {
        docs()
    }

    if (argsMap.packageOnly) {
        return
    }
    processAuth()

    if (argsMap.message) {
        commitMessage = argsMap.message
    }

    if (argsMap.repository) {
        configureRepositoryForName(argsMap.repository, "distribution")
    }
    if (argsMap.snapshot || argsMap.'skip-latest') {
        skipLatest = true
    }
    remoteLocation = "${pluginSVN}/grails-${pluginName}"
    trunk = SVNURL.parseURIDecoded("${remoteLocation}/trunk")
    latestRelease = "${remoteLocation}/tags/LATEST_RELEASE"
    versionedRelease = "${remoteLocation}/tags/RELEASE_${plugin.version.toString().replaceAll('\\.','_')}"

    FSRepositoryFactory.setup()
    DAVRepositoryFactory.setup()
    SVNRepositoryFactoryImpl.setup()

    try {
        if (argsMap.pluginlist) {
            commitNewGlobalPluginList()
        }
        else if (argsMap.zipOnly) {
            publishZipOnlyRelease()
        }
        else {
            def statusClient = new SVNStatusClient((ISVNAuthenticationManager)authManager,null)

            try {
                // get status of base directory, if this fails exception will be thrown
                statusClient.doStatus(baseFile, true)
                updateAndCommitLatest()
            }
            catch (SVNException ex) {
                // error with status, not in repo, attempt import.
                if (ex.message.contains("is not a working copy")) {
                    boolean notInRepository = isPluginNotInRepository()
                    if (notInRepository) {
                        importToSVN()
                    }
                    else {
                        def result = confirmInput("""
The current directory is not a working copy and your latest changes won't be committed.
You need to checkout a working copy and make your changes there.
Alternatively, would you like to publish a zip-only (no sources) release?""")
                        if (!result) exit(0)

                        publishZipOnlyRelease()
                        return
                    }
                }
                else {
                    event('StatusFinal', ["Failed to stat working directory: ${ex.message}"])
                    exit(1)
                }
            }
            tagPluginRelease()
            modifyOrCreatePluginList()
            event('StatusFinal', ["Plug-in release successfully published"])
        }
    }
    catch(Exception e) {
        logErrorAndExit("Error occurred with release-plugin", e)
    }
}

def publishZipOnlyRelease() {
    def localWorkingCopy = new File("${projectWorkDir}/working-copy")
    ant.mkdir(dir: localWorkingCopy)

    if (isPluginNotInRepository()) {
        updateLocalZipAndXml(localWorkingCopy)
        importBaseToSVN(localWorkingCopy)
    }
    cleanLocalWorkingCopy(localWorkingCopy)
    checkoutFromSVN(localWorkingCopy, trunk)
    updateLocalZipAndXml(localWorkingCopy)
    addPluginZipAndMetadataIfNeccessary(new File("${localWorkingCopy}/plugin.xml"), new File("${localWorkingCopy}/${new File(pluginZip).name}"))
    commitDirectoryToSVN(localWorkingCopy)

    tagPluginRelease()
    modifyOrCreatePluginList()
	deleteZipFromTrunk()
    println "Successfully published zip-only plugin release."
}

def deleteZipFromTrunk() {
    def commitClient = new SVNCommitClient((ISVNAuthenticationManager) authManager, null)

    if (!commitMessage) askForMessage()
	if(pluginZip) {
		def pluginZipFile = new File(pluginZip)
		def zipLocation = SVNURL.parseURIDecoded("${remoteLocation}/trunk/${pluginZipFile.name}")
	    try { commitClient.doDelete([zipLocation] as SVNURL[], commitMessage) }
	    catch (SVNException e) {
	        // ok - the zip doesn't exist yet
	    }			
	}
}

def updateLocalZipAndXml(File localWorkingCopy) {
    ant.copy(file: pluginZip, todir: localWorkingCopy, overwrite:true)
    ant.copy(file: "${basedir}/plugin.xml", todir: localWorkingCopy, overwrite:true)
}

def cleanLocalWorkingCopy(File localWorkingCopy) {
    ant.delete(dir: localWorkingCopy)
    ant.mkdir(dir: localWorkingCopy)
}

boolean isPluginNotInRepository() {
    // Now check whether the plugin is in the repository.
    // If not, we ask the user whether they want to import it.
    SVNRepository repos = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(pluginSVN))
    if(authManager != null)
        repos.authenticationManager = authManager
    boolean notInRepository = true
    try {
        notInRepository = !repos.info("grails-$pluginName", -1)
    }
    catch (e) {
        // ignore
    }
    return notInRepository
}

target(modifyOrCreatePluginList:"Updates the remote plugin.xml descriptor or creates a new one in the repo") {
    withPluginListUpdate {
        ant.delete(file:pluginsListFile)
        // get newest version of plugin list
        try {
            fetchRemoteFile("${pluginSVN}/.plugin-meta/plugins-list.xml", pluginsListFile)
        }
        catch (Exception e) {
            println "Error reading remote plugin list [${e.message}], building locally..."
            updatePluginsListManually()
        }

        def remoteRevision = "0"
        if (shouldUseSVNProtocol(pluginDistURL)) {
            withSVNRepo(pluginDistURL) { repo ->
                remoteRevision = repo.getLatestRevision().toString()
            }
        }
        else {
            new URL(pluginDistURL).withReader { Reader reader ->
                def line = reader.readLine()
                line.eachMatch(/Revision (.*):/) {
                    remoteRevision = it[1]
                }
            }
        }

        def publisher = new DefaultPluginPublisher(remoteRevision, pluginDistURL)
        def updatedList = publisher.publishRelease(pluginName, new FileSystemResource(pluginsListFile), !skipLatest)
        pluginsListFile.withWriter("UTF-8") { w ->
            publisher.writePluginList(updatedList, w)
        }
    }
}

target(commitNewGlobalPluginList:"updates the plugins.xml descriptor stored in the repo") {
    withPluginListUpdate {
        ant.delete(file:pluginsListFile)
        println "Building plugin list for commit..."
        updatePluginsListManually()
    }
}

private withPluginListUpdate(Closure updateLogic) {
    if (!commitMessage) {
        askForMessage()
    }
    updateLogic()

    def pluginMetaDir = new File("${grailsSettings.grailsWorkDir}/${repositoryName}/.plugin-meta")
    def updateClient = new SVNUpdateClient((ISVNAuthenticationManager)authManager, null)
    def importClient = new SVNCommitClient((ISVNAuthenticationManager)authManager, null)
    def addClient = new SVNWCClient((ISVNAuthenticationManager)authManager, null)

    String remotePluginMetadata = "${pluginSVN}/.plugin-meta"
    if (!pluginMetaDir.exists()) {
        println "Checking out locally to '${pluginMetaDir}'."
        checkoutOrImportPluginMetadata(pluginMetaDir, remotePluginMetadata, updateClient, importClient)
    }
    else {
        try {
            updateClient.doUpdate(pluginMetaDir, SVNRevision.HEAD, true)
            commitNewestPluginList(pluginMetaDir, importClient)
        }
        catch (SVNException e) {
            println "Plugin meta directory corrupt, checking out again"
            checkoutOrImportPluginMetadata(pluginMetaDir, remotePluginMetadata, updateClient, importClient)
        }
    }
}

private checkoutOrImportPluginMetadata (File pluginMetaDir, String remotePluginMetadata, SVNUpdateClient updateClient, SVNCommitClient importClient) {
    def svnURL = SVNURL.parseURIDecoded (remotePluginMetadata)
    try {
        updateClient.doCheckout(svnURL, pluginMetaDir, SVNRevision.HEAD, SVNRevision.HEAD, true)

        try {
            addClient.doAdd(new File("$pluginMetaDir/plugins-list.xml"), false, false, false, false)
        }
        catch (e) {
        // ignore
        }
        commitNewestPluginList(pluginMetaDir, importClient)
    }
    catch (SVNException e) {
        println "Importing plugin meta data to ${remotePluginMetadata}. Please wait..."

        ant.mkdir(dir: pluginMetaDir)
        ant.copy(file: pluginsListFile, tofile: "$pluginMetaDir/plugins-list.xml")

        def commit = importClient.doImport(pluginMetaDir, svnURL, commitMessage, true)
        println "Committed revision ${commit.newRevision} of plugins-list.xml."
        ant.delete(dir: pluginMetaDir)
        updateClient.doCheckout(svnURL, pluginMetaDir, SVNRevision.HEAD, SVNRevision.HEAD, true)
    }
}

private def commitNewestPluginList(File pluginMetaDir, SVNCommitClient importClient) {
    ant.copy(file: pluginsListFile, tofile: "$pluginMetaDir/plugins-list.xml", overwrite: true)

    def commit = importClient.doCommit([pluginMetaDir] as File[], false, commitMessage, true, true)

    println "Committed revision ${commit.newRevision} of plugins-list.xml."
}

target(checkInPluginZip:"Checks in the plug-in zip if it has not been checked in already") {

    def pluginXml = new File("${basedir}/plugin.xml")

    addPluginZipAndMetadataIfNeccessary(pluginXml, new File(pluginZip))
}

def addPluginZipAndMetadataIfNeccessary(File pluginXml, File pluginFile) {
    def statusClient = new SVNStatusClient((ISVNAuthenticationManager) authManager, null)
    def wcClient = new SVNWCClient((ISVNAuthenticationManager) authManager, null)

    def addPluginFile = false
    try {
        def status = statusClient.doStatus(pluginFile, true)
        if (status.kind == SVNNodeKind.NONE || status.kind == SVNNodeKind.UNKNOWN) addPluginFile = true
    }
    catch (SVNException) {
        // not checked in add and commit
        addPluginFile = true
    }
    if (addPluginFile) wcClient.doAdd(pluginFile, true, false, false, false)
    addPluginFile = false
    try {
        def status = statusClient.doStatus(pluginXml, true)
        if (status.kind == SVNNodeKind.NONE || status.kind == SVNNodeKind.UNKNOWN) addPluginFile = true
    }
    catch (SVNException e) {
        addPluginFile = true
    }
    if (addPluginFile) wcClient.doAdd(pluginXml, true, false, false, false)
}

target(updateAndCommitLatest:"Commits the latest revision of the Plug-in") {
   def result = !isInteractive || confirmInput("""
This command will perform the following steps to release your plug-in to Grails' SVN repository:
* Update your sources to the HEAD revision
* Commit any changes you've made to SVN
* Tag the release

NOTE: This command will not add new resources for you, if you have additional sources to add please run 'svn add' before running this command.
NOTE: Make sure you have updated the version number in your *GrailsPlugin.groovy descriptor.

Are you sure you wish to proceed?
""")
    if (!result) exit(0)

    println "Checking in plugin zip..."
    checkInPluginZip()

    long r = updateDirectoryFromSVN(baseFile)
    println "Updated to revision ${r}. Committing local, please wait..."

    def commit = commitDirectoryToSVN(baseFile)

    println "Committed revision ${commit.newRevision}."
}

def commitDirectoryToSVN(baseFile) {
    commitClient = new SVNCommitClient((ISVNAuthenticationManager) authManager, null)

    if (!commitMessage) askForMessage()

    println "Committing code. Please wait..."

    return commitClient.doCommit([baseFile] as File[], false, commitMessage, true, true)
}

long updateDirectoryFromSVN(baseFile) {
    updateClient = new SVNUpdateClient((ISVNAuthenticationManager) authManager, null)

    println "Updating from SVN..."
    long r = updateClient.doUpdate(baseFile, SVNRevision.HEAD, true)
    return r
}

target(importToSVN:"Imports a plugin project to Grails' remote SVN repository") {
    File checkOutDir = new File("${baseFile.parentFile.absolutePath}/checkout/${baseFile.name}")

    ant.unzip(src:pluginZip, dest:"${basedir}/unzipped")
    ant.copy(file:pluginZip, todir:"${basedir}/unzipped")
    ant.mkdir(dir:"${basedir}/unzipped/grails-app")

    File importBaseDirectory = new File("${basedir}/unzipped")

    String testsDir = "${importBaseDirectory}/test"
    ant.mkdir(dir:testsDir)
    ant.copy(todir:testsDir) {
        fileset(dir:"${grailsSettings.testSourceDir}")
    }

    try {
        def result = !isInteractive || confirmInput("""
    This plug-in project is not currently in the repository, this command will now:
    * Perform an SVN import into the repository
    * Checkout the imported version of the project from SVN to '${checkOutDir}'
    * Tag the plug-in project as the LATEST_RELEASE
    Are you sure you wish to proceed?
        """)
        if (!result) {
            ant.delete(dir:importBaseDirectory, failonerror:false)
            exit(0)
        }
        svnURL = importBaseToSVN(importBaseDirectory)
    }
    finally {
        ant.delete(dir:importBaseDirectory, failonerror:false)
    }
    checkOutDir.parentFile.mkdirs()

    checkoutFromSVN(checkOutDir, svnURL)

    event('StatusFinal', ["""
Completed SVN project import. If you are in terminal navigate to imported project with:
cd ${checkOutDir}

Future changes should be made to the SVN controlled sources!"""])
}

def checkoutFromSVN(File checkOutDir, SVNURL svnURL) {
    updateClient = new SVNUpdateClient((ISVNAuthenticationManager) authManager, null)
    println "Checking out locally to '${checkOutDir}'."
    updateClient.doCheckout(svnURL, checkOutDir, SVNRevision.HEAD, SVNRevision.HEAD, true)
}

SVNURL importBaseToSVN(File importBaseDirectory) {
    importClient = new SVNCommitClient((ISVNAuthenticationManager) authManager, null)
    if (!commitMessage) askForMessage()

    println "Importing project to ${remoteLocation}. Please wait..."

    def svnURL = SVNURL.parseURIDecoded("${remoteLocation}/trunk")
    importClient.doImport(importBaseDirectory, svnURL, commitMessage, true)
    println "Plug-in project imported to SVN at location '${remoteLocation}/trunk'"
    return svnURL
}

target(tagPluginRelease:"Tags a plugin-in with the LATEST_RELEASE tag and version tag within the /tags area of SVN") {
    println "Preparing to publish the release..."

    def copyClient = new SVNCopyClient((ISVNAuthenticationManager) authManager, null)
    def commitClient = new SVNCommitClient((ISVNAuthenticationManager) authManager, null)

    if (!commitMessage) askForMessage()

    tags = SVNURL.parseURIDecoded("${remoteLocation}/tags")
    latest = SVNURL.parseURIDecoded(latestRelease)
    release = SVNURL.parseURIDecoded(versionedRelease)

    try { commitClient.doMkDir([tags] as SVNURL[], commitMessage) }
    catch (SVNException e) {
    // ok - already exists
    }
    if (!skipLatest) {
        try { commitClient.doDelete([latest] as SVNURL[], commitMessage) }
        catch (SVNException e) {
            // ok - the tag doesn't exist yet
        }
    }
    try { commitClient.doDelete([release] as SVNURL[], commitMessage) }
    catch (SVNException e) {
        // ok - the tag doesn't exist yet
    }

    // Get remote URL for this working copy.
    def wcClient = new SVNWCClient((ISVNAuthenticationManager) authManager, null)
    def copyFromUrl = trunk

    // First tag this release with the version number.
    try {
        println "Tagging version release, please wait..."
        def copySource = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, copyFromUrl)
        def commit = copyClient.doCopy([copySource] as SVNCopySource[], release, false, false, true, commitMessage, new SVNProperties())
        println "Copied trunk to ${versionedRelease} with revision ${commit.newRevision} on ${commit.date}"

        // And now make it the latest release.
        if (!skipLatest) {
            println "Tagging latest release, please wait..."
            copySource = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, release)
            commit = copyClient.doCopy([copySource] as SVNCopySource[], latest, false, false, true, commitMessage, new SVNProperties())
            println "Copied trunk to ${latestRelease} with revision ${commit.newRevision} on ${commit.date}"
        }
    }
    catch (SVNException e) {
        logErrorAndExit("Error tagging release", e)
    }
}

target(askForMessage:"Asks for the users commit message") {
    ant.input(message:"Enter a SVN commit message:", addproperty:"commit.message")
    commitMessage = ant.antProject.properties."commit.message"
}

target(updatePluginsListManually: "Updates the plugin list by manually reading each URL, the slow way") {
    depends(configureProxy)
    try {
        def recreateCache = false
        document = null
        if (!pluginsListFile.exists()) {
            println "Plugins list cache doesn't exist creating.."
            recreateCache = true
        }
        else {
            try {
                document = DOMBuilder.parse(new FileReader(pluginsListFile))
            }
            catch (Exception e) {
                recreateCache = true
                println "Plugins list cache is corrupt [${e.message}]. Re-creating.."
            }
        }
        if (recreateCache) {
            document = DOMBuilder.newInstance().createDocument()
            def root = document.createElement('plugins')
            root.setAttribute('revision', '0')
            document.appendChild(root)
        }

        pluginsList = document.documentElement
        builder = new DOMBuilder(document)

        def localRevision = pluginsList ? new Integer(pluginsList.getAttribute('revision')) : -1
        // extract plugins svn repository revision - used for determining cache up-to-date
        def remoteRevision = 0
        try {
            // determine if this is a secure plugin spot..
            if (shouldUseSVNProtocol(pluginDistURL)) {
                withSVNRepo(pluginDistURL) { repo ->
                    remoteRevision = repo.getLatestRevision()
                    if (remoteRevision > localRevision) {
                        // Plugins list cache is expired, need to update
                        event("StatusUpdate", ["Plugins list cache has expired. Updating, please wait"])
                        pluginsList.setAttribute('revision', remoteRevision as String)
                        repo.getDir('', -1,null,(Collection)null).each() { entry ->
                            final String PREFIX = "grails-"
                            if (entry.name.startsWith(PREFIX)) {
                                def pluginName = entry.name.substring(PREFIX.length())
                                buildPluginInfo(pluginsList, pluginName)
                            }
                        }
                    }
                }
            }
            else {
                new URL(pluginDistURL).withReader { Reader reader ->
                    def line = reader.readLine()
                    line.eachMatch(/Revision (.*):/) {
                        remoteRevision = it[1].toInteger()
                    }
                    if (remoteRevision > localRevision) {
                        // Plugins list cache is expired, need to update
                        event("StatusUpdate", ["Plugins list cache has expired. Updating, please wait"])
                        pluginsList.setAttribute('revision', remoteRevision as String)
                        // for each plugin directory under Grails Plugins SVN in form of 'grails-*'
                        while (line = reader.readLine()) {
                            line.eachMatch(/<li><a href="grails-(.+?)">/) {
                                // extract plugin name
                                def pluginName = it[1][0..-2]
                                // collect information about plugin
                                buildPluginInfo(pluginsList, pluginName)
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            event("StatusError", ["Unable to list plugins, please check you have a valid internet connection: ${e.message}" ])
        }

        // update plugins list cache file
        writePluginsFile()
    }
    catch (Exception e) {
        event("StatusError", ["Unable to list plugins, please check you have a valid internet connection: ${e.message}" ])
    }
}

def buildPluginInfo(root, pluginName) {
    use(DOMCategory) {
        // determine the plugin node..
        def pluginNode = root.'plugin'.find {it.'@name' == pluginName}
        if (!pluginNode) {
            // add it if it doesn't exist..
            pluginNode = builder.'plugin'(name: pluginName)
            root.appendChild(pluginNode)
        }

        // add each of the releases..
        event("StatusUpdate", ["Reading [$pluginName] plugin info"])
        // determine if this is a secure plugin spot..
        def tagsUrl = "${pluginDistURL}/grails-${pluginName}/tags"
        try {
            if (shouldUseSVNProtocol(pluginDistURL)) {
                withSVNRepo(tagsUrl) { repo ->
                    repo.getDir('',-1,null,(Collection)null).each() { entry ->
                        buildReleaseInfo(pluginNode, pluginName, tagsUrl, entry.name)
                    }
                }
            }
            else {
                def releaseTagsList = new URL(tagsUrl).text
                releaseTagsList.eachMatch(/<li><a href="(.+?)">/) {
                    def releaseTag = it[1][0..-2]
                    buildReleaseInfo(pluginNode, pluginName, tagsUrl, releaseTag)
                }
            }
        }
        catch(e) {
            // plugin has not tags
            println "Plugin [$pluginName] doesn't have any tags"
        }

        try {
            def latestRelease = null
            def url = "${pluginDistURL}/grails-${pluginName}/tags/LATEST_RELEASE/plugin.xml"
            fetchPluginListFile(url).withReader {Reader reader ->
                def line = reader.readLine()
                line.eachMatch (/.+?version='(.+?)'.+/) {
                    latestRelease = it[1]
                }
            }
            if (latestRelease && pluginNode.'release'.find {it.'@version' == latestRelease}) {
                pluginNode.setAttribute('latest-release', latestRelease as String)
            }
        }
        catch(e) {
            // plugin doesn't have a latest release
            println "Plugin [$pluginName] doesn't have a latest release"
        }
    }
}

def buildReleaseInfo(root, pluginName, releasePath, releaseTag) {
    // quick exit nothing to do..
    if (releaseTag == '..' || releaseTag == 'LATEST_RELEASE') {
        return
    }

    // remove previous version..
    def releaseNode = root.'release'.find() {
        it.'@tag' == releaseTag && it.'&type' == 'svn'
    }
    if (releaseNode) {
        root.removeChild(releaseNode)
    }
    try {

        // copy the properties to the new node..
        def releaseUrl = "${releasePath}/${releaseTag}"
        def properties = ['title', 'author', 'authorEmail', 'description', 'documentation']
        def releaseDescriptor = parseRemoteXML("${releaseUrl}/plugin.xml").documentElement
        def version = releaseDescriptor.'@version'

        releaseNode = builder.createNode('release', [tag: releaseTag, version: version, type: 'svn'])
        root.appendChild(releaseNode)
        properties.each {
            if (releaseDescriptor."${it}") {
                releaseNode.appendChild(builder.createNode(it, releaseDescriptor."${it}".text()))
            }
        }
        releaseNode.appendChild(builder.createNode('file', "${releaseUrl}/grails-${pluginName}-${version}.zip"))
    }
    catch(e) {
        // no release info available, probably an older plugin with no plugin.xml defined.
    }
}

def parsePluginList() {
    if (pluginsList == null) {
        profile("Reading local plugin list from $pluginsListFile") {
            def document
            try {
                document = DOMBuilder.parse(new FileReader(pluginsListFile))
            }
            catch (Exception e) {
                println "Plugin list file corrupt, retrieving again.."
                readRemotePluginList()
                document = DOMBuilder.parse(new FileReader(pluginsListFile))
            }
            pluginsList = document.documentElement
        }
    }
}

def readRemotePluginList() {
    ant.delete(file:pluginsListFile, failonerror:false)
    ant.mkdir(dir:pluginsListFile.parentFile)
    fetchRemoteFile(remotePluginList, pluginsListFile)
}

def writePluginsFile() {
    pluginsListFile.parentFile.mkdirs()

    Transformer transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "true")
    transformer.transform(new DOMSource(document), new StreamResult(pluginsListFile))
}

shouldUseSVNProtocol = { pluginDistURL ->
    return isSecureUrl(pluginDistURL) || pluginDistURL.startsWith("file://")
}

def parseRemoteXML(url) {
    fetchPluginListFile(url).withReader() { DOMBuilder.parse(it) }
}

/**
 * Downloads a remote plugin zip into the plugins dir
 */
downloadRemotePlugin = { url, pluginsBase ->
    def slash = url.file.lastIndexOf('/')
    def fullPluginName = "${url.file[slash + 8..-5]}"
    String zipLocation = "${pluginsBase}/grails-${fullPluginName}.zip"
    fetchRemoteFile("${url}", zipLocation)
    readMetadataFromZip(zipLocation, url)
    return fullPluginName
}

fetchRemoteFile = { url, destfn ->
    if (shouldUseSVNProtocol(pluginDistURL)) {
        // fetch the remote file..
        fetchRemote(url) { repo, file ->
            // get the latest file from the repository..
            def f = (destfn instanceof File) ? destfn : new File(destfn)
            f.withOutputStream() { os ->
                def props = new SVNProperties()
                repo.getFile(file , (long)-1L, props , os)
            }
        }
    }
    else {
        ant.get(src:url, dest:destfn, verbose:"yes", usetimestamp:true)
    }
}

/**
 * Fetch the entire plugin list file.
 */
fetchPluginListFile = { url ->
    // attempt to fetch the file using SVN.
    if (shouldUseSVNProtocol(pluginDistURL)) {
        def rdr = fetchRemote(url) { repo, file ->
            // get the latest file from the repository..
            def props = new SVNProperties()
            def baos = new ByteArrayOutputStream()
            def ver = repo.getFile(file , (long)-1L, props , baos)
            def mimeType = props.getSVNPropertyValue(SVNProperty.MIME_TYPE)
            if (!SVNProperty.isTextMimeType(mimeType)) {
                throw new Exception("Must be a text file..")
            }
            return new StringReader(new String(baos.toByteArray(), 'utf-8'))
        }
        return rdr
    }
    // attempt using URL
    return new URL(url)
}

def fetchRemote(url, closure) {
    def idx = url.lastIndexOf('/')
    def svnUrl = url.substring(0,idx)
    def file = url.substring(idx+1,url.length())

    withSVNRepo(svnUrl) { repo ->
        // determine if the file exists
        SVNNodeKind nodeKind = repo.checkPath(file , -1)
        if (nodeKind == SVNNodeKind.NONE) {
            throw new Exception("The file does not exist.: " + url)
        }
        if (nodeKind != SVNNodeKind.FILE) {
            throw new Exception("Error not a file..: " + url)
        }
        // present w/ file etc for repo extraction
        closure.call(repo, file)
    }
}

def isSecureUrl(Object url) {
    url.startsWith('https://') || url.startsWith('svn://')
}

withSVNRepo = { url, closure ->
    // create a authetication manager using the defaults
    ISVNAuthenticationManager authMgr = getAuthFromUrl(url,"discovery")

    // create the url
    def svnUrl = SVNURL.parseURIEncoded(url)
    def repo = SVNRepositoryFactory.create(svnUrl, null)
    repo.authenticationManager = authMgr
    // trigger authentication failure?.?.
    try {
        repo.getLatestRevision()
    }
    catch (SVNAuthenticationException ex) {
        event "StatusUpdate", ["Default authentication failed please enter credentials."]
        // prompt for login information..
        ant.input(message:"Please enter your SVN username:", addproperty:"user.svn.username")
        ant.input(message:"Please enter your SVN password:", addproperty:"user.svn.password")
        def username = ant.antProject.properties."user.svn.username"
        def password = ant.antProject.properties."user.svn.password"
        authMgr = SVNWCUtil.createDefaultAuthenticationManager(username , password)
        repo.setAuthenticationManager(authMgr)
        // don't bother to catch this one let it bubble up..
        repo.getLatestRevision()
    }
    // make sure the closure return is returned..
    closure.call(repo)
}
