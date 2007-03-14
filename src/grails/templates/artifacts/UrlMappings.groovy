mappings {
  "/$controller/$action?/$id?"{
      constraints {
	     id(matches:/\d+/)
	  }
  }
}