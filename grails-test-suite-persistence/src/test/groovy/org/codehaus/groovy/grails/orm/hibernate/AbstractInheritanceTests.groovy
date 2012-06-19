package org.codehaus.groovy.grails.orm.hibernate

class AbstractInheritanceTests extends AbstractGrailsHibernateTests {
    @Override protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
abstract class AbstractInheritanceAbstractBase {
      static hasMany = [ones:AbstractInheritanceOne]
      String name
}

@Entity
class AbstractInheritanceDerived extends AbstractInheritanceAbstractBase {
}

@Entity
class AbstractInheritanceOne {
     static belongsTo = [derived:AbstractInheritanceAbstractBase]
}
''')

        gcl.parseClass('''
import grails.persistence.*

@Entity
class AbstractInheritancePerson {
    String name
}

@Entity
class AbstractInheritanceComment {
  static belongsTo = [person:AbstractInheritancePerson, contract:AbstractInheritanceContract]
}

@Entity
abstract class AbstractInheritanceContract {
  static hasMany = [comments:AbstractInheritanceComment]

  String name
  Date contractDate

  static transients = [ 'name' ]

  abstract String getTypeName();

  def onLoad() {
    name = getTypeName()
  }
}

@Entity
class AbstractInheritanceReferral extends AbstractInheritanceContract {
  String getTypeName() { return "Referral" }
}

@Entity
class AbstractInheritanceInservice extends AbstractInheritanceContract {
    String getTypeName() { return "Inservice" }
}
''')

        gcl.parseClass('''
abstract class BaseNonGormEnhanced {
    String name
}

import grails.persistence.*

@Entity
class ConcreteGormEnhanced extends BaseNonGormEnhanced {

}


''')
    }


    void testAbstractInheritanceWithOneToMany() {
        def Derived = ga.getDomainClass("AbstractInheritanceDerived").clazz
        def Abstract = ga.getDomainClass("AbstractInheritanceAbstractBase").clazz
        def One = ga.getDomainClass("AbstractInheritanceOne").clazz

        def derived = Derived.newInstance(name:"Bob")

        derived.save(flush:true)

        def one = One.newInstance(derived:derived)

        one.save(flush:true)

        session.clear()

        one = One.get(1)

        assert one.derived != null
        assert one.derived.name == "Bob"
    }

    void testGRAILS5356() {
        def Person = ga.getDomainClass("AbstractInheritancePerson").clazz
        def Comment = ga.getDomainClass("AbstractInheritanceComment").clazz
        def Referral = ga.getDomainClass("AbstractInheritanceReferral").clazz
        def Inservice = ga.getDomainClass("AbstractInheritanceInservice").clazz
        def Contract = ga.getDomainClass("AbstractInheritanceContract").clazz


        def bob = Person.newInstance(name:"Bob")
        bob.save()
        def fred = Person.newInstance(name:"Fred")
        fred.save()


        final now = new Date()
        Referral.newInstance(contractDate:now)
                .addToComments(person:bob)
                .save()

        Inservice.newInstance(contractDate:new Date()-7)
                 .addToComments(person:fred)
                 .save(flush:true)

        session.clear()

        assert Contract.count() == 2
        assert Referral.count() == 1
        assert Inservice.count() == 1

        assert Contract.findByContractDate(now) != null
        assert Contract.findByContractDate(now).name == 'Referral'
        assert Contract.createCriteria().get { eq 'contractDate', now} != null

        def referral = Contract.findByContractDate(now)

        assert referral != null
        assert referral.comments != null
        assert referral.comments.size() == 1

        def comment = referral.comments.iterator().next()
        assert comment != null
        assert comment.person.name == 'Bob'

    }

}
