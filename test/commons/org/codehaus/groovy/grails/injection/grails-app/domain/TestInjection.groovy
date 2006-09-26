class TestInjection {	
	String name
	
	def hasMany = [ presets : PresetIdObject.class,
	                lasts : ZLoadedLast.class]
	                
}