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
 * File Name: fckplugin.js
 * 
 * File Authors:
 * 		Graeme Rocher
 */

var GCMSPreviewCommand = function()
{
	this.Name = 'Preview' ;
}

GCMSPreviewCommand.prototype.Execute = function()
{
	if(window.parent.handleEditorPreview) {
		window.parent.handleEditorPreview();
	}
	else {
		FCK.Preview() ;
	}
}

GCMSPreviewCommand.prototype.GetState = function()
{
	return FCK_TRISTATE_OFF ;
}

FCKCommands.RegisterCommand( 'Preview', new GCMSPreviewCommand() )
