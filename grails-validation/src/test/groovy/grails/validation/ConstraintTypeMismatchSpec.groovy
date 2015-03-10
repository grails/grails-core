package grails.validation

import grails.validation.exceptions.ConstraintException
import spock.lang.Specification
import spock.lang.Unroll

class ConstraintTypeMismatchSpec extends Specification {

    @Unroll
    void 'test calling #methodName on a ConstrainedProperty for a non-String property'() {
        given:
        def cp = new ConstrainedProperty(ConstraintTypeMismatchSpec, 'testProperty', Integer);

        expect:
        cp."$methodName"() == expectedResult

        where:
        methodName     | expectedResult
        'isEmail'      | false
        'isCreditCard' | false
        'isUrl'        | false
        'getMatches'   | null
        'isUrl'        | false
    }

    @Unroll
    void 'test calling set#constraintName(#argValue) on a ConstrainedProperty for a non-String property'() {
        given:
        def cp = new ConstrainedProperty(ConstraintTypeMismatchSpec, 'testProperty', Integer);

        when:
        cp."set${constraintName}"(argValue)

        then:
        ConstraintException ex = thrown()
        "$constraintName constraint can only be applied to String properties" == ex.message

        where:
        constraintName | argValue
        'Email'        | true
        'CreditCard'   | true
        'Url'          | true
        'Matches'      | '.*'
        'Blank'        | true
    }
}
