package org.codehaus.groovy.grails.plugins.support;

import grails.util.GrailsUtil

class GrailsPluginUtils {

	static grailsVersion = null
	static getGrailsVersion() {
		if(grailsVersion)return grailsVersion

		grailsVersion = GrailsUtil.getGrailsVersion()
		if(!grailsVersion)return 0.1
		if(grailsVersion.endsWith("-SNAPSHOT"))
			grailsVersion = grailsVersion[0..-10].toBigDecimal()
		else {
			grailsVersion.toBigDecimal()
		}
		return grailsVersion
	}

}