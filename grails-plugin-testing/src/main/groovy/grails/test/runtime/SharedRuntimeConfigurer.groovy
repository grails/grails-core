package grails.test.runtime

/**
 * The class given as parameter to SharedRuntime has to implement this interface.
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
interface SharedRuntimeConfigurer {
    /**
     * The list of features that should be available in the runtime.
     * when null is returned, all available features will be selected.
     *  
     * @return required features
     */
    String[] getRequiredFeatures()
    /**
     * This method will be called after a new runtime has been instantiated
     * 
     * @param runtime the new runtime instance
     */
    void configure(TestRuntime runtime)
}
