package org.codehaus.groovy.grails.web.converters;

import grails.converters.JSON;
import grails.converters.XML;

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests;

class ConvertsWithHibernateProxiesTests extends AbstractGrailsHibernateTests {

	@Override
	protected void onSetUp() {
		gcl.parseClass '''
import grails.persistence.*
		
@Entity
class Parent {

  static hasMany = [children:Child]

  String name = "Bob"

  static constraints = {
  }
}
		
@Entity
class Child {

	  static belongsTo = [parent: Parent]

	  String name = "Bob Junior"

	  static constraints = {
	    parent nullable:false
	  }
}		
		'''
	}
	
	void testMarshDomainModelContainingProxiesToJSON() {
		def Parent = ga.getDomainClass("Parent").clazz
		def Child =  ga.getDomainClass("Child").clazz
	    def parent = Parent.newInstance()
	    def child = Child.newInstance()
	    parent.addToChildren child
	    parent.save(flush: true)
		
	    session.clear()
	    
       def children = Child.list()
       assert children, "No Children Found!!!"
       def firstSon = children[0]//take first child
       parent = firstSon.parent// use parent to create JSON
       JSON.use("deep") {
			def converter = parent as JSON
			println converter.toString(true)
       }
	    
	}
	
	void testMarshDomainModelContainingProxiesToXML() {
		def Parent = ga.getDomainClass("Parent").clazz
		def Child =  ga.getDomainClass("Child").clazz
	    def parent = Parent.newInstance()
	    def child = Child.newInstance()
	    parent.addToChildren child
	    parent.save(flush: true)
		
	    session.clear()
	    
       def children = Child.list()
       assert children, "No Children Found!!!"
       def firstSon = children[0]//take first child
       parent = firstSon.parent// use parent to create JSON
       XML.use("deep") {
			def converter = parent as XML
			println converter.toString()
       }
	    
	}
	
}
