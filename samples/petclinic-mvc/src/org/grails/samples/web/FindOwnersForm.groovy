package org.grails.samples.web

import org.grails.samples.Owner
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * JavaBean Form controller that is used to search for <code>Owner</code>s by
 * last name.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Graeme Rocher
 */
@Controller
@RequestMapping("/findOwners.do")
public class FindOwnersForm {


    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.disallowedFields = [ 'id'] as String[]
    }

	@RequestMapping(method = RequestMethod.GET)
	public  String setupForm(Model model) {
		model.addAttribute("owner", new Owner());
		return "findOwners";
	}

	@RequestMapping(method = RequestMethod.POST)
	public  String processSubmit(Owner owner, BindingResult result, Model model) {
		// find owners by last name
		Collection<Owner> results = Owner.findAllByLastName(owner.lastName)
		if (results.size() < 1) {
			// no owners found
			result.rejectValue("lastName", "notFound", "not found")
			return "findOwners"
		}
		if (results.size() > 1) {
			// multiple owners found
			model.addAttribute("selections", results)
			return "owners"
		}
		else {
			// 1 owner found
			owner = results.iterator().next()
			return "redirect:owner.do?ownerId=" + owner.getId()
		}
	}

}
