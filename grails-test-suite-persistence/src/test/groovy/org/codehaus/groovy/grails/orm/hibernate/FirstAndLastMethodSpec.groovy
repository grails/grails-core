package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class FirstAndLastMethodSpec extends GormSpec {

    void "Test first and last method with empty datastore"() {
        given:
        assert SimpleWidget.count() == 0

        when:
        def result = SimpleWidget.first()

        then:
        result == null
            
        when:
        result = SimpleWidget.last()
            
        then:
        result == null
    }

    void "Test first and last method with multiple entities in the datastore"() {
        given:
        assert new SimpleWidget(name: 'one', spanishName: 'uno').save()
        assert new SimpleWidget(name: 'two', spanishName: 'dos').save()
        assert new SimpleWidget(name: 'three', spanishName: 'tres').save()
        assert SimpleWidget.count() == 3

        when:
        def result = SimpleWidget.first()

        then:
        result?.name == 'one'
            
        when:
        result = SimpleWidget.last()
            
        then:
        result?.name == 'three'
    }
    
    void "Test first and last method with one entity"() {
        given:
        assert new SimpleWidget(name: 'one', spanishName: 'uno').save()
        assert SimpleWidget.count() == 1

        when:
        def result = SimpleWidget.first()

        then:
        result?.name == 'one'
            
        when:
        result = SimpleWidget.last()
            
        then:
        result?.name == 'one'
    }
    
    void "Test first and last method with sort parameter"() {
        given:
        assert new SimpleWidget(name: 'one', spanishName: 'uno').save()
        assert new SimpleWidget(name: 'two', spanishName: 'dos').save()
        assert new SimpleWidget(name: 'three', spanishName: 'tres').save()
        assert SimpleWidget.count() == 3

        when:
        def result = SimpleWidget.first(sort: 'name')

        then:
        result?.name == 'one'
            
        when:
        result = SimpleWidget.last(sort: 'name')
            
        then:
        result?.name == 'two'

        when:
        result = SimpleWidget.first('name')

        then:
        result?.name == 'one'
            
        when:
        result = SimpleWidget.last('name')
            
        then:
        result?.name == 'two'

        when:
        result = SimpleWidget.first(sort: 'spanishName')

        then:
        result?.spanishName == 'dos'
            
        when:
        result = SimpleWidget.last(sort: 'spanishName')
            
        then:
        result?.spanishName == 'uno'

        when:
        result = SimpleWidget.first('spanishName')

        then:
        result?.spanishName == 'dos'
            
        when:
        result = SimpleWidget.last('spanishName')
            
        then:
        result?.spanishName == 'uno'
    }
    
    void "Test first and last method with non standard identifier"() {
        given:
        ['one', 'two', 'three'].each { name ->
            assert new SimpleWidgetWithNonStandardId(name: name).save(validate: false)
        }
        assert SimpleWidgetWithNonStandardId.count() == 3

        when:
        def result = SimpleWidgetWithNonStandardId.first()

        then:
        result?.name == 'one'
            
        when:
        result = SimpleWidgetWithNonStandardId.last()
            
        then:
        result?.name == 'three'
    }
    
    void "Test first and last method with composite key"() {
        when:
        PersonWithCompositeKey.first()

        then:
        UnsupportedOperationException ex = thrown()
        'The first() method is not supported for domain classes that have composite keys.' == ex.message
        
        when:
        PersonWithCompositeKey.first(sort: 'firstName')

        then:
        ex = thrown()
        'The first() method is not supported for domain classes that have composite keys.' == ex.message
        
        when:
        PersonWithCompositeKey.first('firstName')

        then:
        ex = thrown()
        'The first() method is not supported for domain classes that have composite keys.' == ex.message
        
        when:
        PersonWithCompositeKey.last()

        then:
        ex = thrown()
        'The last() method is not supported for domain classes that have composite keys.' == ex.message
        
        when:
        PersonWithCompositeKey.last(sort: 'firstName')

        then:
        ex = thrown()
        'The last() method is not supported for domain classes that have composite keys.' == ex.message
        
        when:
        PersonWithCompositeKey.last('firstName')

        then:
        ex = thrown()
        'The last() method is not supported for domain classes that have composite keys.' == ex.message
    }
    
    @Override
    List getDomainClasses() {
        [SimpleWidget, PersonWithCompositeKey, SimpleWidgetWithNonStandardId]
    }
}

@Entity
class SimpleWidget implements Serializable {
    Long id
    Long version
    String name
    String spanishName
}

@Entity
class SimpleWidgetWithNonStandardId implements Serializable {
    Long myIdentifier
    Long version
    String name
    static mapping = {
        id name: 'myIdentifier'
    }
}

@Entity
class PersonWithCompositeKey implements Serializable {
    Long version
    String firstName
    String lastName
    Integer age
    static mapping = {
        id composite: ['lastName', 'firstName']
    }
}
