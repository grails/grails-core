package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FetchMode

class NamedCriteriaRelationshipTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [NamedCriteriaPlantCategory,
         NamedCriteriaPlant]
    }

    void testFetch() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        session.clear()

        // the code below relies on some implementation details which is a little brittle but should work...

        def query = NamedCriteriaPlantCategory.withPlantsInPatch
        query.list(fetch: [plants: 'lazy'])
        def crit = query.queryBuilder.instance
        def plantFetchMode = crit.getFetchMode('plants')
        assertEquals 'wrong fetch mode for plants', FetchMode.SELECT, plantFetchMode

        query.list(fetch: [plants: 'eager'])
        crit = query.queryBuilder.instance
        plantFetchMode = crit.getFetchMode('plants')
        assertEquals 'wrong fetch mode fro plants', FetchMode.JOIN, plantFetchMode
    }

    void testNamedQueryWithRelationshipInCriteria() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = NamedCriteriaPlantCategory.withPlantsInPatch.list()
        assertEquals 2, results.size()
        assertTrue 'leafy' in results*.name
        assertTrue 'groovy' in results*.name

        results = NamedCriteriaPlantCategory.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        assertTrue 'groovy' in results*.name
        assertTrue 'grapes' in results*.name

        results = NamedCriteriaPlantCategory.withPlantsInPatchThatStartWithG.list()
        assertEquals 1, results.size()
        assertEquals 'groovy', results[0].name
    }

    void testInvokingNamedQueryDefinedInAnotherDomainClass() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = NamedCriteriaPlantCategory.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        def names = results*.name
        assertTrue 'groovy' in names
        assertTrue 'grapes' in names
    }

    void testListDistinct() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:false, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = NamedCriteriaPlantCategory.withPlantsInPatch().listDistinct()

        assertEquals 2, categories.size()
        def names = categories*.name
        assertEquals 2, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
    }

    void testListDistinct2() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:true, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = NamedCriteriaPlantCategory.withPlantsInPatch.listDistinct()

        assertEquals 3, categories.size()
        def names = categories*.name
        assertEquals 3, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
        assertTrue 'grapes' in names
    }


}
