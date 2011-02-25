package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FetchMode

class NamedCriteriaRelationshipTests extends AbstractGrailsHibernateTests {
    
    protected getDomainClasses() {
        [NamedCriteriaPlantCategory,
         NamedCriteriaPlant]
    }

    void testFetch() {
        def plantCategoryClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        session.clear()

        // the code below relies on some implementation details which is a little brittle but should work...

        def query = plantCategoryClass.withPlantsInPatch
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
        def plantCategoryClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = plantCategoryClass.withPlantsInPatch.list()
        assertEquals 2, results.size()
        assertTrue 'leafy' in results*.name
        assertTrue 'groovy' in results*.name

        results = plantCategoryClass.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        assertTrue 'groovy' in results*.name
        assertTrue 'grapes' in results*.name

        results = plantCategoryClass.withPlantsInPatchThatStartWithG.list()
        assertEquals 1, results.size()
        assertEquals 'groovy', results[0].name
    }

    void testInvokingNamedQueryDefinedInAnotherDomainClass() {
        def plantCategoryClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = plantCategoryClass.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        def names = results*.name
        assertTrue 'groovy' in names
        assertTrue 'grapes' in names
    }

    void testListDistinct() {
        def plantCategoryClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:false, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = plantCategoryClass.withPlantsInPatch().listDistinct()

        assertEquals 2, categories.size()
        def names = categories*.name
        assertEquals 2, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
    }

    void testListDistinct2() {
        def plantCategoryClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:true, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = plantCategoryClass.withPlantsInPatch.listDistinct()

        assertEquals 3, categories.size()
        def names = categories*.name
        assertEquals 3, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
        assertTrue 'grapes' in names
    }


}
