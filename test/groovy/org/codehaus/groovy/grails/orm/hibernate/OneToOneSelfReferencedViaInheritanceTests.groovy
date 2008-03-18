package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 17, 2008
*/
class OneToOneSelfReferencedViaInheritanceTests extends AbstractGrailsMockTests{

    protected void onSetUp() {
        gcl.parseClass '''
class Content implements Serializable {
    Long id
    Long version
	String title
	String body

    static mapping = {
        tablePerSubclass true
    }
}
class Version extends Content {
	Integer number
	Content current
    
}
class WikiPage extends Content {
    Set versions
	static hasMany = [versions:Version]    
}
'''
    }


    void testOneToOneSelfReferencingViaInheritance() {
        GrailsDomainClass versionClass = ga.getDomainClass("Version")
        GrailsDomainClass wikiPageClass = ga.getDomainClass("WikiPage")


        assertTrue wikiPageClass.getPropertyByName("versions").isOneToMany()
        assertTrue wikiPageClass.getPropertyByName("versions").isBidirectional()
        assertTrue versionClass.getPropertyByName("current").isManyToOne()
        assertTrue versionClass.getPropertyByName("current").isBidirectional()
    }

    

}