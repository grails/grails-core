<%=packageName ? "package ${packageName}\n\n" : ''%>

import org.junit.*
import grails.test.mixin.*
import javax.servlet.http.HttpServletResponse

@TestFor(${className}Controller)
@Mock(${className})
class ${className}ControllerTests {

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
        assert response.status == HttpServletResponse.SC_METHOD_NOT_ALLOWED

        response.reset()
        request.method = 'POST'
        controller.save()

        assert model.${propertyName}Instance != null
        assert view == '/${propertyName}/create'

        response.reset()

        // TODO: Populate valid properties

        controller.save()

        assert response.redirectedUrl == '/${propertyName}/show/1'
        assert controller.flash.message != null
        assert ${className}.count() == 1
    }

    void testShow() {
        controller.show()

        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'


        def ${propertyName} = new ${className}()

        // TODO: populate domain properties

        assert ${propertyName}.save() != null

        params.id = ${propertyName}.id

        def model = controller.show()

        assert model.${propertyName}Instance == ${propertyName}
    }

    void testEdit() {
        controller.edit()

        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'


        def ${propertyName} = new ${className}()

        // TODO: populate valid domain properties

        assert ${propertyName}.save() != null

        params.id = ${propertyName}.id

        def model = controller.edit()

        assert model.${propertyName}Instance == ${propertyName}
    }

    void testUpdate() {

        controller.update()
        assert response.status == HttpServletResponse.SC_METHOD_NOT_ALLOWED

        response.reset()
        request.method = 'POST'
        controller.update()

        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'

        response.reset()


        def ${propertyName} = new ${className}()

        // TODO: populate valid domain properties

        assert ${propertyName}.save() != null

        // test invalid parameters in update
        params.id = ${propertyName}.id

        controller.update()

        assert view == "/${propertyName}/edit"
        assert model.${propertyName}Instance != null

        ${propertyName}.clearErrors()

        // TODO: populate valid domain form parameter
        controller.update()

        assert response.redirectedUrl == "/${propertyName}/show/\$${propertyName}.id"
        assert flash.message != null
    }

    void testDelete() {
        controller.delete()
        assert response.status == HttpServletResponse.SC_METHOD_NOT_ALLOWED

        response.reset()
        request.method = 'POST'
        controller.delete()
        assert flash.message != null
        assert response.redirectedUrl == '/${propertyName}/list'

        response.reset()

        def ${propertyName} = new ${className}()

        // TODO: populate valid domain properties
        assert ${propertyName}.save() != null
        assert ${className}.count() == 1

        params.id = ${propertyName}.id

        controller.delete()

        assert ${className}.count() == 0
        assert ${className}.get(${propertyName}.id) == null
        assert response.redirectedUrl == '/${propertyName}/list'
    }
}
