class BookmarkTagLib {  
	Writer out
	
	def repeat = { attrs,body ->
		attrs.times?.toInteger().times {
			body(it)
		}
	}
	def editInPlace = { attrs, body ->
		def rows = attrs.rows ? attrs.rows : 0;
		def cols = attrs.cols ? attrs.cols : 0;
		def id = attrs.remove('id')
		out << "<span id='${id}'>"
			body()
		out << "</span>"
		out << "<script type='text/javascript'>"
        out << "new Ajax.InPlaceEditor('${id}', '"
			createLink(attrs)
		out << "',{"
		if(rows)
			out << "rows:${rows},"
		if(cols)
			out << "cols:${cols},"
		if(attrs.paramName) {
			out <<  "callback: function(form, value) { return '${attrs.paramName}=' + escape(value) }"
		}
		out << "});"
		out << "</script>"
	}
}