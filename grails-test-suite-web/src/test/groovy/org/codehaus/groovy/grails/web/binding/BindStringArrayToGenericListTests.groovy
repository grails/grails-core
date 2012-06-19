package org.codehaus.groovy.grails.web.binding

import grails.persistence.Entity

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindStringArrayToGenericListTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getControllerClasses() {
        [MenuController]
    }

    @Override
    protected Collection<Class> getDomainClasses() {
        [Menu]
    }

    void testBindStringArrayToGenericList() {
        def controller = new MenuController()

        controller.params.name = "day"
        controller.params.items = ['rice', 'soup']as String[]

        def model = controller.save()

        assertEquals(['rice', 'soup'], model.menu.items)
    }
}

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
