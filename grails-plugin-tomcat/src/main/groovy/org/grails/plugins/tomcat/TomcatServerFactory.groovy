package org.grails.plugins.tomcat

import grails.web.container.*

class TomcatServerFactory implements EmbeddableServerFactory {

	def pluginSettings

	EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
		return new TomcatServer(basedir, webXml, contextPath, classLoader)
	}

	EmbeddableServer createForWAR(String warPath, String contextPath) {
		return new TomcatServer(warPath, contextPath)
	}
}