import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU    
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory  
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.*
import org.tmatesoft.svn.core.*    
import org.tmatesoft.svn.core.auth.*    
import org.tmatesoft.svn.core.wc.*                  
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

grailsHome = Ant.project.properties."environment.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugin.groovy" )
  
pluginSVN = "https://svn.codehaus.org/grails-plugins"
//pluginSVN = "file:///Developer/localsvn" 
authManager = null	      
message = null    
trunk = null    
latestRelease = null
versionedRelease = null

target ('default': "A target for plug-in developers that uploads and commits the current plug-in as the latest revision. The command will prompt for your SVN login details.") {
	releasePlugin()
}
  
target(processAuth:"Prompts user for login details to create authentication manager") {
	if(!authManager) {
		Ant.input(message:"Please enter your SVN username:", addproperty:"user.svn.username")
		Ant.input(message:"Please enter your SVN password:", addproperty:"user.svn.password") 

	 	def username = Ant.antProject.properties."user.svn.username"		
		def password = Ant.antProject.properties."user.svn.password"		

	 	authManager = SVNWCUtil.createDefaultAuthenticationManager( username , password )			
	}
}
target(releasePlugin: "The implementation target") {
	//depends(packagePlugin)
	depends(packagePlugin, processAuth)                       
	 
	remoteLocation = "${pluginSVN}/grails-${pluginName}"	
	trunk = SVNURL.parseURIDecoded("${remoteLocation}/trunk")	      
	latestRelease = "${remoteLocation}/tags/LATEST_RELEASE"                      	
	versionedRelease = "${remoteLocation}/tags/RELEASE_${plugin.version.toString().replaceAll('\\.','_')}"
	
		
    FSRepositoryFactory.setup();	
	DAVRepositoryFactory.setup()
	
	try {
		def statusClient = new SVNStatusClient((ISVNAuthenticationManager)authManager,null)		 
		 
		boolean imported = false
		try {
			// get status of base directory, if this fails exception will be thrown
			def status = statusClient.doStatus(baseFile, true)			         		
		}   
		catch(SVNException ex) {
			  
			// error with status, not in repo, attempt import..   
			importToSVN()   			
			imported = true
		}                  
		if(!imported) {     
			updateAndCommitLatest()
			tagPluginRelease()			
			event('StatusFinal', ["Plug-in release successfully published"])				
		}
	}   
	catch(Exception e) {  
		event('StatusFinal', ["Error occurred with release-plugin: ${e.message}"])	
		e.printStackTrace()
	}
}
     
target(checkInPluginZip:"Checks in the plug-in zip if it has not been checked in already") {
	def statusClient = new SVNStatusClient((ISVNAuthenticationManager)authManager,null)		 	
	def wcClient = new SVNWCClient((ISVNAuthenticationManager)authManager,null)			
	def pluginFile = new File(pluginZip)	
	def addPluginFile = false
	try {
		def status = statusClient.doStatus(pluginFile, true)
		if(status.kind == SVNNodeKind.NONE || status.kind == SVNNodeKind.UNKNOWN) addPluginFile = true
	}   
	catch(SVNException) {                  
		// not checked in add and commit       
		addPluginFile = true
	}                       
	if(addPluginFile) wcClient.doAdd(pluginFile,true,false,false,false)
	def pluginXml = new File("${basedir}/plugin.xml") 
	addPluginFile = false                              
	try {
		def status = statusClient.doStatus(pluginXml, true)
		if(status.kind == SVNNodeKind.NONE || status.kind == SVNNodeKind.UNKNOWN) addPluginFile = true
	}                                        
	catch(SVNException e) {
		addPluginFile = true
	}                       
	if(addPluginFile) wcClient.doAdd(pluginXml, true, false,false,false)
}
target(updateAndCommitLatest:"Commits the latest revision of the Plug-in") {
   def result = confirmInput("""
This command will perform the following steps to release your plug-in into Grails' SVN repository:
* Update your sources to the HEAD revision
* Commit any changes you've made to SVN
* Tag the release

NOTE: This command will not add new resources for you, if you have additional sources to add please run 'svn add' before running this command.
NOTE: Make sure you have updated the version number if you *GrailsPlugin.groovy descriptor.                                                   

Are you sure you wish to proceed?
""") 
	if(result == 'n') exit(0)   
	
		checkInPluginZip()	
	
	updateClient = new SVNUpdateClient((ISVNAuthenticationManager)authManager, null)
	
	println "Updating from SVN '${remoteLocation}'"
	long r = updateClient.doUpdate(baseFile, SVNRevision.HEAD, true)
	println "Updated to revision ${r}. Committing local, please wait..."

	commitClient = new SVNCommitClient((ISVNAuthenticationManager)authManager, null)	
	
	if(!message) askForMessage()
	
	println "Committing code. Please wait..."
	
	def commit = commitClient.doCommit([baseFile] as File[],false,message,true,true)
	
	println "Committed revision ${commit.newRevision}."
}  

target(importToSVN:"Imports a plug-in project to Grails' remote SVN repository") {
	checkOutDir = new File("${baseFile.parentFile.absolutePath}/checkout/${baseFile.name}")                                         
		
	def result = confirmInput("""
This plug-in project is not currently in the repository, this command will now:
* Perform an SVN import into the repository                             
* Tag the plug-in project as the LATEST_RELEASE
* Checkout the imported version of the project from SVN to '${checkOutDir}'
Are you sure you wish to proceed?   		
	""")
	if(result == 'n') exit(0)

	Ant.unzip(src:pluginZip, dest:"${basedir}/unzipped")
	Ant.copy(file:pluginZip, todir:"${basedir}/unzipped")


	importClient = new SVNCommitClient((ISVNAuthenticationManager)authManager, null)
    askForMessage()

    println "Importing project to ${remoteLocation}. Please wait..."               

	def svnURL = SVNURL.parseURIDecoded("${remoteLocation}/trunk")
	importClient.doImport(new File("${basedir}/unzipped"),svnURL,message,true)
	println "Plug-in project imported to SVN at location '${remoteLocation}/trunk'"
	
	tagPluginRelease()

	Ant.delete(dir:"${basedir}/unzipped")


	checkOutDir.parentFile.mkdirs()

	updateClient = new SVNUpdateClient((ISVNAuthenticationManager)authManager, null)
	println "Checking out locally to '${checkOutDir}'."
	updateClient.doCheckout(svnURL, checkOutDir, SVNRevision.HEAD,SVNRevision.HEAD, true)

	event('StatusFinal', ["""   
Completed SVN project import. If you are in terminal navigate to imported project with:
cd ${checkOutDir}
	
Future changes should be made to the SVN controlled sources!"""])	
}                      

target(tagPluginRelease:"Tags a plugin-in with the LATEST_RELEASE tag and version tag within the /tags area of SVN") {
	

	copyClient = new SVNCopyClient((ISVNAuthenticationManager)authManager, null)
	commitClient = new SVNCommitClient((ISVNAuthenticationManager)authManager, null)
	
	if(!message) askForMessage()                                    	
	
	println "Tagging release. Please wait..."

	tags = SVNURL.parseURIDecoded("${remoteLocation}/tags")
	latest = SVNURL.parseURIDecoded(latestRelease)
	release = SVNURL.parseURIDecoded(versionedRelease)               
		
    try {  commitClient.doMkDir([tags] as SVNURL[], message)}	                       
	catch(SVNException e) {
		// ok - already exists
	}
	try { commitClient.doDelete([latest] as SVNURL[], message)	}                                                                                                      
	catch(SVNException e) {  
	  // ok - already exists
	}
	try { commitClient.doDelete([release] as SVNURL[], message)	}                                                                                                      
	catch(SVNException e) {  
	  // ok - already exists
	}	
	try { commitClient.doMkDir([latest] as SVNURL[], message)	}	
	catch(SVNException e) {
	   // ok - already exists 
	}
	try { commitClient.doMkDir([release] as SVNURL[], message)	}	
	catch(SVNException e) {
	   // ok - already exists 
	}
	

	repository = SVNRepositoryFactory.create( trunk )
	//repository.setAuthenticationManager( authManager )
	 
	// we need to get a list of SVNURL entries to copy into the tags/LATEST_RELEASE directory
	def entries = []
	def root = repository.getDir("",-1,false	, entries)
	def urls = entries.collect { it.URL }  
	
	def commit          
	println "Tagging latest release, please wait..."	
	urls.each { url -> commit = copyClient.doCopy(url,SVNRevision.HEAD,latest, false, false, message ) }
	println "Copied trunk to ${latestRelease} with revision ${commit.newRevision} on ${commit.date}"
	
	println "Tagging version release, please wait..."			
	urls.each { url -> commit = copyClient.doCopy(url,SVNRevision.HEAD,release, false, false, message ) }	
	println "Copied trunk to ${versionedRelease} with revision ${commit.newRevision} on ${commit.date}"    	
}  

target(askForMessage:"Asks for the users commit message") {
	Ant.input(message:"Enter a SVN commit message:", addproperty:"commit.message")
 	message = Ant.antProject.properties."commit.message"	
}
