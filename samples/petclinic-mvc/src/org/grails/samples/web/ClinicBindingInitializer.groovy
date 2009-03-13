package org.grails.samples.web

import java.text.SimpleDateFormat
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.beans.propertyeditors.StringTrimmerEditor
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.support.WebBindingInitializer
import org.springframework.web.context.request.WebRequest
import org.grails.samples.PetType

/**
 * Shared WebBindingInitializer for PetClinic's custom editors.
 *
 * <p>Alternatively, such init-binder code may be put into
 * {@link org.springframework.web.bind.annotation.InitBinder}
 * annotated methods on the controller classes themselves.
 *
 * @author Juergen Hoeller
 * @author Graeme Rocher
 */
class ClinicBindingInitializer implements WebBindingInitializer {

	void initBinder(WebDataBinder binder, WebRequest request) {
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
		dateFormat.lenient = false
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false))
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(false))
		binder.registerCustomEditor(PetType.class, new PetTypeEditor())
	}

}
