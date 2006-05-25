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
 * File Name: fckeditorapi.js
 * 	Create the FCKeditorAPI object that is available as a global object in
 * 	the page where the editor is placed in.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKeditorAPI ;

function FCKeditorAPI_GetInstance( instanceName )
{
	return this.__Instances[ instanceName ] ;
}

if ( !window.parent.FCKeditorAPI )
{
	// Make the FCKeditorAPI object available in the parent window.
	FCKeditorAPI = window.parent.FCKeditorAPI = new Object() ;
	FCKeditorAPI.__Instances = new Object() ;

	// Set the current version.
	FCKeditorAPI.Version = '2.2' ;

	// Function used to get a instance of an existing editor present in the 
	// page.
	FCKeditorAPI.GetInstance = FCKeditorAPI_GetInstance ;
}
else
	FCKeditorAPI = window.parent.FCKeditorAPI ;

// Add the current instance to the FCKeditorAPI's instances collection.
FCKeditorAPI.__Instances[ FCK.Name ] = FCK ;