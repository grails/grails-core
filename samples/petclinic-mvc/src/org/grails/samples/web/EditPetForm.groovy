package org.grails.samples.web

import org.grails.samples.Pet
import org.grails.samples.PetType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus;

/**
 * JavaBean Form controller that is used to edit an existing <code>Pet</code>.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Graeme Rocher
 */
@Controller
@RequestMapping("/editPet.do")
@SessionAttributes(types = Pet.class)
public class EditPetForm {

	@ModelAttribute("types")
	Collection<PetType> populatePetTypes() {
		return PetType.list()
	}

    @InitBinder
    void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.disallowedFields = ['id'] as String[]
    }

	@RequestMapping(method = RequestMethod.GET)
	String setupForm(@RequestParam("petId") int petId, Model model) {
		Pet pet = Pet.get(petId)
		model.addAttribute("pet", pet)
		return "petForm"
	}

	@RequestMapping(method = RequestMethod.POST)
	String processSubmit(@ModelAttribute Pet pet, BindingResult result, SessionStatus status) {
		if (!pet.validate()) {
			pet.errors.allErrors.each { result.addError it }			
			return "petForm"
		}
		else {
			pet.merge(flush:true)
			status.setComplete()
			return "redirect:owner.do?ownerId=" + pet.owner.id
		}
	}

}
