package org.codehaus.groovy.grails.web.commandobjects

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Specification

@TestFor(CommandController)
class CommandObjectNullabilitySpec extends Specification {

    @Issue('GRAILS-9686')
    void 'Test nullability'() {
        when:
        controller.createWidget()
        def widget = model.widget
        
        then:
        widget
        widget.hasErrors()
        widget.errors.errorCount == 3
        widget.errors.getFieldError('explicitlyNonNullableProperty').code == 'nullable'
        widget.errors.getFieldError('implicitlyNonNullableConstrainedProperty').code == 'nullable'
        widget.errors.getFieldError('implicitlyNonNullableUnconstrainedProperty').code == 'nullable'
    }

    @Issue('GRAILS-9686')
    void 'Test nullability inheritance'() {
        when:
        controller.createWidgetSubclass()
        def widget = model.widget
        
        then:
        widget
        widget.hasErrors()
        widget.errors.errorCount == 6
        widget.errors.getFieldError('explicitlyNonNullableProperty').code == 'nullable'
        widget.errors.getFieldError('implicitlyNonNullableConstrainedProperty').code == 'nullable'
        widget.errors.getFieldError('implicitlyNonNullableUnconstrainedProperty').code == 'nullable'
        widget.errors.getFieldError('subclassExplicitlyNonNullableProperty').code == 'nullable'
        widget.errors.getFieldError('subclassImplicitlyNonNullableConstrainedProperty').code == 'nullable'
        widget.errors.getFieldError('subclassImplicitlyNonNullableUnconstrainedProperty').code == 'nullable'
    }
}

@Artefact('Controller')
class CommandController {
    
    def createWidget(Widget widget) {
        render view: 'create', model: [widget: widget]
    }
    
    def createWidgetSubclass(WidgetSubclass widget) {
        render view: 'create', model: [widget: widget]
    }
}

class Widget {
    String explicitlyNullableProperty
    String explicitlyNonNullableProperty
    String implicitlyNonNullableConstrainedProperty
    String implicitlyNonNullableUnconstrainedProperty
    static String someStaticProperty
    
    
    static constraints = {
        explicitlyNullableProperty nullable: true
        explicitlyNonNullableProperty nullable: false
        implicitlyNonNullableConstrainedProperty matches: /[A-Z].*/
    }
}

class WidgetSubclass extends Widget {
    String subclassExplicitlyNullableProperty
    String subclassExplicitlyNonNullableProperty
    String subclassImplicitlyNonNullableConstrainedProperty
    String subclassImplicitlyNonNullableUnconstrainedProperty
    static String someSubclassStaticProperty
    
    static constraints = {
        subclassExplicitlyNullableProperty nullable: true
        subclassExplicitlyNonNullableProperty nullable: false
        subclassImplicitlyNonNullableConstrainedProperty matches: /[A-Z].*/
    }
}
