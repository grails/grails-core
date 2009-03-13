package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class OneToManyWithSelfAndInheritanceTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Party{
    String name
    String description
}

@Entity
class Organization extends Party {
   static hasMany = [children: Organization]
   Organization parent
}

@Entity
class ExtOrganization extends Organization{
}
''')
    }


    // test for GRAILS-3210
    void testSubclassAssociationsWork() {
        def Organization = ga.getDomainClass("Organization").clazz
        def ExtOrganization = ga.getDomainClass("ExtOrganization").clazz

        def org1 = Organization.newInstance(name:'Org 1', description:'root organization').save()
        def orgA = Organization.newInstance(name:'Org A', description:'child A of Org 1', parent: org1).save()
        def orgB = Organization.newInstance(name:'Org B', description:'child B of Org 1', parent: org1).save()
        def orgaa = Organization.newInstance(name:'Org aa', description:'child aa of Org A (granchild of root)', parent: orgA).save()

        def xorg1 = ExtOrganization.newInstance(name:'ExtOrg 1', description:'root organization').save()
        def xorgA = ExtOrganization.newInstance(name:'ExtOrg A', description:'child A of Org 1', parent: xorg1).save()
        def xorgB = ExtOrganization.newInstance(name:'ExtOrg B', description:'child B of Org 1', parent: xorg1).save()
        def xorgaa = ExtOrganization.newInstance(name:'ExtOrg aa', description:'child aa of Org A (granchild of root)', parent: xorgA).save()


        session.flush()


        session.clear()

        org1 = Organization.findByName("Org 1")

        assertEquals 2, org1.children.size()

        // TODO: The following is broken due to the workaround that is in place for http://opensource.atlassian.com/projects/hibernate/browse/HHH-2855
        // Even though the issue is resolved the problem still remains. If you remove the line:
        // collection.setWhere(RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME + " = '" + referenced.getFullName() + "'");
        // from GrailsDomainBinder the problem goes away, but causes other regressions. The current workaround is to use table-per-subclass inheritance

        if(notYetImplemented()) return
        xorg1 = ExtOrganization.findByName('ExtOrg 1')

        assertEquals 2, xorg1.children.size()
    }


}