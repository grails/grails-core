package org.codehaus.groovy.grails.domain
class CircularRelationship {
	 Long id
	 Long version
	
	 def relatesToMany = [children:CircularRelationship]
	
	 CircularRelationship parent
	 Set children	
}