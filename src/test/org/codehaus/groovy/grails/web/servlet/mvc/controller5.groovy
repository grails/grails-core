import grails.util.OpenRicoBuilder

class NoViewController {
    Closure list = { request, response ->
        new OpenRicoBuilder(response).ajax { element(id:"test") { } }
        null
    }
}
