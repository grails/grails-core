package grails.test.mixin

import grails.persistence.Entity
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.webflow.WebFlowUnitTestMixin

import org.junit.Test

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
    void testSubflowArgs() {
        mockDomain(Meal)
        mockController(MealController)

        def subflow = breakfastFlow.prepareBacon.subflow

        assert 'prepareBacon' == breakfastFlow.prepareBacon.subflowArgs.action
        assert 'meal' == breakfastFlow.prepareBacon.subflowArgs.controller
        assert breakfastFlow.prepareBacon.subflowArgs.input.meal
    }

    @Test
    void testFlowOutput() {
        mockDomain(Meal)
        mockController(MealController)
        flow.baconFlow = 'bacon'

        prepareBaconFlow.end.output()

        assert 'bacon' == currentEvent.attributes.baconConst
        assert 'bacon' == currentEvent.attributes.baconValue
        assert 'bacon' == currentEvent.attributes.baconFlow
    }

    @Test
    void testFlowInputDefault() {
        mockDomain(Meal)
        mockController(MealController)

        def inputParams = [
                defaultBaconInput: 'bacon',
                requiredWithoutValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.defaultBaconInput
        assert 'bacon' == flow.requiredWithoutValueBaconInput
    }

    @Test
    void testFlowInputRequiredWithoutValue() {
        mockDomain(Meal)
        mockController(MealController)

        def inputParams = [
                defaultBaconInput: 'bacon'
        ]

        shouldFail MissingPropertyException, {
            prepareBaconFlow.input(inputParams)
        }

        inputParams = [
                defaultBaconInput: 'bacon',
                requiredWithoutValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.requiredWithoutValueBaconInput
    }

    @Test
    void testFlowInputRequiredWithValue() {
        mockDomain(Meal)
        mockController(MealController)

        def inputParams = [
                requiredWithoutValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'baconValue' == flow.requiredWithValueBaconInput

        inputParams = [
                requiredWithoutValueBaconInput: 'bacon',
                requiredWithValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.requiredWithValueBaconInput
    }

    @Test
    void testFlowInputNotRequiredWithValue() {
        mockDomain(Meal)
        mockController(MealController)

        def inputParams = [
                requiredWithoutValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'baconValue' == flow.notRequiredWithValueBaconInput

        inputParams = [
                requiredWithoutValueBaconInput: 'bacon',
                notRequiredWithValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.notRequiredWithValueBaconInput
    }

    @Test
    void testFlowInputClosure() {
        mockDomain(Meal)
        mockController(MealController)

        flow.closureBaconInputValue = 'bacon'

        def inputParams = [
                requiredWithoutValueBaconInput: 'bacon',
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.closureBaconInput
    }

    @Test
    void testFlowInputClosureValue() {
        mockDomain(Meal)
        mockController(MealController)

        flow.closureWithValueBaconInputValue = 'bacon'

        def inputParams = [
                requiredWithoutValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.closureWithValueBaconInput
    }

    @Test
    void testFlowInputValue() {
        mockDomain(Meal)
        mockController(MealController)

        def inputParams = [
                requiredWithoutValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'baconValue' == flow.defaultValueBaconInput

        inputParams = [
                requiredWithoutValueBaconInput: 'bacon',
                defaultValueBaconInput: 'bacon'
        ]

        prepareBaconFlow.input(inputParams)

        assert 'bacon' == flow.defaultValueBaconInput

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
     def prepareBaconFlow = {
        input {
            defaultBaconInput()
            requiredWithoutValueBaconInput required: true
            requiredWithValueBaconInput required: true, value: 'baconValue'
            notRequiredWithValueBaconInput required: false, value: 'baconValue'
            defaultValueBaconInput 'baconValue'
            closureBaconInput { flow.closureBaconInputValue }
            closureWithValueBaconInput value: { flow.closureWithValueBaconInputValue }
        }
        fryBacon {
            on 'sucess' to 'end'
        }
        end {
            output {
                baconConst("bacon")
                baconValue(value: "bacon")
                baconFlow { flow.baconFlow }
            }
        }
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
            on('bacon').to('prepareBacon')
            on('nothing') {
                conversation.meal.skip(params.reason)
                if (!conversation.meal.save()) {
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

        prepareBacon{
            subflow(controller: 'meal', action: 'prepareBacon', input: [meal: new Meal()])
            on('success').to('eatBreakfast')
            on('errors').to('chooseMainDish')
        }

        eatBreakfast {
            action {
                def meal = conversation.meal
                if (meal.isEggs()) {
                    mealService.addHotSauce(meal)
                } else if (meal.isToast()) {
                    mealService.addButter(meal)
                }

                def valid = meal.validate()
                if (!valid) {
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
