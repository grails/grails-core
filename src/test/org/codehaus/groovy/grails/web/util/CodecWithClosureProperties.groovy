package org.codehaus.groovy.grails.web.util


class CodecWithClosureProperties {
	static encode = { arg ->
		"-> ${arg} <-"
	}
}