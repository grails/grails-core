## grails-databinding

This subproject contains much of the core data binding code.  The main class here is
[SimpleDataBinder](./src/main/groovy/grails/databinding/SimpleDataBinder.groovy).  Most of the other code
 here exists to support that.  The real databinding used in a Grails app is
 [GrailsWebDataBinder](../grails-web-databinding/src/main/groovy/grails/web/databinding/WebDataBinding.groovy) which
 extends `SimpleDataBinder` and is defined in the `grails-web-databinding` subproject.  `SimpleDataBinder` is where
 much of the core data binding logic is defined. The `GrailsWebDataBinder` subclass defines a lot of the logic that
  is specific to data binding in the context of a Grails app.  For example, all of the GORM special handling that the
  data binder does is in `GrailsWebDataBinder`.
