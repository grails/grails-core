/*
Copyright (c) 2006 Yahoo! Inc. All rights reserved.
version 0.9.0
*/

/**
 * The Connection Manager provides a simplified interface to the XMLHttpRequest
 * object.  It handles cross-browser instantiantion of XMLHttpRequest, negotiates the
 * interactive states and server response returning the results to a pre-defined
 * callback function you create.
 * @ class
 */
YAHOO.util.Connect = {};

YAHOO.util.Connect =
{
  /**
   * Array of  MSFT ActiveX ids for XMLHttpRequest.
   * @private
   * @type array
   */
	_msxml_progid:[
		'MSXML2.XMLHTTP.5.0',
		'MSXML2.XMLHTTP.4.0',
		'MSXML2.XMLHTTP.3.0',
		'MSXML2.XMLHTTP',
		'Microsoft.XMLHTTP'
		],

  /**
   * Array of HTTP header(s)
   * @private
   * @type array
   */
	_http_header:[],

 /**
  * Property modified by setForm() to determine if the transaction
  * should be processed as a HTML form POST.
  * @private
  * @type boolean
  */
	_isFormPost:false,

 /**
  * Property modified by setForm() to the HTML form POST body.
  * @private
  * @type string
  */
	_sFormData:null,

  /**
   * The polling frequency, in milliseconds, for HandleReadyState.
   * when attempting to determine a transaction's XHR  readyState.
   * The default is 300 milliseconds.
   * @private
   * @type int
   */
	_polling_interval:300,

  /**
   * A transaction counter that increments the transaction id for each transaction.
   * @private
   * @type int
   */
	_transaction_id:0,

  /**
   * Member to add an ActiveX id to the existing xml_progid array.
   * In the event(unlikely) a new ActiveX id is introduced, it can be added
   * without internal code modifications.
   * @public
   * @param string id The ActiveX id to be added to initialize the XHR object.
   * @return void
   */
	setProgId:function(id)
	{
		this.msxml_progid.unshift(id);
	},

  /**
   * Instantiates a XMLHttpRequest object and returns an object with two properties:
   * the XMLHttpRequest instance and the transaction id.
   * @private
   * @param {int} transactionId Property containing the transaction id for this transaction.
   * @return connection object
   * @type object
   */
	createXhrObject:function(transactionId)
	{
		var obj,http;
		try
		{
			// Instantiates XMLHttpRequest in non-IE browsers and assigns to http.
			http = new XMLHttpRequest();
			//  Object literal with http and id properties
			obj = { conn:http, tId:transactionId };
		}
		catch(e)
		{
			for(var i=0; i<this._msxml_progid.length; ++i){
				try
				{
					// Instantiates XMLHttpRequest for IE and assign to http.
					http = new ActiveXObject(this._msxml_progid[i]);
					//  Object literal with http and id properties
					obj = { conn:http, tId:transactionId };
				}
				catch(e){}
			}
		}
		finally
		{
			return obj;
		}
	},

  /**
   * This method is called by asyncRequest and syncRequest to create a
   * valid connection object for the transaction.  It also passes a
   * transaction id and increments the transaction id counter.
   * @private
   * @return object
   */
	getConnectionObject:function()
	{
		var o;
		var tId = this._transaction_id;

		try
		{
			o = this.createXhrObject(tId);
			if(o){
				this._transaction_id++;
			}
		}
		catch(e){}
		finally
		{
			return o;
		}
	},

  /**
   * Method for initiating an asynchronous request via the XHR object.
   * @public
   * @param {string} method HTTP transaction method
   * @param {string} uri Fully qualified path of resource
   * @param callback User-defined callback function or object
   * @param callbackArg User-defined callback arguments
   * @param {string} postData POST body
   * @return {object} Returns the connection object
   */
	asyncRequest:function(method, uri, callback, postData)
	{
		var errorObj;
		var o = this.getConnectionObject();

		if(!o){
			return null;
		}
		else{
			var oConn = this;

			o.conn.open(method, uri, true);
		    this.handleReadyState(o, callback);

			if(this._isFormPost){
				postData = this._sFormData;
				this._isFormPost = false;
			}
			else if(postData){
				this.initHeader('Content-Type','application/x-www-form-urlencoded');
			}

			//Verify whether the transaction has any user-defined HTTP headers
			//and set them.
			if(this._http_header.length>0){
				this.setHeader(o);
			}
			postData?o.conn.send(postData):o.conn.send(null);

			return o;
		}
	},

  /**
   * This method serves as a timer that polls the XHR object's readyState
   * property during a transaction, instead of binding a callback to the
   * onreadystatechange event.  Upon readyState 4, handleTransactionResponse
   * will process the response, and the timer will be cleared.
   *
   * @private
   * @param {object} o The connection object
   * @param callback User-defined callback object
   * @param callbackArg User-defined arguments passed to the callback
   * @return void
   */
	handleReadyState:function(o, callback)
	{
		var oConn = this;
		var poll = window.setInterval(
			function(){
				if(o.conn.readyState==4){
					oConn.handleTransactionResponse(o, callback);
					window.clearInterval(poll);
				}
			}
		,this._polling_interval);
	},

  /**
   * This method attempts to interpret the server response and
   * determine whether the transaction was successful, or if an error or
   * exception was encountered.
   *
   * @private
   * @param {object} o The connection object
   * @param {function} callback - User-defined callback object
   * @param {} callbackArg - User-defined arguments to be passed to the callback
   * @return void
   */
	handleTransactionResponse:function(o, callback)
	{
		var httpStatus;
		var responseObject;

		try{
			httpStatus = o.conn.status;
		}
		catch(e){
			// 13030 is the custom code to indicate the condition -- in Mozilla/FF --
			// when the o object's status and statusText properties are
			// unavailable, and a query attempt throws an exception.
			httpStatus = 13030;
		}

		if(httpStatus == 200){
			responseObject = this.createResponseObject(o, callback.argument);
			if(callback.success){
				if(!callback.scope){
					callback.success(responseObject);
				}
				else{
					callback.success.apply(callback.scope, [responseObject]);
				}
			}
		}
		else{
			switch(httpStatus){
				// The following case labels are wininet.dll error codes that may be encountered.
				// Server timeout
				case 12002:
				// 12029 to 12031 correspond to dropped connections.
				case 12029:
				case 12030:
				case 12031:
				// Connection closed by server.
				case 12152:
				// See above comments for variable status.
				case 13030:
					responseObject = this.createExceptionObject(o, callback.argument);
					if(callback.failure){
						if(!callback.scope){
							callback.failure(responseObject);
						}
						else{
							callback.failure.apply(callback.scope,[responseObject]);
						}
					}
					break;
				default:
					responseObject = this.createResponseObject(o, callback.argument);
					if(callback.failure){
						if(!callback.scope){
							callback.failure(responseObject);
						}
						else{
							callback.failure.apply(callback.scope,[responseObject]);
						}
					}
			}
		}

		this.releaseObject(o);
	},

  /**
   * This method evaluates the server response, creates and returns the results via
   * its properties.  Success and failure cases(and exceptions) will differ in their defined properties
   * but property "type" will confirm the transaction's status.
   * @private
   * @param {object} o The connection object
   * @param {} callbackArg User-defined arguments to be passed to the callback
   * @param {boolean} isSuccess Indicates whether the transaction was successful or not.
   * @return object
   */
	createResponseObject:function(o, callbackArg)
	{
		var obj = {};

		obj.tId = o.tId;
		obj.status = o.conn.status;
		obj.statusText = o.conn.statusText;
		obj.allResponseHeaders = o.conn.getAllResponseHeaders();
		obj.responseText = o.conn.responseText;
		obj.responseXML = o.conn.responseXML;
		if(callbackArg){
			obj.argument = callbackArg;
		}

		return obj;
	},

  /**
   * If a transaction cannot be completed due to dropped or closed connections,
   * there may be not be enough information to build a full response object.
   * The object's property "type" value will be "failure", and two additional
   * unique, properties are added - errorCode and errorText.
   * @private
   * @param {int} tId Transaction Id
   * @param callbackArg The user-defined arguments
   * @param {string} errorCode Error code associated with the exception.
   * @param {string} errorText Error message describing the exception.
   * @return object
   */
	createExceptionObject:function(tId, callbackArg)
	{
		var COMM_CODE = 0;
		var COMM_ERROR = 'communication failure';

		var obj = {};

		obj.tId = tId;
		obj.status = COMM_CODE;
		obj.statusText = COMM_ERROR;
		if(callbackArg){
			obj.argument = callbackArg;
		}

		return obj;
	},

  /**
   * Accessor that stores the HTTP headers for each transaction.
   * @public
   * @param {string} label The HTTP header label
   * @param {string} value The HTTP header value
   * @return void
   */
	initHeader:function(label,value)
	{
		var oHeader = [label,value];
		this._http_header.push(oHeader);
	},

  /**
   * Accessor that sets the HTTP headers for each transaction.
   * @private
   * @param {object} o The connection object for the transaction.
   * @return void
   */
	setHeader:function(o)
	{
		var oHeader = this._http_header;
		for(var i=0;i<oHeader.length;i++){
			o.conn.setRequestHeader(oHeader[i][0],oHeader[i][1]);
		}
		oHeader.splice(0,oHeader.length);
	},

  /**
   * This method assembles the form label and value pairs and
   * constructs an encoded POST body.  Both syncRequest()
   * and asyncRequest() will automatically initialize the
   * transaction with a HTTP header Content-Type of
   * application/x-www-form-urlencoded.
   * @public
   * @param {string} formName value of form name attribute
   * @return void
   */
	setForm:function(formName)
	{
		this._sFormData = '';
		var oForm = document.forms[formName];
		var oElement, elName, elValue;
		// iterate over the form elements collection to construct the
		// label-value pairs.
		for (var i=0; i<oForm.elements.length; i++){
			oElement = oForm.elements[i];
			elName = oForm.elements[i].name;
			elValue = oForm.elements[i].value;
			switch (oElement.type)
			{
				case 'select-multiple':
					for(var j=0; j<oElement.options.length; j++){
						if(oElement.options[j].selected){
							this._sFormData += encodeURIComponent(elName) + '=' + encodeURIComponent(oElement.options[j].value) + '&';
						}
					}
					break;
				case 'radio':
				case 'checkbox':
					if(oElement.checked){
						this._sFormData += encodeURIComponent(elName) + '=' + encodeURIComponent(elValue) + '&';
					}
					break;
				case 'file':
				// stub case as XMLHttpRequest will only send the file path as a string.
					break;
				case undefined:
				// stub case for fieldset element which returns undefined.
					break;
				default:
					this._sFormData += encodeURIComponent(elName) + '=' + encodeURIComponent(elValue) + '&';
					break;
			}
		}
		this._sFormData = this._sFormData.substr(0, this._sFormData.length - 1);
		this._isFormPost = true;
		this.initHeader('Content-Type','application/x-www-form-urlencoded');
	},

  /**
   * Public method to terminate a transaction, if it has not reached readyState 4.
   * @public
   * @param {object} o The connection object returned by asyncRequest.
   * @return void
   */
	abort:function(o)
	{
		if(this.isCallInProgress(o)){
			o.conn.abort();
			this.releaseObject(o);
		}
	},

  /**
   * Accessor to check if the transaction associated with the connection object
   * is still being processed.
   * @public
   * @param {object} o The connection object returned by asyncRequest
   * @return boolean
   */
	isCallInProgress:function(o)
	{
		if(o){
			return o.conn.readyState != 4 && o.conn.readyState != 0;
		}
	},

  /**
   * Dereference the XHR instance and the connection object after the transaction is completed.
   * @private
   * @param {object} o The connection object
   * @return void
   */
	releaseObject:function(o)
	{
			//dereference the XHR instance.
			o.conn = null;
			//dereference the connection object.
			o = null;
	}
}