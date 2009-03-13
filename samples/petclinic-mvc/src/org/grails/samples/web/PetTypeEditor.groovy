package org.grails.samples.web

import java.beans.PropertyEditorSupport
import org.grails.samples.PetType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
class PetTypeEditor extends PropertyEditorSupport {

	@Override
	void setAsText(String text)  {
        value = PetType.findByName(text)
	}

}
