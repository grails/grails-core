/*
 * FCKeditor - The text editor for internet
 * Copyright (C) 2003-2005 Frederico Caldeira Knabben
 * 
 * Licensed under the terms of the GNU Lesser General Public License:
 * 		http://www.opensource.org/licenses/lgpl-license.php
 * 
 * For further information visit:
 * 		http://www.fckeditor.net/
 * 
 * "Support Open Source software. What about a donation today?"
 * 
 * File Name: fckplugins.js
 * 	Defines the FCKPlugins object that is responsible for loading the Plugins.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKPlugins = FCK.Plugins = new Object() ;
FCKPlugins.ItemsCount = 0 ;
FCKPlugins.Loaded = false ;
FCKPlugins.Items = new Object() ;

// Set the defined plugins scripts paths.
for ( var i = 0 ; i < FCKConfig.Plugins.Items.length ; i++ )
{
	var oItem = FCKConfig.Plugins.Items[i] ;
	FCKPlugins.Items[ oItem[0] ] = new FCKPlugin( oItem[0], oItem[1], oItem[2] ) ;
	FCKPlugins.ItemsCount++ ;
}
	
FCKPlugins.Load = function()
{
	// Load all items.
	for ( var s in this.Items )
		this.Items[s].Load() ;
	
	// Mark as loaded.
	this.Loaded = true ;
	
	// This is a self destroyable function (must be called once).
	FCKPlugins.Load = null ;
}