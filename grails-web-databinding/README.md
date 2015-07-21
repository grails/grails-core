## grails-web-databinding

This subproject includes a lot of code to support data binding.

The [GrailsWebDataBinder)[src/main/groovy/grails/web/databindeing/WebDataBinding.groovy)
class extends [SimpleDataBinder](../grails-databinding/src/main/groovy/grails/databinding/SimpleDataBinder.groovy) from
the (grails-databinding)[../grails-databinding] subproject and adds to it a lot of Grails specific behavior like
special handing of GORM entities, code specificaly relevant to binding web requests to objects and other behaviors.

The [WebDataBinding](src/main/groovy/grails/web/databinding/WebDataBinding.groovy) trait adds special methods
which support special binding usage patterns like `someObj.properties = request`.
