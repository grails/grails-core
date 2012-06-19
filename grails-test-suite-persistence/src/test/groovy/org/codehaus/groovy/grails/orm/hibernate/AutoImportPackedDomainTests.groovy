package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import org.springframework.orm.hibernate3.HibernateQueryException

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 21, 2009
 */
class AutoImportPackedDomainTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [AutoImportModel, AutoImportModel2]
    }

    void testAutoImport() {
        def Model = ga.getDomainClass(AutoImportModel.name).clazz
        def Model2 = ga.getDomainClass(AutoImportModel2.name).clazz

        Model.newInstance(name:'Name').save(flush:true)
        Model2.newInstance(name:'Name').save(flush:true)

        def models = Model.findAll("from org.codehaus.groovy.grails.orm.hibernate.AutoImportModel as m where m.name = 'Name'")
        assertTrue models.size() == 1

        models = Model.findAll("from AutoImportModel as m where m.name = 'Name'")
        assertTrue models.size() == 1

        models = Model2.findAll("from org.codehaus.groovy.grails.orm.hibernate.AutoImportModel2 as m where m.name = 'Name'")
        assertTrue models.size() == 1

        shouldFail(HibernateQueryException) {
            models = Model2.findAll("from AutoImportModel2 as m where m.name = 'Name'")
        }
    }
}

@Entity
class AutoImportModel {
    String name
}

@Entity
class AutoImportModel2 {
    String name

    static mapping = {
        autoImport false
    }
}
