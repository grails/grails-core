package org.codehaus.groovy.grails.orm.hibernate
/**
 *
 * Test for GRAILS-3178
 * 
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 7, 2008
 */

public class CriteriaListDistinctTests extends AbstractGrailsHibernateTests{

  protected void onSetUp() {
    gcl.parseClass('''
class PlantCategory {
    Long id
    Long version
    Set plants
    String name

    static hasMany = [plants:Plant]
}

class Plant {
    Long id
    Long version
    boolean goesInPatch
    String name
}
''')
  }



  void testListDistinct() {
    def PlantCategory = ga.getDomainClass("PlantCategory").clazz
    def Plant = ga.getDomainClass("Plant").clazz

    assert PlantCategory.newInstance(name:"leafy")
                   .addToPlants(goesInPatch:true, name:"lettuce")
                   .addToPlants(goesInPatch:true, name:"cabbage")
                   .save(flush:true)

    assert PlantCategory.newInstance(name:"orange")
                   .addToPlants(goesInPatch:true, name:"carrots")
                   .addToPlants(goesInPatch:true, name:"pumpkin")
                   .save(flush:true)


    assert PlantCategory.newInstance(name:"grapes")
                   .addToPlants(goesInPatch:false, name:"red")
                   .addToPlants(goesInPatch:false, name:"white")
                   .save(flush:true)



    session.clear()


    def categories = PlantCategory.createCriteria().listDistinct {
          plants {
              eq('goesInPatch', true)
          }
          order('name', 'asc')
      }

    assert categories
    assertEquals 2, categories.size()
    assertEquals "leafy", categories[0].name
    assertEquals "orange", categories[1].name
  }


  void testListDistinct2() {
    def PlantCategory = ga.getDomainClass("PlantCategory").clazz
    def Plant = ga.getDomainClass("Plant").clazz

    assert PlantCategory.newInstance(name:"leafy")
                   .addToPlants(goesInPatch:true, name:"lettuce")
                   .addToPlants(goesInPatch:true, name:"cabbage")
                   .save(flush:true)

    assert PlantCategory.newInstance(name:"orange")
                   .addToPlants(goesInPatch:true, name:"carrots")
                   .addToPlants(goesInPatch:true, name:"pumpkin")
                   .save(flush:true)


    assert PlantCategory.newInstance(name:"grapes")
                   .addToPlants(goesInPatch:false, name:"red")
                   .addToPlants(goesInPatch:true, name:"white")
                   .save(flush:true)



    session.clear()


    def categories = PlantCategory.createCriteria().listDistinct {
          plants {
              eq('goesInPatch', true)
          }
          order('name', 'asc')
      }

    assert categories
    assertEquals 3, categories.size()
    assertEquals "grapes", categories[0].name
    assertEquals "leafy", categories[1].name
    assertEquals "orange", categories[2].name
  }
}