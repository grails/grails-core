package org.codehaus.groovy.grails.orm.hibernate

class MappedByMergedWithInheritedValueTests extends AbstractGrailsHibernateTests{
    @Override
    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Document {
    String name

    static hasMany = [
        toRole: DocDocRole,
        fromRole: DocDocRole
    ]

    static mappedBy = [
        toRole: 'fromDocument',
        fromRole: 'toDocument'
    ]
}

@Entity
class DocDocRole {
    String roleName

    Document fromDocument
    Document toDocument

    static belongsTo = [
        Document
    ]

    static mappedBy = [
        fromDocument: 'toRole',
        toDocument: 'fromRole'
    ]
}

@Entity
class SpecialDocument extends Document {
    String specialStatus

    static mappedBy = [
        //some other things
    ]
}
''')
    }

    // test for GRAILS-6328
    void testMappedByMergedWithInheritedValue() {
        def SpecialDocument = ga.getDomainClass("SpecialDocument").clazz

        def sd = SpecialDocument.newInstance(name:"My Doc", specialStatus: "special")

        sd.addToToRole(roleName:"To Role", toDocument:SpecialDocument.newInstance(name:"To Doc", specialStatus: "special").save())
        sd.addToFromRole(roleName:"From Role", fromDocument:SpecialDocument.newInstance(name:"From Doc", specialStatus: "special").save())

        sd.save(flush:true)
        assert !sd.errors.hasErrors()

        session.clear()

        sd = SpecialDocument.get(sd.id)

        assert sd != null
        assert sd.toRole.size() == 1
        assert sd.fromRole.size() == 1
    }
}
