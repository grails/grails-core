package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.validation.ConstrainedProperty

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
               enteredBy(nullable:false,maxSize:20,custom: true)
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

    void testCustomConstraint() {
        def personClass = ga.getDomainClass("ComponentValidationTestsPerson").clazz
        def auditClass =  ga.getDomainClass("ComponentValidationTestsAuditInfo").clazz

        // Load the custom constraint.
        def constraintClass = gcl.parseClass('''
import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.springframework.validation.Errors

class CustomConstraint extends AbstractConstraint {
    boolean active
    String name = "custom"

    public void setParameter(Object constraintParameter) {
        assert constraintParameter instanceof Boolean

        this.active = constraintParameter.booleanValue()
        super.setParameter(constraintParameter)
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if (this.active) {
            if (propertyValue != "fred") {
                def args = [constraintPropertyName, constraintOwningClass, propertyValue] as Object[]
                super.rejectValue(
                    target,
                    errors,
                    "some.error.message",
                    "invalid.custom",
                    args)
            }
        }
    }

    boolean supports(Class type) {
        return type == String
    }
}
''')

        // Register the new constraint.
        Class clazz = gcl.loadClass("org.codehaus.groovy.grails.validation.ConstrainedProperty")
        clazz.registerNewConstraint("custom", constraintClass)

        // Refresh the constraints now that we have registered a new one.
        ga.refreshConstraints()

        // Create the test domain instances.
        def person = personClass.newInstance()
        def date = new Date()
        person.name = 'graeme'
        person.auditInfo = auditClass.newInstance(
                dateEntered: date,
                dateUpdated: date,
                enteredBy: 'chris',
                updatedBy: 'chris')

        assertFalse "The validation should fail since the custom validator has been registered.", person.validate()
    }
}
