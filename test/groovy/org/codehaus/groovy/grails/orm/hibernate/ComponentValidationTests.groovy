package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 29, 2008
 */
class ComponentValidationTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class ComponentValidationTestsPerson {
       Long id
       Long version
       String name

       ComponentValidationTestsAuditInfo auditInfo
       static embedded = ['auditInfo']

       static constraints = {
               name(nullable:false, maxSize:35)
       }
}
class ComponentValidationTestsAuditInfo{
       Long id
       Long version

       Date dateEntered
       Date dateUpdated
       String enteredBy
       String updatedBy

       static constraints = {
               dateEntered(nullable:false)
               dateUpdated(nullable:false)
               enteredBy(nullable:false,maxSize:20)
               updatedBy(nullable:false,maxSize:20)
       }

       String toString(){
               return "$enteredBy $dateEntered $updatedBy $dateUpdated"
       }
}
'''
    }


     void testComponentValidation() {
        def personClass = ga.getDomainClass("ComponentValidationTestsPerson").clazz
        def auditClass =  ga.getDomainClass("ComponentValidationTestsAuditInfo").clazz

        def person = personClass.newInstance()
        person.name = 'graeme'
        def date = new Date()
        person.auditInfo = auditClass.newInstance(dateEntered:date,dateUpdated:date,enteredBy:'chris',updatedBy:'chris')
        println "auditInfo: " + person.auditInfo


        if(!person.validate()) {
           person.errors.each {
                 println it
           }
        }

        person.save()

        assert person.id != null
     }

}