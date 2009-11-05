package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class CircularOneToOneWithInheritanceLazinessTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Content {

    static mapping = {}
}

@Entity
class VirtualContent extends Content {
     Content target

    static mapping = {
        target lazy:false
    }
}
''')
    }

    void testForLazyProxies() {
        def Content = ga.getDomainClass("Content").clazz
        def VirtualContent = ga.getDomainClass("VirtualContent").clazz

        def c = Content.newInstance().save(flush:true)

        assert c : "should have saved c"

        def vc = VirtualContent.newInstance(target:c).save(flush:true)

        assert vc : "should have saved vc"

        session.clear()

        def c1 = VirtualContent.get(2)

        assert GrailsHibernateUtil.isInitialized(c1, "target") : "should not be a proxy!"
    }


}