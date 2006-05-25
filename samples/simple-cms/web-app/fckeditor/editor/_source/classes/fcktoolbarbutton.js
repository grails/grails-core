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
 * File Name: fcktoolbarbutton.js
 * 	FCKToolbarButton Class: represents a button in the toolbar.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKToolbarButton = function( commandName, label, tooltip, style, sourceView, contextSensitive )
{
	this.Command			= FCKCommands.GetCommand( commandName ) ;
	this.Label				= label ? label : commandName ;
	this.Tooltip			= tooltip ? tooltip : ( label ? label : commandName) ;
	this.Style				= style ? style : FCK_TOOLBARITEM_ONLYICON ;
	this.SourceView			= sourceView ? true : false ;
	this.ContextSensitive	= contextSensitive ? true : false ;
	this.IconPath			= FCKConfig.SkinPath + 'toolbar/' + commandName.toLowerCase() + '.gif' ;
	this.State				= FCK_UNKNOWN ;
}

FCKToolbarButton.prototype.CreateInstance = function( parentToolbar )
{
	this.DOMDiv = document.createElement( 'div' ) ;
	this.DOMDiv.className = 'TB_Button_Off' ;

	this.DOMDiv.FCKToolbarButton = this ;
	
	var sHtml =
		'<table title="' + this.Tooltip + '" cellspacing="0" cellpadding="0" border="0">' +
			'<tr>' ;
	
	if ( this.Style != FCK_TOOLBARITEM_ONLYTEXT ) 
		sHtml += '<td class="TB_Icon"><img src="' + this.IconPath + '" width="21" height="21"></td>' ;
	
	if ( this.Style != FCK_TOOLBARITEM_ONLYICON ) 
		sHtml += '<td class="TB_Text" nowrap>' + this.Label + '</td>' ;
	
	sHtml +=	
			'</tr>' +
		'</table>' ;
	
	this.DOMDiv.innerHTML = sHtml ;

	var oCell = parentToolbar.DOMRow.insertCell(-1) ;
	oCell.appendChild( this.DOMDiv ) ;
	
	this.RefreshState() ;
}

FCKToolbarButton.prototype.RefreshState = function()
{
/*
	TODO: Delete this comment block on stable version.
	// Gets the actual state.
//	var eState ;

//	if ( FCK.EditMode == FCK_EDITMODE_SOURCE && ! this.SourceView )
//		eState = FCK_TRISTATE_DISABLED ;
//	else
*/
	// Gets the actual state.
	var eState = this.Command.GetState() ;
	
	// If there are no state changes than do nothing and return.
	if ( eState == this.State ) return ;
	
	// Sets the actual state.
	this.State = eState ;
	
	switch ( this.State )
	{
		case FCK_TRISTATE_ON :
			this.DOMDiv.className = 'TB_Button_On' ;

			this.DOMDiv.onmouseover	= FCKToolbarButton_OnMouseOnOver ;
			this.DOMDiv.onmouseout	= FCKToolbarButton_OnMouseOnOut ;
			this.DOMDiv.onclick		= FCKToolbarButton_OnClick ;
			
			break ;
		case FCK_TRISTATE_OFF :
			this.DOMDiv.className = 'TB_Button_Off' ;

			this.DOMDiv.onmouseover	= FCKToolbarButton_OnMouseOffOver ;
			this.DOMDiv.onmouseout	= FCKToolbarButton_OnMouseOffOut ;
			this.DOMDiv.onclick		= FCKToolbarButton_OnClick ;
			
			break ;
		default :
			this.Disable() ;
			break ;
	}
}

function FCKToolbarButton_OnMouseOnOver()
{
	this.className = 'TB_Button_On TB_Button_On_Over' ;
}

function FCKToolbarButton_OnMouseOnOut()
{
	this.className = 'TB_Button_On' ;
}
	
function FCKToolbarButton_OnMouseOffOver()
{
	this.className = 'TB_Button_On TB_Button_Off_Over' ;
}

function FCKToolbarButton_OnMouseOffOut()
{
	this.className = 'TB_Button_Off' ;
}
	
function FCKToolbarButton_OnClick(e)
{
	this.FCKToolbarButton.Click(e) ;
	return false ;
}

FCKToolbarButton.prototype.Click = function()
{
	this.Command.Execute() ;
}

FCKToolbarButton.prototype.Enable = function()
{
	this.RefreshState() ;
}

FCKToolbarButton.prototype.Disable = function()
{
	this.State = FCK_TRISTATE_DISABLED ;
	this.DOMDiv.className = 'TB_Button_Disabled' ;
	this.DOMDiv.onmouseover	= null ;
	this.DOMDiv.onmouseout	= null ;
	this.DOMDiv.onclick		= null ;
}