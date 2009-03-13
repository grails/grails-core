package org.codehaus.groovy.grails.orm.hibernate

import org.springframework.orm.hibernate3.HibernateQueryException

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 21, 2009
 */

public class AutoImportPackedDomainTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.*

@Entity
class Model {
    String name
}

@Entity
class Model2 {
    String name

    static mapping = {
        autoImport false
    }
}

''')
    }


    void testAutoImport() {
        def Model = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.Model").clazz
        def Model2 = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.Model2").clazz

        Model.newInstance(name:'Name').save(flush:true)
        Model2.newInstance(name:'Name').save(flush:true)

        def models = Model.findAll("from org.codehaus.groovy.grails.orm.hibernate.Model as m where m.name = 'Name'")
        assertTrue models.size() == 1

        models = Model.findAll("from Model as m where m.name = 'Name'")
        assertTrue models.size() == 1


        models = Model2.findAll("from org.codehaus.groovy.grails.orm.hibernate.Model2 as m where m.name = 'Name'")
        assertTrue models.size() == 1

        shouldFail(HibernateQueryException) {
            models = Model2.findAll("from Model2 as m where m.name = 'Name'")
        }


    }

}