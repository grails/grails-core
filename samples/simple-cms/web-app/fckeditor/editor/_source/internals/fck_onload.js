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
 * File Name: fck_onload.js
 * 	This is the script that is called when the editor page is loaded inside
 * 	its IFRAME. It's the editor startup.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

// Disable the context menu in the editor (areas outside the editor area).
function Window_OnContextMenu( e )
{
	if ( e )
		e.preventDefault() ;	// The Gecko way.
	else
	{
		if ( event.srcElement == document.getElementById('eSourceField') )
			return true ;
	}

	return false ;				// The IE way.
}
window.document.oncontextmenu = Window_OnContextMenu ;

// Gecko browsers doens't calculate well that IFRAME size so we must
// recalculate it every time the window size changes.
if ( FCKBrowserInfo.IsGecko )
{
	function Window_OnResize()
	{
		var oFrame = document.getElementById('eEditorArea') ;
		oFrame.height = 0 ;

		var oCell = document.getElementById( FCK.EditMode == FCK_EDITMODE_WYSIWYG ? 'eWysiwygCell' : 'eSource' ) ;
		var iHeight = oCell.offsetHeight ;

		oFrame.height = iHeight - 2 ;
	}
	window.onresize = Window_OnResize ;
}

if ( FCKBrowserInfo.IsIE )
{
	var aCleanupDocs = new Array() ;
	aCleanupDocs[0] = document ;

	// On IE, some circular references must be cleared to avoid memory leak.
	function Window_OnBeforeUnload()
	{
//		if ( typeof( FCKToolbarSet ) != 'undefined' )
//			FCKToolbarSet.Collapse() ;

		var d, e ;

		var j = 0 ;
		while ( ( d = aCleanupDocs[j++] ) )
		{
			var i = 0 ;
			while ( ( e = d.getElementsByTagName("DIV").item(i++) ) )
			{
				if ( e.FCKToolbarButton )
					e.FCKToolbarButton = null ;

				if ( e.FCKSpecialCombo )
					e.FCKSpecialCombo = null ;

				if ( e.Command )
					e.Command = null ;
			}

			i = 0 ;
			while ( ( e = d.getElementsByTagName("TR").item(i++) ) )
			{
				if ( e.FCKContextMenuItem )
					e.FCKContextMenuItem = null ;
			}

			aCleanupDocs[j] = null ;
		}
		
		if ( typeof( FCKTempBin ) != 'undefined' )
			FCKTempBin.Reset() ;
	}
	window.attachEvent( "onunload", Window_OnBeforeUnload ) ;
}

// The editor startup follows these steps:
//		1. Load the editor main page (fckeditor.html).
//		2. Load the main configuration file (fckconfig.js)
//		3. Process the configurations set directly in the page code (just load then).
//		4. Override the configurations with the custom config file (if set).
//		5. Override the configurations with that ones set directly in the page code.
//		6. Load the editor skin styles CSS files.
//		7. Load the first part of tha main scripts.
//		8. Load the language file.
//		9. Start the editor.

// Start the editor as soon as the window is loaded.
function Window_OnLoad()
{
	// There is a bug on Netscape when rendering the frame. It goes over the
	// right border. So we must correct it.
	if ( FCKBrowserInfo.IsNetscape )
		document.getElementById('eWysiwygCell').style.paddingRight = '2px' ;

	LoadConfigFile() ;
}
window.onload = Window_OnLoad ;

function LoadConfigFile()
{
	FCKScriptLoader.OnEmpty = ProcessHiddenField ;

	// First of all load the configuration file.
	FCKScriptLoader.AddScript( '../fckconfig.js' ) ;
}

function ProcessHiddenField()
{
	FCKConfig.ProcessHiddenField() ;

	LoadCustomConfigFile() ;
}

function LoadCustomConfigFile()
{
	// Load the custom configurations file (if defined).
	if ( FCKConfig.CustomConfigurationsPath.length > 0 )
	{
		// Wait for the configuration file to be loaded to call the "LoadPageConfig".
		FCKScriptLoader.OnEmpty = LoadPageConfig ;

		FCKScriptLoader.AddScript( FCKConfig.CustomConfigurationsPath ) ;
	}
	else
	{
		// Load the page defined configurations immediately.
		LoadPageConfig() ;
	}
}

function LoadPageConfig()
{
	FCKConfig.LoadPageConfig() ;
	
	if ( FCKConfig.AllowQueryStringDebug && (/fckdebug=true/i).test( window.top.location.search ) )
		FCKConfig.Debug = true ;

	// Load the styles for the configured skin.
	LoadStyles() ;
}

function LoadStyles()
{
	if( window.console ) window.console.log( 'LoadStyles()' ) ;	// @Packager.Compactor.RemoveLine

	FCKScriptLoader.OnEmpty = LoadScripts ;

	// Load the active skin CSS.
	FCKScriptLoader.AddScript( FCKConfig.SkinPath + 'fck_editor.css' ) ;
	FCKScriptLoader.AddScript( FCKConfig.SkinPath + 'fck_contextmenu.css' ) ;
}

function LoadScripts()
{
	if( window.console ) window.console.log( 'LoadScripts()' ) ;	// @Packager.Compactor.RemoveLine

	FCKScriptLoader.OnEmpty = null ;

	// @Packager.Compactor.Remove.Start
	var sSuffix = FCKBrowserInfo.IsIE ? 'ie' : 'gecko' ;

	FCKScriptLoader.AddScript( '_source/internals/fckdebug.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fcktools.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fcktools_' + sSuffix + '.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fckregexlib.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fcklanguagemanager.js' ) ;
	FCKScriptLoader.AddScript( '_source/classes/fckevents.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fckxhtmlentities.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fckxhtml.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fckxhtml_' + sSuffix + '.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fckcodeformatter.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fckundo_' + sSuffix + '.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fck_1.js' ) ;
	FCKScriptLoader.AddScript( '_source/internals/fck_1_' + sSuffix + '.js' ) ;
	// @Packager.Compactor.Remove.End

	/* @Packager.Compactor.RemoveLine
	if ( FCKBrowserInfo.IsIE )
		FCKScriptLoader.AddScript( 'js/fckeditorcode_ie_1.js' ) ;
	else
		FCKScriptLoader.AddScript( 'js/fckeditorcode_gecko_1.js' ) ;
	@Packager.Compactor.RemoveLine */
}

function LoadLanguageFile()
{
	if( window.console ) window.console.log( 'LoadLanguageFile()' ) ;	// @Packager.Compactor.RemoveLine

	FCKScriptLoader.OnEmpty = LoadEditor ;

	if( window.console ) window.console.log( 'Active Language: ' + FCKLanguageManager.ActiveLanguage.Code ) ;	// @Packager.Compactor.RemoveLine

	FCKScriptLoader.AddScript( 'lang/' + FCKLanguageManager.ActiveLanguage.Code + '.js' ) ;
}

function LoadEditor()
{
	if( window.console ) window.console.log( 'LoadEditor()' ) ;	// @Packager.Compactor.RemoveLine

	// Removes the OnEmpty listener.
	FCKScriptLoader.OnEmpty = null ;

	// Correct the editor layout to the correct language direction.
	if ( FCKLang )
		window.document.dir = FCKLang.Dir ;

	// Starts the editor.
	FCK.StartEditor() ;
}