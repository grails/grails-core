class ApplicationBootStrap {

     def init = { servletContext ->     
		System.setProperty("javax.xml.transform.TransformerFactory","org.apache.xalan.processor.TransformerFactoryImpl")
     }
     def destroy = {
     }
} 