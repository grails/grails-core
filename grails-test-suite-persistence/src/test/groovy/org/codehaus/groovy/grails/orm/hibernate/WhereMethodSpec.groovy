package org.codehaus.groovy.grails.orm.hibernate

import grails.gorm.DetachedCriteria
import spock.lang.Ignore

/**
 * Tests the where method in Grails
 */
class WhereMethodSpec extends GormSpec{
    @Override
    List getDomainClasses() {
        [Person, Pet]
    }

   String nameBart() { "Bart" }
   def "Test where method with value obtained via method call"() {
       given:"A bunch of people"
            createPeople()

       when:"We find a person where first name is obtained via method call"
            Person p = Person.find { firstName == nameBart() }

       then:"The expected result is returned"
            p != null
            p.firstName == "Bart"
   }

   def "Test second where declaration on detached criteria instance"() {
       given:"A bunch of people"
            createPeople()

       when:"We create a 2 where queries, one derived from the other"
            def q1 = Person.where {
                lastName == "Simpson"
            }

            def q2 = q1.where {
                firstName == "Bart"
            }

       then:"The first query is not modified, and the second works as expected"
            q1.count() == 4
            q2.count() == 1
   }

   def "Test query association"() {
       given:"People with a few pets"
            createPeopleWithPets()

       when:"We query for people by Pet using a simple equals query"

            def query = Person.where {
                pets.name == "Butch"
            }
            def count = query.count()
            def result = query.find()

       then:"The expected result is returned"
            count == 1
            result != null
            result.firstName == "Joe"

       when:"We query for people by Pet with multiple results"
            query = Person.where {
                pets.name ==~ "B%"
            }
            count = query.count()
            def results = query.list(sort:"firstName")

       then:"The expected results are returned"
            count == 2
            results[0].firstName == "Ed"
            results[1].firstName == "Joe"


   }

   def "Test query association with or"() {
       given:"People with a few pets"
            createPeopleWithPets()

       when:"We use a logical or to query people by pets"
           def query = Person.where {
               pets { name == "Jack" || name == "Joe" }
           }
           def count = query.count()
           def results = query.list(sort:"firstName")

       then:"The expected results are returned"
            count == 2
            results[0].firstName == "Fred"
            results[1].firstName == "Joe"


       when:"We use a logical or to query pets combined with another top-level logical expression"
           query = Person.where {
               pets { name == "Jack" } || firstName == "Ed"
           }
           count = query.count()
           results = query.list(sort:"firstName")

        then:"The correct results are returned"
            results[0].firstName == "Ed"
            results[1].firstName == "Joe"
   }

   def "Test findAll method for inline query"() {
       given:"A bunch of people"
            createPeople()

       when:"We find a person where first name is Bart"
            List people = Person.findAll { lastName == "Simpson" }

       then:"The expected result is returned"
            people.size() == 4
   }

   def "Test find method for inline query"() {
       given:"A bunch of people"
            createPeople()

       when:"We find a person where first name is Bart"
            Person p = Person.find { firstName == "Bart" }

       then:"The expected result is returned"
            p != null
            p.firstName == "Bart"
   }

   def "Test declare closure as detached criteria"() {
       given:"A bunch of people"
            createPeople()

       when:"A closure is declared as detached criteria and then passed to where"
            def callable = { firstName == "Bart" } as DetachedCriteria<Person>
            def query = Person.where(callable)
            Person p = query.find()

       then:"The right result is returned"
            p != null
            p.firstName == "Bart"

   }

   def "Test query using captured variables"() {
       given:"A bunch of people"
            createPeople()

       when:"We query with variable captured from outside the closures scope"
            def params = [firstName:"Bart"]
            def query = Person.where {
                firstName == params.firstName
            }
            def count = query.count()
            Person p = query.find()

       then:"The correct results are returned"
            count == 1
            p != null
            p.firstName == "Bart"
   }
   def "Test negation query"() {
       given:"A bunch of people"
            createPeople()

       when:"A single criterion is negated"
            def query = Person.where {
                !(lastName == "Simpson")
            }
            def results = query.list(sort:"firstName")

       then:"The right results are returned"
            results.size() == 2

       when:"Multiple criterion are negated"

            query = Person.where {
                !(firstName == "Fred" || firstName == "Barney")
            }
            results = query.list(sort:"firstName")

       then:"The right results are returned"
            results.every { it.lastName == "Simpson" }

       when:"Negation is combined with non-negation"

            query = Person.where {
                firstName == "Fred" && !(lastName == 'Simpson')
            }
            Person result = query.find()

       then:"The correct results are returned"
            result != null
            result.firstName == "Fred"

       when:"Negation is combined with non-negation"

            query = Person.where {
                !(firstName == "Homer") && lastName == 'Simpson'
            }
            results = query.list(sort:"firstName")

       then:"The correct results are returned"
            results.size() == 3
            results[0].firstName == "Bart"
            results[1].firstName == "Lisa"
            results[2].firstName == "Marge"


   }


    def "Test eqProperty query"() {
       given:"A bunch of people"
            createPeople()
            new Person(firstName: "Frank", lastName: "Frank").save()

       when:"We query for a person with the same first name and last name"
             def query = Person.where {
                  firstName == lastName
             }
             Person result = query.get()
             int count = query.count()

       then:"The correct result is returned"
            result != null
            count == 1
            result.firstName == "Frank"

   }

   @Ignore // rlike database specific
   def "Test rlike query"() {
       given:"A bunch of people"
            createPeople()

       when:"We query for people whose first names start with the letter B"
         def query = Person.where {
              firstName ==~ ~/B.+/
         }
         def results = query.list(sort:'firstName')

       then:"The correct results are returned"
           results.size() == 2
           results[0].firstName == "Barney"
           results[1].firstName == "Bart"
   }

   def "Test like query"() {
       given:"A bunch of people"
            createPeople()

       when:"We query for people whose first names start with the letter B"
         def query = Person.where {
              firstName ==~ "B%"
         }
         def results = query.list(sort:'firstName')

       then:"The correct results are returned"
           results.size() == 2
           results[0].firstName == "Barney"
           results[1].firstName == "Bart"
   }

   def "Test in list query"() {
       given:"A bunch of people"
            createPeople()

       when:"We query for people in a list"
         def query = Person.where {
              firstName in ["Bart", "Homer"]
         }
         def results = query.list(sort:'firstName')

       then:"The correct results are returned"
           results.size() == 2
           results[0].firstName == "Bart"
           results[1].firstName == "Homer"
   }

   def "Test less than or equal to query"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people older than 30"
            def query = Person.where {
                age <= 35
            }
            def results = query.list(sort:'firstName')

        then:"The correct results are returned"
            results.size() == 3
            results[0].firstName == "Barney"
            results[1].firstName == "Bart"
            results[2].firstName == "Lisa"

        when:"A greater than query is combined with an equals query"
            query = Person.where {
                age <= 35 && lastName == 'Simpson'
            }
            results = query.list(sort:'firstName')

        then:"The correct results are returned"

            results.size() == 2
            results[0].firstName == "Bart"
            results[1].firstName == "Lisa"
    }

    def "Test greater than or equal to query"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people older than 30"
            def query = Person.where {
                age >= 35
            }
            def results = query.list(sort:'firstName')

        then:"The correct results are returned"
            results.size() == 4
            results[0].firstName == "Barney"
            results[1].firstName == "Fred"
            results[2].firstName == "Homer"
            results[3].firstName == "Marge"

        when:"A greater than query is combined with an equals query"
            query = Person.where {
                age >= 35 && lastName == 'Simpson'
            }
            results = query.list(sort:'firstName')

        then:"The correct results are returned"

            results.size() == 2
            results[0].firstName == "Homer"
            results[1].firstName == "Marge"
    }

    def "Test less than query"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people younger than 30"
            def query = Person.where {
                age < 30
            }
            def results = query.list(sort:'firstName')

        then:"The correct results are returned"
            results.size() == 2
            results[0].firstName == "Bart"
            results[1].firstName == "Lisa"

        when:"A greater than query is combined with an equals query"
            query = Person.where {
                age < 30 && firstName == 'Bart'
            }
            results = query.list(sort:'firstName')

        then:"The correct results are returned"

            results.size() == 1
            results[0].firstName == "Bart"
    }

    def "Test greater than query"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people older than 30"
            def query = Person.where {
                age > 35
            }
            def results = query.list(sort:'firstName')

        then:"The correct results are returned"
            results.size() == 3
            results[0].firstName == "Fred"
            results[1].firstName == "Homer"
            results[2].firstName == "Marge"

        when:"A greater than query is combined with an equals query"
            query = Person.where {
                age > 35 && lastName == 'Simpson'
            }
            results = query.list(sort:'firstName')

        then:"The correct results are returned"

            results.size() == 2
            results[0].firstName == "Homer"
            results[1].firstName == "Marge"
    }

    def "Test nested and or query"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
                (lastName != "Simpson" && firstName != "Fred") || firstName == "Bart"
            }
            def results = query.list(sort:"firstName")

        then:"The correct result is returned"
            results.size() == 2
            results[0].firstName == "Barney"
            results[1].firstName == "Bart"
    }

    def "Test not equal query"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
                 lastName != "Simpson"
            }
            def results = query.list(sort:"firstName")

        then:"The correct result is returned"
            results.size() == 2
            results[0].firstName == "Barney"
            results[1].firstName == "Fred"
    }

    def "Test basic binary criterion where call"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
                 firstName == "Bart" && lastName == "Simpson"
            }
            def result = query.get()

        then:"The correct result is returned"

            result != null
            result.firstName == "Bart"

    }


    def "Test basic single criterion where call"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
               firstName == "Bart"
            }
            def result = query.get()

        then:"The correct result is returned"

            result != null
            result.firstName == "Bart"

    }

    protected def createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson", age:45).save()
        new Person(firstName: "Marge", lastName: "Simpson", age:40).save()
        new Person(firstName: "Bart", lastName: "Simpson", age:9).save()
        new Person(firstName: "Lisa", lastName: "Simpson", age:7).save()
        new Person(firstName: "Barney", lastName: "Rubble", age:35).save()
        new Person(firstName: "Fred", lastName: "Flinstone", age:41).save()
    }

    protected def createPeopleWithPets() {
        new Person(firstName: "Joe", lastName: "Bloggs").addToPets(name: "Jack").addToPets(name: "Butch").save()

        new Person(firstName: "Ed", lastName: "Floggs").addToPets(name: "Mini").addToPets(name: "Barbie").save()

        new Person(firstName: "Fred", lastName: "Cloggs").addToPets(name: "Jim").addToPets(name: "Joe").save()
    }

}

