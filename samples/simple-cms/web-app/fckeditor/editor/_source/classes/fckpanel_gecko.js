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
 * File Name: fckpanel_gecko.js
 * 	FCKPanel Class: Creates and manages floating panels in Gecko Browsers.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKPanel = function( parentWindow )
{
	this.IsRTL			= false ;
	this.IsContextMenu	= false ;
	this._IsOpened		= false ;
	
	if ( parentWindow )
		this._Window = parentWindow ;
	else
	{
		this._Window = window ;

		while ( this._Window != window.top )
		{
			// Try/Catch must be used to avoit an error when using a frameset
			// on a different domain:
			// "Permission denied to get property HTMLDocument.Body".
			try
			{
				if ( this._Window.parent.document.body.tagName == 'FRAMESET' )
					break ;
			} catch (e) { break ; }

			this._Window = this._Window.parent ;
		}
	}
	
	var oIFrame = this._IFrame = this._Window.document.createElement('iframe') ; 
	oIFrame.frameBorder			= '0';
	oIFrame.scrolling			= 'no' ;
	oIFrame.style.position		= 'absolute';
	oIFrame.width = oIFrame.height = 0 ;
	oIFrame.style.zIndex		= FCKConfig.FloatingPanelsZIndex ;

	this._Window.document.body.appendChild( oIFrame ) ;
	
	this.Document = oIFrame.contentWindow.document ;

	// Initialize the IFRAME document body.
	this.Document.open() ;
	this.Document.write( '<html><head></head><body><\/body><\/html>' ) ;
	this.Document.close() ;

	// Remove the default margins.
	this.Document.body.style.margin = this.Document.body.style.padding = '0px' ;

	this._IFrame.contentWindow.onblur = this.Hide ;
	
	oIFrame.contentWindow.Panel = this ;


	// Create the main DIV that is used as the panel base.
	this.PanelDiv = this.Document.body.appendChild( this.Document.createElement('DIV') ) ;
	this.PanelDiv.className = 'FCK_Panel' ;
	
	this.EnableContextMenu( false ) ;
	this.SetDirection( FCKLang.Dir ) ;
}

FCKPanel.prototype.EnableContextMenu = function( enabled )
{
	this.Document.oncontextmenu = enabled ? null : FCKTools.CancelEvent ;
}

FCKPanel.prototype.AppendStyleSheet = function( styleSheet )
{
	FCKTools.AppendStyleSheet( this.Document, styleSheet ) ;
}

FCKPanel.prototype.SetDirection = function( dir )
{
	this.IsRTL = ( dir == 'rtl' ) ;
	this.Document.dir = dir ;

	// The "float" property must be set so Firefox calculates the size correcly.
	this.PanelDiv.style.cssFloat = ( dir == 'rtl' ? 'right' : 'left' ) ;
}

FCKPanel.prototype.Load = function()
{
	// This is a IE only need.
}

FCKPanel.prototype.Show = function( x, y, relElement, width, height )
{
	this.PanelDiv.style.width	= width ? width + 'px' : '' ;
	this.PanelDiv.style.height	= height ? height + 'px' : '' ;

	if ( !width )	this._IFrame.width	= 1 ;
	if ( !height )	this._IFrame.height	= 1 ;

	var oPos = FCKTools.GetElementPosition( relElement, this._Window ) ;

	x += oPos.X ;
	y += oPos.Y ;

	if ( this.IsRTL )
	{
		if ( this.IsContextMenu )
			x  = x - this.PanelDiv.offsetWidth + 1 ;
		else if ( relElement )
			x  = x + ( relElement.offsetWidth - this.PanelDiv.offsetWidth ) ;
	}
	else
	{
		if ( ( x + this.PanelDiv.offsetWidth ) > this._Window.document.body.clientWidth )
			x -= x + this.PanelDiv.offsetWidth - this._Window.document.body.clientWidth ;
	}
	
	if ( x < 0 )
			x = 0 ;

	// Set the context menu DIV in the specified location.
	this._IFrame.style.left	= x + 'px' ;
	this._IFrame.style.top	= y + 'px' ;
	
	var iWidth	= this.PanelDiv.offsetWidth ;
	var iHeight	= this.PanelDiv.offsetHeight ;
	
	this._IFrame.width	= iWidth ;
	this._IFrame.height = iHeight ;

	// Move the focus to the IFRAME so we catch the "onblur".
	this._IFrame.contentWindow.focus() ;

	this._IsOpened = true ;
}

FCKPanel.prototype.Hide = function()
{
	var oPanel = this.Panel ? this.Panel : this ;
	
	if ( !oPanel._IsOpened )
		return ;
	
	// It is better to set the sizes to 0, otherwise Firefox would have 
	// rendering problems.
	oPanel._IFrame.width = oPanel._IFrame.height = 0 ;
	
	if ( oPanel._OnHide )
		oPanel._OnHide( oPanel ) ;

	oPanel._IsOpened = false ;
}

FCKPanel.prototype.CheckIsOpened = function()
{
	return this._IsOpened ;
}

FCKPanel.prototype.AttachToOnHideEvent = function( targetFunction )
{
	this._OnHide = targetFunction ;
}