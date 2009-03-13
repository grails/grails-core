package org.codehaus.groovy.grails.web.servlet.filter;
class GrailsResourceCopier implements ResourceCopier {

    String basedir = "."
    String destdir = "./web-app"


	public void cleanControllers() {
	    def ant = new AntBuilder()	
        if(new File("${basedir}/grails-app/controllers").exists()) {	
        	ant.delete {
        		fileset(dir:"${destdir}/WEB-INF/grails-app/controllers",includes:"*.groovy")
        	}
            ant.copy(todir:"${destdir}/WEB-INF/grails-app/controllers",failonerror:false) {
                fileset(dir:"${basedir}/grails-app/controllers",includes:"**")
            }        	
        }
	}
	
    public void copyGrailsApp() {
	    def ant = new AntBuilder()    
        if(new File("${basedir}/grails-app").exists()) {
            ant.copy(todir:"${destdir}/WEB-INF/grails-app",failonerror:false) {
                fileset(dir:"${basedir}/grails-app",includes:"**")
            }
        }
    }

    public void copyViews(boolean shouldOverwrite) {
	    def ant = new AntBuilder()
        if(new File("${basedir}/grails-app/views").exists()) {
            ant.copy(todir:"${destdir}/WEB-INF/grails-app/views",failonerror:false,overwrite:shouldOverwrite) {
                fileset(dir:"${basedir}/grails-app/views",includes:"**")
            }
        }
    }

    public void copyViews() {
	    def ant = new AntBuilder()
        if(new File("${basedir}/grails-app/views").exists()) {
            ant.copy(todir:"${destdir}/WEB-INF/grails-app/views",failonerror:false) {
                fileset(dir:"${basedir}/grails-app/views",includes:"**")
            }
        }
    }
    
    public void copyResourceBundles() {
	    def ant = new AntBuilder()
        if(new File("${basedir}/grails-app/i18n").exists()) {
        	ant.native2ascii(src:"${basedir}/grails-app/i18n",
					 dest:"${destdir}/WEB-INF/grails-app/i18n",
					 includes:"*.properties",
					 encoding:"UTF-8")
        }
    }
 
    public void generateWebXml() {
        def controllersHome = new File("${basedir}/grails-app/controllers")
        if(controllersHome.exists()) {
            println "base directory exists re-creating web.xml"
            def controllers = []
            def flows = []
            controllersHome.eachFileRecurse {
                def match = it.name =~ "(\\w+)(Controller.groovy\$)"
                if(match) {
                    def controllerName = match[0][1]
                    controllerName = controllerName[0].toLowerCase() + controllerName[1..-1]
                    controllers << controllerName
                }
                match = it.name =~ "(\\w+)(PageFlow.groovy\$)"
                if(match) {
                    def flowName = match[0][1]
                    flowName = flowName[0].toLowerCase() + flowName[1..-1]
                    flows << flowName
                }
            }
            def binding = [ "controllers" : controllers,
                            "flows" : flows,
                            "basedir" : "${basedir}",
                            "destdir" : "${basedir}",
                            "dev" : true     ]
            def engine = new groovy.text.SimpleTemplateEngine()
            def template = engine.createTemplate( new File("${basedir}/web-app/WEB-INF/web.template.xml"  ).text ).make(binding)

            new File("${destdir}/WEB-INF/web.xml" ).write( template.toString() )
        }
    }
}