package org.grails.samples.web

import org.grails.samples.Pet
import org.grails.samples.Visit
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus;

/**
 * JavaBean form controller that is used to add a new <code>Visit</code> to
 * the system.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Graeme Rocher 
 */
@Controller
@RequestMapping("/addVisit.do")
@SessionAttributes("visit")
class AddVisitForm {

    @InitBinder
    void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.disallowedFields = [ 'id'] as String[]
    }

	@RequestMapping(method = RequestMethod.GET)
	String setupForm(@RequestParam("petId") int petId, Model model) {
		Pet pet = Pet.get(petId)
		Visit visit = new Visit();
		pet.addToVisits(visit);
		model.addAttribute("visit", visit);
		return "visitForm";
	}

	@RequestMapping(method = RequestMethod.POST)
	String processSubmit(@ModelAttribute("visit") Visit visit, BindingResult result, SessionStatus status) {
		if (!visit.validate()) {
			visit.errors.allErrors.each { result.addError it }			
			return "visitForm";
		}
		else {
			visit.save()
			status.setComplete();
			return "redirect:owner.do?ownerId=" + visit.pet.owner.id
		}
	}

}
