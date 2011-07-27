package grails.test.mixin

import grails.test.mixin.webflow.WebFlowUnitTestMixin
import org.junit.Test
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/12/11
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
@TestMixin([WebFlowUnitTestMixin, DomainClassUnitTestMixin])
class WebFlowUnitTestMixinTests {



    @Test
    void testEatBreakfast_EggsFailValidation() {
        mockDomain(Meal)
        mockController(MealController)

        def meal = new Meal()
        meal.eggs = true
        conversation.meal = meal

        breakfastFlow.eatBreakfast.action()

        assert 'unableToEatBreakfast' == stateTransition
        assert !conversation.meal.hotSauce
        assert conversation.meal.hasErrors()
    }

    @Test
    void testFlowExecution() {
        mockController(MealController)

        breakfastFlow.init.action()

        assert conversation.meal != null
    }

    @Test
    void testExecuteTransitionAction() {
        mockDomain(Meal)
        mockController(MealController)
        def meal = new Meal()
        conversation.meal = meal

        params.reason = "Feeling Sick"

        def event = breakfastFlow.chooseMainDish.on.nothing.action()

        assert event == 'success'
        assert 'end' == stateTransition
        assert params.reason == conversation.meal.skipReason
        assert conversation.meal.id
    }


    @Test
    void testChooseMainDish_TransitionNothingAction_BadReason() {
        mockDomain(Meal)
        mockController(MealController)
        def meal = new Meal()
        conversation.meal = meal

        params.reason = ""

        def event = breakfastFlow.chooseMainDish.on.nothing.action()

        assert 'error' == event
        assert 'chooseMainDish' == stateTransition
        assert !conversation.meal.skipReason
        assert !conversation.meal.id
    }

    @Test
    void testGeneralTransitionStructure() {
        mockController(MealController)

        assert 'chooseMainDish' == breakfastFlow.init.on.success.to

        assert 'prepareEggs' == breakfastFlow.chooseMainDish.on.eggs.to
        assert 'prepareToast' == breakfastFlow.chooseMainDish.on.toast.to
        assert 'end' == breakfastFlow.chooseMainDish.on.nothing.to

        assert 'eatBreakfast' == breakfastFlow.prepareEggs.on.success.to
        assert 'chooseMainDish' == breakfastFlow.prepareEggs.on.errors.to

        assert 'eatBreakfast' == breakfastFlow.prepareToast.on.success.to
        assert 'chooseMainDish' == breakfastFlow.prepareToast.on.errors.to

        assert 'beHappy' == breakfastFlow.eatBreakfast.on.success.to
        assert 'chooseMainDish' == breakfastFlow.eatBreakfast.on.unableToEatBreakfast.to

        assert 'end' == breakfastFlow.beHappy.on.done.to
        assert 'chooseMainDish' == breakfastFlow.beHappy.on.eatMore.to

        assert !breakfastFlow.end.on
    }

}

@Entity
class Meal {
    String skipReason
    boolean eggs
    boolean hotSauce

    void skip(String reason) {
        skipReason = reason
    }

    static constraints = {
        skipReason blank:false
        hotSauce validator: { val, obj ->
            return !(val == false && obj.eggs == true)
        }
    }
}
class MealController {

    def mealService = [prepareForBreakfast:{ new Meal() }, addHotSauce:{ },addButter:{} ]

    def prepareEggsFlow = {
        crackEggs {
            on 'sucess' to 'end'
        }
        end()
    }
    def prepareToastFlow = {
        sliceBread {
            on 'sucess' to 'end'
        }
        end()
    }
    final breakfastFlow = {
        init {
            action {
                conversation.meal = mealService.prepareForBreakfast()
            }
            on('success').to('chooseMainDish')
        }

        chooseMainDish {
            on('eggs').to('prepareEggs')
            on('toast').to('prepareToast')
            on('nothing'){
                conversation.meal.skip(params.reason)
                if(!conversation.meal.save()) {
                    return error()
                }
                return success()
            }.to('end')
        }

        prepareEggs {
            subflow(prepareEggsFlow)
            on('success').to('eatBreakfast')
            on('errors').to('chooseMainDish')
        }

        prepareToast{
            subflow(prepareToastFlow)
            on('success').to('eatBreakfast')
            on('errors').to('chooseMainDish')
        }

        eatBreakfast {
            action {
                def meal = conversation.meal
                if(meal.isEggs()) {
                    mealService.addHotSauce(meal)
                } else if(meal.isToast()) {
                    mealService.addButter(meal)
                }


                def valid = meal.validate()
                if(!valid) {
                    return unableToEatBreakfast()
                }
                return success()
            }
            on('success').to('beHappy')
            on('unableToEatBreakfast').to('chooseMainDish')
        }

        beHappy {
            on('done') {
                conversation.meal.save()
            }.to('end')
            on('eatMore').to('chooseMainDish')
        }

        end()
    }
}