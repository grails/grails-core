/* Copyright (c) 2006 Yahoo! Inc. All rights reserved. */

/**
 * @class a general logging class.  Expects to be initalized with a reference
 * to a html element to write to.
 *
 * @constructor
 * @param {String} sModuleName the name of the module this instance belongs 
 * to.  Used in the log message to help id where the msg came from.
 */
function ygLogger(sModuleName) {
	if (this.setModuleName)
	this.setModuleName(sModuleName);
}

ygLogger.DEBUG_ENABLED = true;
ygLogger.targetEl = null;
ygLogger.logStack = [];
ygLogger.startLog = new Date().getTime();
ygLogger.lastLog = new Date().getTime();
ygLogger.locked = true;
ygLogger.logTimeout = null;

ygLogger.init = function(oHostElement) {
	if (oHostElement) {
		ygLogger.targetEl = oHostElement;
	} else {
		// create element or create window?
	}
};

ygLogger.prototype.setModuleName = function(sModuleName) {
	this.logName = sModuleName;
};

ygLogger.prototype.debug = function() {
	if (ygLogger.DEBUG_ENABLED) {
		var newDate = new Date();
		var newTime = newDate.getTime();
		var timeStamp = newTime - ygLogger.lastLog;
		var totalSeconds = (newTime - ygLogger.startLog) / 1000;

		ygLogger.lastLog = newTime;

		for (var i = 0; i < arguments.length; i++) {
			ygLogger.logStack[ygLogger.logStack.length] = 
					timeStamp + " ms(" + totalSeconds + ") " + 
					newDate.toLocaleTimeString() + "<br />" + 
					this.logName + ": <b>" + arguments[i] + "</b>";
		}

		if (ygLogger.logTimeout == null) {
			ygLogger.logTimeout = setTimeout("ygLogger._outputMessages()" , 1);
		}

	}
};

ygLogger.disable = function() {
	ygLogger.DEBUG_ENABLED = false;
	try { ygLogger.targetEl.style.visibility = "hidden"; } catch(e) {}

};

ygLogger.enable = function() {
	ygLogger.DEBUG_ENABLED = true;
	try { ygLogger.targetEl.style.visibility = ""; } catch(e) {}
};

ygLogger._outputMessages = function() {
	if (ygLogger.targetEl != null) {

		for (var i = 0; i < ygLogger.logStack.length; i++) {
			var sMsg = ygLogger.logStack[i];
			var oNewElement = ygLogger.targetEl.appendChild(
					document.createElement("p"));
			oNewElement.innerHTML = sMsg;
		}
		
		ygLogger.logStack = [];

		ygLogger.targetEl.scrollTop = ygLogger.targetEl.scrollHeight;

		// debugger;
	}

	ygLogger.logTimeout = null;
};

