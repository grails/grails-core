package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class MapMappingJoinTableTests extends AbstractGrailsHibernateTests {

    void testTypeMappings() {
		def skydiveClass = ga.getDomainClass("Skydive")

        def map = ['freefall':'60 sec','altitude': '14,000 ft']

        def skydive = skydiveClass.newInstance()

        skydive.jumplog = map
        skydive.save(flush:true)

        session.clear()

        
        skydive = skydiveClass.clazz.get(1)

        assertEquals 2, skydive.jumplog.size()
        assertEquals '60 sec', skydive.jumplog.freefall

        def c = session.connection()

        def ps = c.prepareStatement("select * from  jump_info")

        def rs = ps.executeQuery()

        assert rs.next()
    }

    void onSetUp() {
		this.gcl.parseClass('''
class Skydive {
	Long id
	Long version

    Map jumplog

    static mapping = {
        jumplog joinTable:[name:"jump_info",column:"map_key"]
    }
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
