package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class BindStringArrayToGenericListTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Menu {

  String name
  static hasMany = [items: String]

  List<String> items

}

class MenuController {

    def save = {
        def m = new Menu(params)
        [menu:m]
    }
}
''')
    }

    void testBindStringArrayToGenericList() {
        def controller = ga.getControllerClass("MenuController").newInstance()

        controller.params.name = "day"
        controller.params.items = ['rice', 'soup'] as String[]

        def model = controller.save()

        assertEquals( ['rice', 'soup'], model.menu.items )

    }
    

}