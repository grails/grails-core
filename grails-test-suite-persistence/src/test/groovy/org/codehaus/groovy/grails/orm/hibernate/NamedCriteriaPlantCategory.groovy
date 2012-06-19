package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class NamedCriteriaPlantCategory {
    Long id
    Long version
    Set plants
    String name

    static hasMany = [plants:NamedCriteriaPlant]

    static namedQueries = {
        withPlantsInPatch {
            plants {
                eq 'goesInPatch', true
            }
        }
        withPlantsThatStartWithG {
            plants {
                like 'name', 'G%'
            }
        }
        withPlantsInPatchThatStartWithG {
            withPlantsInPatch()
            withPlantsThatStartWithG()
        }
        withPlantsThatStartWithG {
            plants {
                nameStartsWithG()
            }
        }
    }
}
