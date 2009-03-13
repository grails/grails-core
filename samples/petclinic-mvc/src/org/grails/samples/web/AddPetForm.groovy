package org.grails.samples.web

import org.grails.samples.Owner
import org.grails.samples.Pet
import org.grails.samples.PetType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus;

/**
 * JavaBean form controller that is used to add a new <code>Pet</code> to the
 * system.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Graeme Rocher
 */
@Controller
@RequestMapping("/addPet.do")
@SessionAttributes("pet")
class AddPetForm {


	@ModelAttribute("types")
	Collection<PetType> populatePetTypes() { PetType.list() }

    @InitBinder
    void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.disallowedFields = [ 'id'] as String[]
    }

    @RequestMapping(method = RequestMethod.GET)
	String setupForm(@RequestParam("ownerId") int ownerId, Model model) {
		Owner owner = Owner.get(ownerId)
		Pet pet = new Pet();
		owner.addToPets(pet);
		model.addAttribute("pet", pet);
		return "petForm";
	}

	@RequestMapping(method = RequestMethod.POST)
	String processSubmit(@ModelAttribute("pet") Pet pet, BindingResult result, SessionStatus status) {

		if (!pet.validate()) {
			pet.errors.allErrors.each { result.addError it }
			return "petForm";
		}
		else {
			pet.save()
			status.setComplete();
			return "redirect:owner.do?ownerId=" + pet.owner.id
		}
	}

}
