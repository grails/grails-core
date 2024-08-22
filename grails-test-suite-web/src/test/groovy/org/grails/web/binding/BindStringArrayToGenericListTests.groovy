package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindStringArrayToGenericListTests extends Specification implements ControllerUnitTest<MenuController>, DomainUnitTest<Menu> {

    void testBindStringArrayToGenericList() {
        when:
        params.name = "day"
        params.items = ['rice', 'soup']as String[]
        def model = controller.save()

        then:
        ['rice', 'soup'] == model.menu.items
    }
}

@Artefact('Controller')
class MenuController {

    def save = {
        def m = new Menu(params)
        [menu:m]
    }
}

@Entity
class Menu {

    String name
    static hasMany = [items: String]

    List<String> items
}
