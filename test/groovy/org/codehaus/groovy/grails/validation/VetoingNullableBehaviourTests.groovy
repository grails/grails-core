package org.codehaus.groovy.grails.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.springframework.validation.BindingResult

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 20, 2008
 */
class VetoingNullableBehaviourTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import org.apache.commons.lang.StringUtils

class VetoingNullableBehaviour {
    Long id
    Long version

    String name

    static constraints = {
        name(nullable:true, validator:{ val ->
            if(val) {
                return Character.isUpperCase(val.toCharArray()[0])
            }
        })
    }
}


class VetoingNullableBehaviourBook {  
    Long id
    Long version
    Boolean online
    String onlineFormatDescription

	/*
	Premise here is that onlineFormatDescription is only required IF online == true
	*/
	static constraints = {
		online(nullable: false)
        onlineFormatDescription(nullable: true, blank: true, validator: onlineValidator)
	}

    static onlineValidator = {val, obj ->
        if (obj.properties['online'] == true && StringUtils.isBlank(val)) {
            return ['blank']
        }
    }

}

''')
    }



    void testVetoingConstraint() {
        def test =  ga.getDomainClass("VetoingNullableBehaviour").newInstance()

        

        assert test.validate()

        test.name = "fred"

        assert !test.validate()

        test.name = "Fred"

        assert test.validate()
    }

	//this test will fail, but should pass.  it never gets to the custom validator because the nullable constraint stops further validation
    void testOnlineFormatDescriptionValidates_null() {
        def book = ga.getDomainClass("VetoingNullableBehaviourBook").newInstance()
        book.online = true
        book.onlineFormatDescription = null

		book.validate()
        assertFieldHasError(book.errors, 'onlineFormatDescription')
    }

	//this validation will fail because the custom validator does not
    // allow blank values, even if the "blank" constraint does.
    void testOnlineFormatDescriptionValidates_blank() {
        def book = ga.getDomainClass("VetoingNullableBehaviourBook").newInstance()
        book.online = true
        book.onlineFormatDescription = ''

        book.validate()
        assertFieldHasError(book.errors, 'onlineFormatDescription')
    }

	//this test will pass
    void testOnlineFormatDescriptionValidates_valid() {
        def book = ga.getDomainClass("VetoingNullableBehaviourBook").newInstance()
        book.online = true
        book.onlineFormatDescription = 'Title'
        book.validate()
        assertFieldDoesNotHasError(book.errors, 'onlineFormatDescription')
    }


    protected assertFieldHasError(BindingResult bindErrors, String field) {
        assertTrue("Error not found for field ${field}, errors were: ${bindErrors}", bindErrors.getFieldError(field) != null)
    }

    protected assertFieldDoesNotHasError(BindingResult bindErrors, String field) {
        assertTrue("Error not found for field ${field}, errors were: ${bindErrors}", bindErrors.getFieldError(field) == null)
    }
}