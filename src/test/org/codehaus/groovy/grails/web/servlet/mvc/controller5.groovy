class NoViewController {
    Closure list = {
      request, response ->

      new grails.util.OpenRicoBuilder(response).ajax { element(id:"test") { } };
      return null;
   }
}