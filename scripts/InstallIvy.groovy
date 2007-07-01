

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
ivyVersion = "1.4.1"
                    
includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" ) 

task ("default": "Installs the Ivy dependency manager into the current Grails application") {
    installIvy()
}

task (installIvy : "Install Ivy tasks") {
    depends(configureProxy)
	def ivyJar = "ivy-${ivyVersion}.jar";
	def remoteFile = "http://www.jaya.free.fr/downloads/ivy/${ivyVersion}/${ivyJar}"
	if (!new File("%{grailsHome}/lib/${ivyJar}").exists()) {
	    event("StatusUpdate", ["Installing Ivy"])
		Ant.get(dest: "${grailsHome}/lib/${ivyJar}",
				src:"${remoteFile}",
				verbose:false,
				usetimestamp:true)
	}   
	
	def ivyFile = new File("${basedir}/ivy.xml")
	if(!ivyFile.exists()) {
		ivyFile.write '''
<ivy-module version="1.0">
    <info organisation="codehaus" module="grails"/>
    <dependencies>
        <!--<dependency org="apache" name="commons-lang" rev="2.1"/>-->
    </dependencies>
</ivy-module>		
		'''    
	    event("StatusUpdate", ["Created ${basedir}/ivy.xml"])
	}        
	
	def ivyConf = new File("${basedir}/ivyconf.xml")
	if(!ivyConf.exists()) {
		ivyConf.write '''
<ivy-conf>         
		<conf defaultResolver="ivy-chain"/>
		<include url="${ivy.default.conf.dir}/ivyconf-public.xml"/>
		<include url="${ivy.default.conf.dir}/ivyconf-shared.xml"/>
		<include url="${ivy.default.conf.dir}/ivyconf-local.xml"/>
		<include url="${ivy.default.conf.dir}/ivyconf-main-chain.xml"/>
		<include url="${ivy.default.conf.dir}/ivyconf-default-chain.xml"/>   

        <resolvers>       
 			<ibiblio pattern="[organisation]/[type]s/[artifact]-[revision].[ext]"
	                  name="ibiblio-retry"  />       
			<chain name="ivy-chain" dual="true">
				<resolver ref="shared"/>
				<resolver ref="public"/>
				<resolver ref="ibiblio-retry"/>  				
			</chain>			
		</resolvers>     	   	
</ivy-conf>                     		
		'''   
	    event("StatusUpdate", [ "Created ${basedir}/ivyconf.xml"])
	}
    event("StatusFinal", [ "Ivy installed"])
}