package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindStringArrayToGenericListTests
        implements ControllerUnitTest<MenuController>, DataTest {

    Class[] getDomainClassesToMock() {
        Menu
    }

    @Test
    void testBindStringArrayToGenericList() {
        params.name = "day"
        params.items = ['rice', 'soup']as String[]

        def model = controller.save()

        assertEquals(['rice', 'soup'], model.menu.items)
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
