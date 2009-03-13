
package org.grails.samples.web

import org.grails.samples.Owner
import org.grails.samples.Vet
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.beans.factory.InitializingBean
import org.grails.samples.Speciality
import org.grails.samples.PetType


/**
 * Annotation-driven <em>MultiActionController</em> that handles all non-form
 * URL's.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 */
@Controller
public class ClinicController implements InitializingBean{

    public void afterPropertiesSet() {
	    if(!Speciality.count()) {
			def rad =	new Speciality(name:"radiology").save()
			def sur =	new Speciality(name:"surgery").save()
			def den =	new Speciality(name:"dentistry").save()

			new Vet(firstName:"James", lastName:"Carter").save()
			new Vet(firstName:"Helen", lastName:"Leary")
					.addToSpecialities(rad)
					.save()
			new Vet(firstName:"Linda", lastName:"Douglas")
					.addToSpecialities(sur)
					.addToSpecialities(den)
					.save()
			new Vet(firstName:"Rafael", lastName:"Ortega")
					.addToSpecialities(sur)
					.save()
			new Vet(firstName:"Henry", lastName:"Stevens")
					.addToSpecialities(rad)
					.save()
			new Vet(firstName:"Sharon", lastName:"Jenkins").save()

			['dog', 'lizard','cat', 'snake','bird', 'hamster'].each {
							new PetType(name:it).save()
			}

		}
    }


    /**
	 * Custom handler for the welcome view.
	 * <p>
	 * Note that this handler relies on the RequestToViewNameTranslator to
	 * determine the logical view name based on the request URL: "/welcome.do"
	 * -&gt; "welcome".
	 */
	@RequestMapping("/welcome.do")
	public void welcomeHandler() {
	}

	/**
	 * Custom handler for displaying vets.
	 * <p>
	 * Note that this handler returns a plain {@link ModelMap} object instead of
	 * a ModelAndView, thus leveraging convention-based model attribute names.
	 * It relies on the RequestToViewNameTranslator to determine the logical
	 * view name based on the request URL: "/vets.do" -&gt; "vets".
	 *
	 * @return a ModelMap with the model attributes for the view
	 */
	@RequestMapping("/vets.do")
	public ModelMap vetsHandler() {
		return new ModelMap(Vet.list())
	}

	/**
	 * Custom handler for displaying an owner.
	 * <p>
	 * Note that this handler returns a plain {@link ModelMap} object instead of
	 * a ModelAndView, thus leveraging convention-based model attribute names.
	 * It relies on the RequestToViewNameTranslator to determine the logical
	 * view name based on the request URL: "/owner.do" -&gt; "owner".
	 *
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@RequestMapping("/owner.do")
	public ModelMap ownerHandler(@RequestParam("ownerId") int ownerId) {
		return new ModelMap(Owner.get(ownerId))
	}

}
