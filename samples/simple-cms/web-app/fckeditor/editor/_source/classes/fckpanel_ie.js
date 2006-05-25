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
 * File Name: fckpanel_ie.js
 * 	FCKPanel Class: Creates and manages floating panels in IE Browsers.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKPanel = function( parentWindow )
{
	this.IsRTL			= false ;
	this.IsContextMenu	= false ;
	this._IsOpened		= false ;
	
	this._Window = parentWindow ? parentWindow : window ;
	
	// Create the Popup that will hold the panel.
	this._Popup	= this._Window.createPopup() ;
	this.Document	= this._Popup.document ;


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
}

FCKPanel.prototype.Load = function( x, y, relElement )
{
	// The offsetWidth and offsetHeight properties are not available if the 
	// element is not visible. So we must "show" the popup with no size to
	// be able to use that values in the second call.
	this._Popup.show( x, y, 0, 0, relElement ) ;
}

FCKPanel.prototype.Show = function( x, y, relElement, width, height )
{
	this.Load( x, y, relElement ) ;

	// The following lines must be place after the above "show", otherwise it 
	// doesn't has the desired effect.
	this.PanelDiv.style.width	= width ? width + 'px' : '' ;
	this.PanelDiv.style.height	= height ? height + 'px' : '' ;

	if ( this.IsRTL )
	{
		if ( this.IsContextMenu )
			x  = x - this.PanelDiv.offsetWidth + 1 ;
		else if ( relElement )
			x  = x + ( relElement.offsetWidth - this.PanelDiv.offsetWidth ) ;
	}

	// Second call: Show the Popup at the specified location, with the correct size.
	this._Popup.show( x, y, this.PanelDiv.offsetWidth, this.PanelDiv.offsetHeight, relElement ) ;
	
	if ( this._OnHide )
	{
		if ( FCKPanel_ActivePopupInfo.Timer )
			CheckPopupOnHide() ;
		FCKPanel_ActivePopupInfo.Timer = window.setInterval( CheckPopupOnHide, 200 ) ;
		FCKPanel_ActivePopupInfo.Panel = this ;
	}

	this._IsOpened = true ;
}

FCKPanel.prototype.Hide = function()
{
	this._Popup.hide() ;
}

FCKPanel.prototype.CheckIsOpened = function()
{
	return this._Popup.isOpen ;
}

FCKPanel.prototype.AttachToOnHideEvent = function( targetFunction )
{
	this._OnHide = targetFunction ;
}

var FCKPanel_ActivePopupInfo = new Object() ;

function CheckPopupOnHide()
{
	var oPanel = FCKPanel_ActivePopupInfo.Panel ;
	
	if ( oPanel && !oPanel._Popup.isOpen )
	{
		window.clearInterval( FCKPanel_ActivePopupInfo.Timer ) ;
		
		if ( oPanel._OnHide )
			oPanel._OnHide( oPanel ) ;
		
		FCKPanel_ActivePopupInfo.Timer = null ;
		FCKPanel_ActivePopupInfo.Panel = null ;
	}
}