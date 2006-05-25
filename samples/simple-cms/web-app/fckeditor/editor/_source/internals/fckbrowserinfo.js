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
 * File Name: fckbrowserinfo.js
 * 	Defines the FCKBrowserInfo object that hold some browser informations.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKBrowserInfo ;

if ( !( FCKBrowserInfo = NS.FCKBrowserInfo ) )
{
	FCKBrowserInfo = NS.FCKBrowserInfo = new Object() ;

	var sAgent = navigator.userAgent.toLowerCase() ;

	FCKBrowserInfo.IsIE			= ( sAgent.indexOf("msie") != -1 ) ;
	FCKBrowserInfo.IsGecko		= !FCKBrowserInfo.IsIE ;
	FCKBrowserInfo.IsSafari		= ( sAgent.indexOf("safari") != -1 ) ;
	FCKBrowserInfo.IsNetscape	= ( sAgent.indexOf("netscape") != -1 ) ;
}
