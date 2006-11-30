package org.codehaus.groovy.grails.plugins.support;


class GrailsPluginUtils {

	static grailsVersion = null
	static getGrailsVersion() {
		if(grailsVersion)return grailsVersion
		def ant = new AntBuilder()
	    ant.property(environment:"env")   
		def grailsHome = ant.antProject.properties."env.GRAILS_HOME"
		ant.property(file:"${grailsHome}/build.properties")	
		def grailsVersion =  ant.antProject.properties.'grails.version'
		if(grailsVersion.endsWith("-SNAPSHOT"))
			grailsVersion = grailsVersion[0..-10].toBigDecimal()
		return grailsVersion
	}

}