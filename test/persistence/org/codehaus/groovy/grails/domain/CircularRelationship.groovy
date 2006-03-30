package org.codehaus.groovy.grails.domain
class CircularRelationship {
	@Property Long id
	@Property Long version
	
	@Property relatesToMany = [children:CircularRelationship]
	
	@Property CircularRelationship parent
	@Property Set children	
}