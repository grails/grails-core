package org.grails.samples.web

import org.grails.samples.Owner
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus;

/**
 * JavaBean form controller that is used to add a new <code>Owner</code> to
 * the system.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Graeme Rocher
 */
@Controller
@RequestMapping("/addOwner.do")
@SessionAttributes(types = Owner.class)
class AddOwnerForm {



    @InitBinder
    void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.disallowedFields = [ 'id'] as String[]
    }

    @RequestMapping(method = RequestMethod.GET)
	String setupForm(Model model) {
		Owner owner = new Owner()
		model.addAttribute(owner)
		return "ownerForm"
	}

	@RequestMapping(method = RequestMethod.POST)
	String processSubmit(@ModelAttribute Owner owner, BindingResult result, SessionStatus status) {

		if (!owner.validate()) {
			owner.errors.allErrors.each { result.addError it }
			return "ownerForm";
		}
		else {
			owner.save()
			status.setComplete();
			return "redirect:owner.do?ownerId=" + owner.id
		}
	}

}
