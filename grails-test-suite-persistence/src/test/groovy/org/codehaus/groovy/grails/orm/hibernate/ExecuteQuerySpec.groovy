package org.codehaus.groovy.grails.orm.hibernate

class ExecuteQuerySpec extends GormSpec {

    @Override
    List getDomainClasses() {
        [Person, Pet, Face, Nose]
    }

    void "Test executeQuery with select"() {
        given:"Some people"
            createPeople()

        when:"executeQuery is called with a select"
            def results = Person.executeQuery("select firstName, age from Person p where p.firstName = ? ", ["Bart"])

        then:"The correct results are returned"
            results != null
            results[0][0] == "Bart"
            results[0][1] == 9

        // Test for GRAILS-8002
        when:"We iterate over the results with each"
            def ages = []
            results.each { ages << it[1]}

        then:"No errors occur"
            ages == [9]
    }

    protected void createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson", age:45).save()
        new Person(firstName: "Marge", lastName: "Simpson", age:40).save()
        new Person(firstName: "Bart", lastName: "Simpson", age:9).save()
        new Person(firstName: "Lisa", lastName: "Simpson", age:7).save()
        new Person(firstName: "Barney", lastName: "Rubble", age:35).save()
        new Person(firstName: "Fred", lastName: "Flinstone", age:41).save()
    }
}
