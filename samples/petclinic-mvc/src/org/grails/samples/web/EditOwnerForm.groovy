package org.grails.samples.web

import org.grails.samples.Owner
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus;

/**
 * JavaBean Form controller that is used to edit an existing <code>Owner</code>.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 */
@Controller
@RequestMapping("/editOwner.do")
@SessionAttributes(types = Owner.class)
class EditOwnerForm {


    @InitBinder
    void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.disallowedFields = [ 'id'] as String[]
    }

	@RequestMapping(method = RequestMethod.GET)
	String setupForm(@RequestParam("ownerId") int ownerId, Model model) {
		Owner owner = Owner.get(ownerId)
		model.addAttribute(owner);
		return "ownerForm";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String processSubmit(@ModelAttribute Owner owner, BindingResult result, SessionStatus status) {

		if (!owner.validate()) {
			owner.errors.allErrors.each { result.addError it }
			return "ownerForm";
		}
		else {
			owner.save(flush:true)
			status.setComplete();
			return "redirect:owner.do?ownerId=" + owner.id
		}
	}

}
