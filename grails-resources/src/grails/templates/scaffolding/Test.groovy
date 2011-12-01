<%=packageName ? "package ${packageName}\n\n" : ''%>

import org.junit.*
import grails.test.mixin.*

@TestFor(${className}Controller)
@Mock(${className})
class ${className}ControllerTests {


    def populateValidParams(params) {
      assert params != null
      // TODO: Populate valid properties like...
      //params["name"] = 'someValidName'
    }

    void testIndex() {
        controller.index()
        assert "/$propertyName/list" == response.redirectedUrl
    }

    void testList() {

        def model = controller.list()

        assert model.${propertyName}InstanceList.size() == 0
        assert model.${propertyName}InstanceTotal == 0
    }

    void testCreate() {
       def model = controller.create()

       assert model.${propertyName}Instance != null
    }

    void testSave() {
        controller.save()

        assert model.${propertyName}Instance != null
        assert view == '/${propertyName}/create'

        response.reset()

        populateValidParams(params)
        controller.save()

        assert response.redirectedUrl == '/${propertyName}/show/1'
        assert controller.flash.message != null
        assert ${className}.count() == 1
    }

    void testShow() {
        controller.show()

        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'


        populateValidParams(params)
        def ${propertyName} = new ${className}(params)

        assert ${propertyName}.save() != null

        params.id = ${propertyName}.id

        def model = controller.show()

        assert model.${propertyName}Instance == ${propertyName}
    }

    void testEdit() {
        controller.edit()

        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'


        populateValidParams(params)
        def ${propertyName} = new ${className}(params)

        assert ${propertyName}.save() != null

        params.id = ${propertyName}.id

        def model = controller.edit()

        assert model.${propertyName}Instance == ${propertyName}
    }

    void testUpdate() {
        controller.update()

        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'

        response.reset()


        populateValidParams(params)
        def ${propertyName} = new ${className}(params)

        assert ${propertyName}.save() != null

        // test invalid parameters in update
        params.id = ${propertyName}.id
        //TODO: add invalid values to params object

        controller.update()

        assert view == "/${propertyName}/edit"
        assert model.${propertyName}Instance != null

        ${propertyName}.clearErrors()

        populateValidParams(params)
        controller.update()

        assert response.redirectedUrl == "/${propertyName}/show/\$${propertyName}.id"
        assert flash.message != null

        //test outdated version number
        response.reset()
        ${propertyName}.clearErrors()

        populateValidParams(params)
        params.id = ${propertyName}.id
        params.version = -1
        controller.update()

        assert view == "/${propertyName}/edit"
        assert model.${propertyName}Instance != null
        assert model.${propertyName}Instance.errors.getFieldError('version')
        assert flash.message != null
    }

    void testDelete() {
        controller.delete()
        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'

        response.reset()

        populateValidParams(params)
        def ${propertyName} = new ${className}(params)

        assert ${propertyName}.save() != null
        assert ${className}.count() == 1

        params.id = ${propertyName}.id

        controller.delete()

        assert ${className}.count() == 0
        assert ${className}.get(${propertyName}.id) == null
        assert response.redirectedUrl == '/${propertyName}/list'
    }
}
