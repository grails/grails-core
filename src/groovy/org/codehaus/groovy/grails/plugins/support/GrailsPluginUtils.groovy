package org.codehaus.groovy.grails.plugins.support;

import grails.util.GrailsUtil

class GrailsPluginUtils {

	static grailsVersion = null
	static getGrailsVersion() { 
		return GrailsUtil.getGrailsVersion()
	}

}