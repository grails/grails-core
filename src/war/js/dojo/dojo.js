/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

/*
	This is a compiled version of Dojo, built for deployment and not for
	development. To get an editable version, please visit:

		http://dojotoolkit.org

	for documentation and information on getting the source.
*/

var dj_global=this;
function dj_undef(_1,_2){
if(!_2){
_2=dj_global;
}
return (typeof _2[_1]=="undefined");
}
if(dj_undef("djConfig")){
var djConfig={};
}
if(dj_undef("dojo")){
var dojo={};
}
dojo.version={major:0,minor:2,patch:2,flag:"+",revision:Number("$Rev: 3802 $".match(/[0-9]+/)[0]),toString:function(){
with(dojo.version){
return major+"."+minor+"."+patch+flag+" ("+revision+")";
}
}};
dojo.evalProp=function(_3,_4,_5){
return (_4&&!dj_undef(_3,_4)?_4[_3]:(_5?(_4[_3]={}):undefined));
};
dojo.parseObjPath=function(_6,_7,_8){
var _9=(_7?_7:dj_global);
var _a=_6.split(".");
var _b=_a.pop();
for(var i=0,l=_a.length;i<l&&_9;i++){
_9=dojo.evalProp(_a[i],_9,_8);
}
return {obj:_9,prop:_b};
};
dojo.evalObjPath=function(_d,_e){
if(typeof _d!="string"){
return dj_global;
}
if(_d.indexOf(".")==-1){
return dojo.evalProp(_d,dj_global,_e);
}
with(dojo.parseObjPath(_d,dj_global,_e)){
return dojo.evalProp(prop,obj,_e);
}
};
dojo.errorToString=function(_f){
return ((!dj_undef("message",_f))?_f.message:(dj_undef("description",_f)?_f:_f.description));
};
dojo.raise=function(_10,_11){
if(_11){
_10=_10+": "+dojo.errorToString(_11);
}
var he=dojo.hostenv;
if((!dj_undef("hostenv",dojo))&&(!dj_undef("println",dojo.hostenv))){
dojo.hostenv.println("FATAL: "+_10);
}
throw Error(_10);
};
dojo.debug=function(){
};
dojo.debugShallow=function(obj){
};
dojo.profile={start:function(){
},end:function(){
},stop:function(){
},dump:function(){
}};
function dj_eval(s){
return dj_global.eval?dj_global.eval(s):eval(s);
}
dojo.unimplemented=function(_15,_16){
var _17="'"+_15+"' not implemented";
if((!dj_undef(_16))&&(_16)){
_17+=" "+_16;
}
dojo.raise(_17);
};
dojo.deprecated=function(_18,_19,_1a){
var _1b="DEPRECATED: "+_18;
if(_19){
_1b+=" "+_19;
}
if(_1a){
_1b+=" -- will be removed in version: "+_1a;
}
dojo.debug(_1b);
};
dojo.inherits=function(_1c,_1d){
if(typeof _1d!="function"){
dojo.raise("dojo.inherits: superclass argument ["+_1d+"] must be a function (subclass: ["+_1c+"']");
}
_1c.prototype=new _1d();
_1c.prototype.constructor=_1c;
_1c.superclass=_1d.prototype;
_1c["super"]=_1d.prototype;
};
dojo.render=(function(){
function vscaffold(_1e,_1f){
var tmp={capable:false,support:{builtin:false,plugin:false},prefixes:_1e};
for(var x in _1f){
tmp[x]=false;
}
return tmp;
}
return {name:"",ver:dojo.version,os:{win:false,linux:false,osx:false},html:vscaffold(["html"],["ie","opera","khtml","safari","moz"]),svg:vscaffold(["svg"],["corel","adobe","batik"]),vml:vscaffold(["vml"],["ie"]),swf:vscaffold(["Swf","Flash","Mm"],["mm"]),swt:vscaffold(["Swt"],["ibm"])};
})();
dojo.hostenv=(function(){
var _22={isDebug:false,allowQueryConfig:false,baseScriptUri:"",baseRelativePath:"",libraryScriptUri:"",iePreventClobber:false,ieClobberMinimal:true,preventBackButtonFix:true,searchIds:[],parseWidgets:true};
if(typeof djConfig=="undefined"){
djConfig=_22;
}else{
for(var _23 in _22){
if(typeof djConfig[_23]=="undefined"){
djConfig[_23]=_22[_23];
}
}
}
return {name_:"(unset)",version_:"(unset)",getName:function(){
return this.name_;
},getVersion:function(){
return this.version_;
},getText:function(uri){
dojo.unimplemented("getText","uri="+uri);
}};
})();
dojo.hostenv.getBaseScriptUri=function(){
if(djConfig.baseScriptUri.length){
return djConfig.baseScriptUri;
}
var uri=new String(djConfig.libraryScriptUri||djConfig.baseRelativePath);
if(!uri){
dojo.raise("Nothing returned by getLibraryScriptUri(): "+uri);
}
var _26=uri.lastIndexOf("/");
djConfig.baseScriptUri=djConfig.baseRelativePath;
return djConfig.baseScriptUri;
};
(function(){
var _27={pkgFileName:"__package__",loading_modules_:{},loaded_modules_:{},addedToLoadingCount:[],removedFromLoadingCount:[],inFlightCount:0,modulePrefixes_:{dojo:{name:"dojo",value:"src"}},setModulePrefix:function(_28,_29){
this.modulePrefixes_[_28]={name:_28,value:_29};
},getModulePrefix:function(_2a){
var mp=this.modulePrefixes_;
if((mp[_2a])&&(mp[_2a]["name"])){
return mp[_2a].value;
}
return _2a;
},getTextStack:[],loadUriStack:[],loadedUris:[],post_load_:false,modulesLoadedListeners:[]};
for(var _2c in _27){
dojo.hostenv[_2c]=_27[_2c];
}
})();
dojo.hostenv.loadPath=function(_2d,_2e,cb){
if((_2d.charAt(0)=="/")||(_2d.match(/^\w+:/))){
dojo.raise("relpath '"+_2d+"'; must be relative");
}
var uri=this.getBaseScriptUri()+_2d;
if(djConfig.cacheBust&&dojo.render.html.capable){
uri+="?"+String(djConfig.cacheBust).replace(/\W+/g,"");
}
try{
return ((!_2e)?this.loadUri(uri,cb):this.loadUriAndCheck(uri,_2e,cb));
}
catch(e){
dojo.debug(e);
return false;
}
};
dojo.hostenv.loadUri=function(uri,cb){
if(this.loadedUris[uri]){
return;
}
var _33=this.getText(uri,null,true);
if(_33==null){
return 0;
}
this.loadedUris[uri]=true;
var _34=dj_eval(_33);
return 1;
};
dojo.hostenv.loadUriAndCheck=function(uri,_36,cb){
var ok=true;
try{
ok=this.loadUri(uri,cb);
}
catch(e){
dojo.debug("failed loading ",uri," with error: ",e);
}
return ((ok)&&(this.findModule(_36,false)))?true:false;
};
dojo.loaded=function(){
};
dojo.hostenv.loaded=function(){
this.post_load_=true;
var mll=this.modulesLoadedListeners;
this.modulesLoadedListeners=[];
for(var x=0;x<mll.length;x++){
mll[x]();
}
dojo.loaded();
};
dojo.addOnLoad=function(obj,_3c){
var dh=dojo.hostenv;
if(arguments.length==1){
dh.modulesLoadedListeners.push(obj);
}else{
if(arguments.length>1){
dh.modulesLoadedListeners.push(function(){
obj[_3c]();
});
}
}
if(dh.post_load_&&dh.inFlightCount==0){
dh.callLoaded();
}
};
dojo.hostenv.modulesLoaded=function(){
if(this.post_load_){
return;
}
if((this.loadUriStack.length==0)&&(this.getTextStack.length==0)){
if(this.inFlightCount>0){
dojo.debug("files still in flight!");
return;
}
dojo.hostenv.callLoaded();
}
};
dojo.hostenv.callLoaded=function(){
if(typeof setTimeout=="object"){
setTimeout("dojo.hostenv.loaded();",0);
}else{
dojo.hostenv.loaded();
}
};
dojo.hostenv._global_omit_module_check=false;
dojo.hostenv.loadModule=function(_3e,_3f,_40){
if(!_3e){
return;
}
_40=this._global_omit_module_check||_40;
var _41=this.findModule(_3e,false);
if(_41){
return _41;
}
if(dj_undef(_3e,this.loading_modules_)){
this.addedToLoadingCount.push(_3e);
}
this.loading_modules_[_3e]=1;
var _42=_3e.replace(/\./g,"/")+".js";
var _43=_3e.split(".");
var _44=_3e.split(".");
for(var i=_43.length-1;i>0;i--){
var _46=_43.slice(0,i).join(".");
var _47=this.getModulePrefix(_46);
if(_47!=_46){
_43.splice(0,i,_47);
break;
}
}
var _48=_43[_43.length-1];
if(_48=="*"){
_3e=(_44.slice(0,-1)).join(".");
while(_43.length){
_43.pop();
_43.push(this.pkgFileName);
_42=_43.join("/")+".js";
if(_42.charAt(0)=="/"){
_42=_42.slice(1);
}
ok=this.loadPath(_42,((!_40)?_3e:null));
if(ok){
break;
}
_43.pop();
}
}else{
_42=_43.join("/")+".js";
_3e=_44.join(".");
var ok=this.loadPath(_42,((!_40)?_3e:null));
if((!ok)&&(!_3f)){
_43.pop();
while(_43.length){
_42=_43.join("/")+".js";
ok=this.loadPath(_42,((!_40)?_3e:null));
if(ok){
break;
}
_43.pop();
_42=_43.join("/")+"/"+this.pkgFileName+".js";
if(_42.charAt(0)=="/"){
_42=_42.slice(1);
}
ok=this.loadPath(_42,((!_40)?_3e:null));
if(ok){
break;
}
}
}
if((!ok)&&(!_40)){
dojo.raise("Could not load '"+_3e+"'; last tried '"+_42+"'");
}
}
if(!_40&&!this["isXDomain"]){
_41=this.findModule(_3e,false);
if(!_41){
dojo.raise("symbol '"+_3e+"' is not defined after loading '"+_42+"'");
}
}
return _41;
};
dojo.hostenv.startPackage=function(_4a){
var _4b=dojo.evalObjPath((_4a.split(".").slice(0,-1)).join("."));
this.loaded_modules_[(new String(_4a)).toLowerCase()]=_4b;
var _4c=_4a.split(/\./);
if(_4c[_4c.length-1]=="*"){
_4c.pop();
}
return dojo.evalObjPath(_4c.join("."),true);
};
dojo.hostenv.findModule=function(_4d,_4e){
var lmn=(new String(_4d)).toLowerCase();
if(this.loaded_modules_[lmn]){
return this.loaded_modules_[lmn];
}
var _50=dojo.evalObjPath(_4d);
if((_4d)&&(typeof _50!="undefined")&&(_50)){
this.loaded_modules_[lmn]=_50;
return _50;
}
if(_4e){
dojo.raise("no loaded module named '"+_4d+"'");
}
return null;
};
dojo.kwCompoundRequire=function(_51){
var _52=_51["common"]||[];
var _53=(_51[dojo.hostenv.name_])?_52.concat(_51[dojo.hostenv.name_]||[]):_52.concat(_51["default"]||[]);
for(var x=0;x<_53.length;x++){
var _55=_53[x];
if(_55.constructor==Array){
dojo.hostenv.loadModule.apply(dojo.hostenv,_55);
}else{
dojo.hostenv.loadModule(_55);
}
}
};
dojo.require=function(){
dojo.hostenv.loadModule.apply(dojo.hostenv,arguments);
};
dojo.requireIf=function(){
if((arguments[0]===true)||(arguments[0]=="common")||(arguments[0]&&dojo.render[arguments[0]].capable)){
var _56=[];
for(var i=1;i<arguments.length;i++){
_56.push(arguments[i]);
}
dojo.require.apply(dojo,_56);
}
};
dojo.requireAfterIf=dojo.requireIf;
dojo.provide=function(){
return dojo.hostenv.startPackage.apply(dojo.hostenv,arguments);
};
dojo.setModulePrefix=function(_58,_59){
return dojo.hostenv.setModulePrefix(_58,_59);
};
dojo.exists=function(obj,_5b){
var p=_5b.split(".");
for(var i=0;i<p.length;i++){
if(!(obj[p[i]])){
return false;
}
obj=obj[p[i]];
}
return true;
};
if(typeof window=="undefined"){
dojo.raise("no window object");
}
(function(){
if(djConfig.allowQueryConfig){
var _5e=document.location.toString();
var _5f=_5e.split("?",2);
if(_5f.length>1){
var _60=_5f[1];
var _61=_60.split("&");
for(var x in _61){
var sp=_61[x].split("=");
if((sp[0].length>9)&&(sp[0].substr(0,9)=="djConfig.")){
var opt=sp[0].substr(9);
try{
djConfig[opt]=eval(sp[1]);
}
catch(e){
djConfig[opt]=sp[1];
}
}
}
}
}
if(((djConfig["baseScriptUri"]=="")||(djConfig["baseRelativePath"]==""))&&(document&&document.getElementsByTagName)){
var _65=document.getElementsByTagName("script");
var _66=/(__package__|dojo|bootstrap1)\.js([\?\.]|$)/i;
for(var i=0;i<_65.length;i++){
var src=_65[i].getAttribute("src");
if(!src){
continue;
}
var m=src.match(_66);
if(m){
root=src.substring(0,m.index);
if(src.indexOf("bootstrap1")>-1){
root+="../";
}
if(!this["djConfig"]){
djConfig={};
}
if(djConfig["baseScriptUri"]==""){
djConfig["baseScriptUri"]=root;
}
if(djConfig["baseRelativePath"]==""){
djConfig["baseRelativePath"]=root;
}
break;
}
}
}
var dr=dojo.render;
var drh=dojo.render.html;
var drs=dojo.render.svg;
var dua=drh.UA=navigator.userAgent;
var dav=drh.AV=navigator.appVersion;
var t=true;
var f=false;
drh.capable=t;
drh.support.builtin=t;
dr.ver=parseFloat(drh.AV);
dr.os.mac=dav.indexOf("Macintosh")>=0;
dr.os.win=dav.indexOf("Windows")>=0;
dr.os.linux=dav.indexOf("X11")>=0;
drh.opera=dua.indexOf("Opera")>=0;
drh.khtml=(dav.indexOf("Konqueror")>=0)||(dav.indexOf("Safari")>=0);
drh.safari=dav.indexOf("Safari")>=0;
var _71=dua.indexOf("Gecko");
drh.mozilla=drh.moz=(_71>=0)&&(!drh.khtml);
if(drh.mozilla){
drh.geckoVersion=dua.substring(_71+6,_71+14);
}
drh.ie=(document.all)&&(!drh.opera);
drh.ie50=drh.ie&&dav.indexOf("MSIE 5.0")>=0;
drh.ie55=drh.ie&&dav.indexOf("MSIE 5.5")>=0;
drh.ie60=drh.ie&&dav.indexOf("MSIE 6.0")>=0;
dr.vml.capable=drh.ie;
drs.capable=f;
drs.support.plugin=f;
drs.support.builtin=f;
if(document.implementation&&document.implementation.hasFeature&&document.implementation.hasFeature("org.w3c.dom.svg","1.0")){
drs.capable=t;
drs.support.builtin=t;
drs.support.plugin=f;
}
})();
dojo.hostenv.startPackage("dojo.hostenv");
dojo.render.name=dojo.hostenv.name_="browser";
dojo.hostenv.searchIds=[];
var DJ_XMLHTTP_PROGIDS=["Msxml2.XMLHTTP","Microsoft.XMLHTTP","Msxml2.XMLHTTP.4.0"];
dojo.hostenv.getXmlhttpObject=function(){
var _72=null;
var _73=null;
try{
_72=new XMLHttpRequest();
}
catch(e){
}
if(!_72){
for(var i=0;i<3;++i){
var _75=DJ_XMLHTTP_PROGIDS[i];
try{
_72=new ActiveXObject(_75);
}
catch(e){
_73=e;
}
if(_72){
DJ_XMLHTTP_PROGIDS=[_75];
break;
}
}
}
if(!_72){
return dojo.raise("XMLHTTP not available",_73);
}
return _72;
};
dojo.hostenv.getText=function(uri,_77,_78){
var _79=this.getXmlhttpObject();
if(_77){
_79.onreadystatechange=function(){
if((4==_79.readyState)&&(_79["status"])){
if(_79.status==200){
_77(_79.responseText);
}
}
};
}
_79.open("GET",uri,_77?true:false);
try{
_79.send(null);
}
catch(e){
if(_78&&!_77){
return null;
}else{
throw e;
}
}
if(_77){
return null;
}
return _79.responseText;
};
dojo.hostenv.defaultDebugContainerId="dojoDebug";
dojo.hostenv._println_buffer=[];
dojo.hostenv._println_safe=false;
dojo.hostenv.println=function(_7a){
if(!dojo.hostenv._println_safe){
dojo.hostenv._println_buffer.push(_7a);
}else{
try{
var _7b=document.getElementById(djConfig.debugContainerId?djConfig.debugContainerId:dojo.hostenv.defaultDebugContainerId);
if(!_7b){
_7b=document.getElementsByTagName("body")[0]||document.body;
}
var div=document.createElement("div");
div.appendChild(document.createTextNode(_7a));
_7b.appendChild(div);
}
catch(e){
try{
document.write("<div>"+_7a+"</div>");
}
catch(e2){
window.status=_7a;
}
}
}
};
dojo.addOnLoad(function(){
dojo.hostenv._println_safe=true;
while(dojo.hostenv._println_buffer.length>0){
dojo.hostenv.println(dojo.hostenv._println_buffer.shift());
}
});
function dj_addNodeEvtHdlr(_7d,_7e,fp,_80){
var _81=_7d["on"+_7e]||function(){
};
_7d["on"+_7e]=function(){
fp.apply(_7d,arguments);
_81.apply(_7d,arguments);
};
return true;
}
dj_addNodeEvtHdlr(window,"load",function(){
if(arguments.callee.initialized){
return;
}
arguments.callee.initialized=true;
var _82=function(){
if(dojo.render.html.ie){
dojo.hostenv.makeWidgets();
}
};
if(dojo.hostenv.inFlightCount==0){
_82();
dojo.hostenv.modulesLoaded();
}else{
dojo.addOnLoad(_82);
}
});
dojo.hostenv.makeWidgets=function(){
var _83=[];
if(djConfig.searchIds&&djConfig.searchIds.length>0){
_83=_83.concat(djConfig.searchIds);
}
if(dojo.hostenv.searchIds&&dojo.hostenv.searchIds.length>0){
_83=_83.concat(dojo.hostenv.searchIds);
}
if((djConfig.parseWidgets)||(_83.length>0)){
if(dojo.evalObjPath("dojo.widget.Parse")){
try{
var _84=new dojo.xml.Parse();
if(_83.length>0){
for(var x=0;x<_83.length;x++){
var _86=document.getElementById(_83[x]);
if(!_86){
continue;
}
var _87=_84.parseElement(_86,null,true);
dojo.widget.getParser().createComponents(_87);
}
}else{
if(djConfig.parseWidgets){
var _87=_84.parseElement(document.getElementsByTagName("body")[0]||document.body,null,true);
dojo.widget.getParser().createComponents(_87);
}
}
}
catch(e){
dojo.debug("auto-build-widgets error:",e);
}
}
}
};
dojo.addOnLoad(function(){
if(!dojo.render.html.ie){
dojo.hostenv.makeWidgets();
}
});
try{
if(dojo.render.html.ie){
document.namespaces.add("v","urn:schemas-microsoft-com:vml");
document.createStyleSheet().addRule("v\\:*","behavior:url(#default#VML)");
}
}
catch(e){
}
dojo.hostenv.writeIncludes=function(){
};
dojo.byId=function(id,doc){
if(id&&(typeof id=="string"||id instanceof String)){
if(!doc){
doc=document;
}
return doc.getElementById(id);
}
return id;
};
(function(){
if(typeof dj_usingBootstrap!="undefined"){
return;
}
var _8a=false;
var _8b=false;
var _8c=false;
if((typeof this["load"]=="function")&&(typeof this["Packages"]=="function")){
_8a=true;
}else{
if(typeof this["load"]=="function"){
_8b=true;
}else{
if(window.widget){
_8c=true;
}
}
}
var _8d=[];
if((this["djConfig"])&&((djConfig["isDebug"])||(djConfig["debugAtAllCosts"]))){
_8d.push("debug.js");
}
if((this["djConfig"])&&(djConfig["debugAtAllCosts"])&&(!_8a)&&(!_8c)){
_8d.push("browser_debug.js");
}
if((this["djConfig"])&&(djConfig["compat"])){
_8d.push("compat/"+djConfig["compat"]+".js");
}
var _8e=djConfig["baseScriptUri"];
if((this["djConfig"])&&(djConfig["baseLoaderUri"])){
_8e=djConfig["baseLoaderUri"];
}
for(var x=0;x<_8d.length;x++){
var _90=_8e+"src/"+_8d[x];
if(_8a||_8b){
load(_90);
}else{
try{
document.write("<scr"+"ipt type='text/javascript' src='"+_90+"'></scr"+"ipt>");
}
catch(e){
var _91=document.createElement("script");
_91.src=_90;
document.getElementsByTagName("head")[0].appendChild(_91);
}
}
}
})();
dojo.provide("dojo.lang.common");
dojo.require("dojo.lang");
dojo.lang.mixin=function(obj,_93){
var _94={};
for(var x in _93){
if(typeof _94[x]=="undefined"||_94[x]!=_93[x]){
obj[x]=_93[x];
}
}
if(dojo.render.html.ie&&dojo.lang.isFunction(_93["toString"])&&_93["toString"]!=obj["toString"]){
obj.toString=_93.toString;
}
return obj;
};
dojo.lang.extend=function(_96,_97){
this.mixin(_96.prototype,_97);
};
dojo.lang.find=function(arr,val,_9a,_9b){
if(!dojo.lang.isArrayLike(arr)&&dojo.lang.isArrayLike(val)){
var a=arr;
arr=val;
val=a;
}
var _9d=dojo.lang.isString(arr);
if(_9d){
arr=arr.split("");
}
if(_9b){
var _9e=-1;
var i=arr.length-1;
var end=-1;
}else{
var _9e=1;
var i=0;
var end=arr.length;
}
if(_9a){
while(i!=end){
if(arr[i]===val){
return i;
}
i+=_9e;
}
}else{
while(i!=end){
if(arr[i]==val){
return i;
}
i+=_9e;
}
}
return -1;
};
dojo.lang.indexOf=dojo.lang.find;
dojo.lang.findLast=function(arr,val,_a3){
return dojo.lang.find(arr,val,_a3,true);
};
dojo.lang.lastIndexOf=dojo.lang.findLast;
dojo.lang.inArray=function(arr,val){
return dojo.lang.find(arr,val)>-1;
};
dojo.lang.isObject=function(wh){
return typeof wh=="object"||dojo.lang.isArray(wh)||dojo.lang.isFunction(wh);
};
dojo.lang.isArray=function(wh){
return (wh instanceof Array||typeof wh=="array");
};
dojo.lang.isArrayLike=function(wh){
if(dojo.lang.isString(wh)){
return false;
}
if(dojo.lang.isFunction(wh)){
return false;
}
if(dojo.lang.isArray(wh)){
return true;
}
if(typeof wh!="undefined"&&wh&&dojo.lang.isNumber(wh.length)&&isFinite(wh.length)){
return true;
}
return false;
};
dojo.lang.isFunction=function(wh){
return (wh instanceof Function||typeof wh=="function");
};
dojo.lang.isString=function(wh){
return (wh instanceof String||typeof wh=="string");
};
dojo.lang.isAlien=function(wh){
return !dojo.lang.isFunction()&&/\{\s*\[native code\]\s*\}/.test(String(wh));
};
dojo.lang.isBoolean=function(wh){
return (wh instanceof Boolean||typeof wh=="boolean");
};
dojo.lang.isNumber=function(wh){
return (wh instanceof Number||typeof wh=="number");
};
dojo.lang.isUndefined=function(wh){
return ((wh==undefined)&&(typeof wh=="undefined"));
};
dojo.provide("dojo.lang.array");
dojo.require("dojo.lang.common");
dojo.lang.has=function(obj,_b0){
try{
return (typeof obj[_b0]!="undefined");
}
catch(e){
return false;
}
};
dojo.lang.isEmpty=function(obj){
if(dojo.lang.isObject(obj)){
var tmp={};
var _b3=0;
for(var x in obj){
if(obj[x]&&(!tmp[x])){
_b3++;
break;
}
}
return (_b3==0);
}else{
if(dojo.lang.isArrayLike(obj)||dojo.lang.isString(obj)){
return obj.length==0;
}
}
};
dojo.lang.map=function(arr,obj,_b7){
var _b8=dojo.lang.isString(arr);
if(_b8){
arr=arr.split("");
}
if(dojo.lang.isFunction(obj)&&(!_b7)){
_b7=obj;
obj=dj_global;
}else{
if(dojo.lang.isFunction(obj)&&_b7){
var _b9=obj;
obj=_b7;
_b7=_b9;
}
}
if(Array.map){
var _ba=Array.map(arr,_b7,obj);
}else{
var _ba=[];
for(var i=0;i<arr.length;++i){
_ba.push(_b7.call(obj,arr[i]));
}
}
if(_b8){
return _ba.join("");
}else{
return _ba;
}
};
dojo.lang.forEach=function(_bc,_bd,_be){
if(dojo.lang.isString(_bc)){
_bc=_bc.split("");
}
if(Array.forEach){
Array.forEach(_bc,_bd,_be);
}else{
if(!_be){
_be=dj_global;
}
for(var i=0,l=_bc.length;i<l;i++){
_bd.call(_be,_bc[i],i,_bc);
}
}
};
dojo.lang._everyOrSome=function(_c0,arr,_c2,_c3){
if(dojo.lang.isString(arr)){
arr=arr.split("");
}
if(Array.every){
return Array[(_c0)?"every":"some"](arr,_c2,_c3);
}else{
if(!_c3){
_c3=dj_global;
}
for(var i=0,l=arr.length;i<l;i++){
var _c5=_c2.call(_c3,arr[i],i,arr);
if((_c0)&&(!_c5)){
return false;
}else{
if((!_c0)&&(_c5)){
return true;
}
}
}
return (_c0)?true:false;
}
};
dojo.lang.every=function(arr,_c7,_c8){
return this._everyOrSome(true,arr,_c7,_c8);
};
dojo.lang.some=function(arr,_ca,_cb){
return this._everyOrSome(false,arr,_ca,_cb);
};
dojo.lang.filter=function(arr,_cd,_ce){
var _cf=dojo.lang.isString(arr);
if(_cf){
arr=arr.split("");
}
if(Array.filter){
var _d0=Array.filter(arr,_cd,_ce);
}else{
if(!_ce){
if(arguments.length>=3){
dojo.raise("thisObject doesn't exist!");
}
_ce=dj_global;
}
var _d0=[];
for(var i=0;i<arr.length;i++){
if(_cd.call(_ce,arr[i],i,arr)){
_d0.push(arr[i]);
}
}
}
if(_cf){
return _d0.join("");
}else{
return _d0;
}
};
dojo.lang.unnest=function(){
var out=[];
for(var i=0;i<arguments.length;i++){
if(dojo.lang.isArrayLike(arguments[i])){
var add=dojo.lang.unnest.apply(this,arguments[i]);
out=out.concat(add);
}else{
out.push(arguments[i]);
}
}
return out;
};
dojo.lang.toArray=function(_d5,_d6){
var _d7=[];
for(var i=_d6||0;i<_d5.length;i++){
_d7.push(_d5[i]);
}
return _d7;
};
dojo.provide("dojo.lang.func");
dojo.require("dojo.lang.common");
dojo.lang.hitch=function(_d9,_da){
if(dojo.lang.isString(_da)){
var fcn=_d9[_da];
}else{
var fcn=_da;
}
return function(){
return fcn.apply(_d9,arguments);
};
};
dojo.lang.anonCtr=0;
dojo.lang.anon={};
dojo.lang.nameAnonFunc=function(_dc,_dd){
var nso=(_dd||dojo.lang.anon);
if((dj_global["djConfig"])&&(djConfig["slowAnonFuncLookups"]==true)){
for(var x in nso){
if(nso[x]===_dc){
return x;
}
}
}
var ret="__"+dojo.lang.anonCtr++;
while(typeof nso[ret]!="undefined"){
ret="__"+dojo.lang.anonCtr++;
}
nso[ret]=_dc;
return ret;
};
dojo.lang.forward=function(_e1){
return function(){
return this[_e1].apply(this,arguments);
};
};
dojo.lang.curry=function(ns,_e3){
var _e4=[];
ns=ns||dj_global;
if(dojo.lang.isString(_e3)){
_e3=ns[_e3];
}
for(var x=2;x<arguments.length;x++){
_e4.push(arguments[x]);
}
var _e6=(_e3["__preJoinArity"]||_e3.length)-_e4.length;
function gather(_e7,_e8,_e9){
var _ea=_e9;
var _eb=_e8.slice(0);
for(var x=0;x<_e7.length;x++){
_eb.push(_e7[x]);
}
_e9=_e9-_e7.length;
if(_e9<=0){
var res=_e3.apply(ns,_eb);
_e9=_ea;
return res;
}else{
return function(){
return gather(arguments,_eb,_e9);
};
}
}
return gather([],_e4,_e6);
};
dojo.lang.curryArguments=function(ns,_ef,_f0,_f1){
var _f2=[];
var x=_f1||0;
for(x=_f1;x<_f0.length;x++){
_f2.push(_f0[x]);
}
return dojo.lang.curry.apply(dojo.lang,[ns,_ef].concat(_f2));
};
dojo.lang.tryThese=function(){
for(var x=0;x<arguments.length;x++){
try{
if(typeof arguments[x]=="function"){
var ret=(arguments[x]());
if(ret){
return ret;
}
}
}
catch(e){
dojo.debug(e);
}
}
};
dojo.lang.delayThese=function(_f6,cb,_f8,_f9){
if(!_f6.length){
if(typeof _f9=="function"){
_f9();
}
return;
}
if((typeof _f8=="undefined")&&(typeof cb=="number")){
_f8=cb;
cb=function(){
};
}else{
if(!cb){
cb=function(){
};
if(!_f8){
_f8=0;
}
}
}
setTimeout(function(){
(_f6.shift())();
cb();
dojo.lang.delayThese(_f6,cb,_f8,_f9);
},_f8);
};
dojo.provide("dojo.string.common");
dojo.require("dojo.string");
dojo.string.trim=function(str,wh){
if(!str.replace){
return str;
}
if(!str.length){
return str;
}
var re=(wh>0)?(/^\s+/):(wh<0)?(/\s+$/):(/^\s+|\s+$/g);
return str.replace(re,"");
};
dojo.string.trimStart=function(str){
return dojo.string.trim(str,1);
};
dojo.string.trimEnd=function(str){
return dojo.string.trim(str,-1);
};
dojo.string.repeat=function(str,_100,_101){
var out="";
for(var i=0;i<_100;i++){
out+=str;
if(_101&&i<_100-1){
out+=_101;
}
}
return out;
};
dojo.string.pad=function(str,len,c,dir){
var out=String(str);
if(!c){
c="0";
}
if(!dir){
dir=1;
}
while(out.length<len){
if(dir>0){
out=c+out;
}else{
out+=c;
}
}
return out;
};
dojo.string.padLeft=function(str,len,c){
return dojo.string.pad(str,len,c,1);
};
dojo.string.padRight=function(str,len,c){
return dojo.string.pad(str,len,c,-1);
};
dojo.provide("dojo.string.extras");
dojo.require("dojo.string.common");
dojo.require("dojo.lang");
dojo.string.paramString=function(str,_110,_111){
for(var name in _110){
var re=new RegExp("\\%\\{"+name+"\\}","g");
str=str.replace(re,_110[name]);
}
if(_111){
str=str.replace(/%\{([^\}\s]+)\}/g,"");
}
return str;
};
dojo.string.capitalize=function(str){
if(!dojo.lang.isString(str)){
return "";
}
if(arguments.length==0){
str=this;
}
var _115=str.split(" ");
var _116="";
var len=_115.length;
for(var i=0;i<len;i++){
var word=_115[i];
word=word.charAt(0).toUpperCase()+word.substring(1,word.length);
_116+=word;
if(i<len-1){
_116+=" ";
}
}
return new String(_116);
};
dojo.string.isBlank=function(str){
if(!dojo.lang.isString(str)){
return true;
}
return (dojo.string.trim(str).length==0);
};
dojo.string.encodeAscii=function(str){
if(!dojo.lang.isString(str)){
return str;
}
var ret="";
var _11d=escape(str);
var _11e,re=/%u([0-9A-F]{4})/i;
while((_11e=_11d.match(re))){
var num=Number("0x"+_11e[1]);
var _120=escape("&#"+num+";");
ret+=_11d.substring(0,_11e.index)+_120;
_11d=_11d.substring(_11e.index+_11e[0].length);
}
ret+=_11d.replace(/\+/g,"%2B");
return ret;
};
dojo.string.escape=function(type,str){
var args=[];
for(var i=1;i<arguments.length;i++){
args.push(arguments[i]);
}
switch(type.toLowerCase()){
case "xml":
case "html":
case "xhtml":
return dojo.string.escapeXml.apply(this,args);
case "sql":
return dojo.string.escapeSql.apply(this,args);
case "regexp":
case "regex":
return dojo.string.escapeRegExp.apply(this,args);
case "javascript":
case "jscript":
case "js":
return dojo.string.escapeJavaScript.apply(this,args);
case "ascii":
return dojo.string.encodeAscii.apply(this,args);
default:
return str;
}
};
dojo.string.escapeXml=function(str,_126){
str=str.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;").replace(/"/gm,"&quot;");
if(!_126){
str=str.replace(/'/gm,"&#39;");
}
return str;
};
dojo.string.escapeSql=function(str){
return str.replace(/'/gm,"''");
};
dojo.string.escapeRegExp=function(str){
return str.replace(/\\/gm,"\\\\").replace(/([\f\b\n\t\r[\^$|?*+(){}])/gm,"\\$1");
};
dojo.string.escapeJavaScript=function(str){
return str.replace(/(["'\f\b\n\t\r])/gm,"\\$1");
};
dojo.string.escapeString=function(str){
return ("\""+str.replace(/(["\\])/g,"\\$1")+"\"").replace(/[\f]/g,"\\f").replace(/[\b]/g,"\\b").replace(/[\n]/g,"\\n").replace(/[\t]/g,"\\t").replace(/[\r]/g,"\\r");
};
dojo.string.summary=function(str,len){
if(!len||str.length<=len){
return str;
}else{
return str.substring(0,len).replace(/\.+$/,"")+"...";
}
};
dojo.string.endsWith=function(str,end,_12f){
if(_12f){
str=str.toLowerCase();
end=end.toLowerCase();
}
if((str.length-end.length)<0){
return false;
}
return str.lastIndexOf(end)==str.length-end.length;
};
dojo.string.endsWithAny=function(str){
for(var i=1;i<arguments.length;i++){
if(dojo.string.endsWith(str,arguments[i])){
return true;
}
}
return false;
};
dojo.string.startsWith=function(str,_133,_134){
if(_134){
str=str.toLowerCase();
_133=_133.toLowerCase();
}
return str.indexOf(_133)==0;
};
dojo.string.startsWithAny=function(str){
for(var i=1;i<arguments.length;i++){
if(dojo.string.startsWith(str,arguments[i])){
return true;
}
}
return false;
};
dojo.string.has=function(str){
for(var i=1;i<arguments.length;i++){
if(str.indexOf(arguments[i])>-1){
return true;
}
}
return false;
};
dojo.string.normalizeNewlines=function(text,_13a){
if(_13a=="\n"){
text=text.replace(/\r\n/g,"\n");
text=text.replace(/\r/g,"\n");
}else{
if(_13a=="\r"){
text=text.replace(/\r\n/g,"\r");
text=text.replace(/\n/g,"\r");
}else{
text=text.replace(/([^\r])\n/g,"$1\r\n");
text=text.replace(/\r([^\n])/g,"\r\n$1");
}
}
return text;
};
dojo.string.splitEscaped=function(str,_13c){
var _13d=[];
for(var i=0,prevcomma=0;i<str.length;i++){
if(str.charAt(i)=="\\"){
i++;
continue;
}
if(str.charAt(i)==_13c){
_13d.push(str.substring(prevcomma,i));
prevcomma=i+1;
}
}
_13d.push(str.substr(prevcomma));
return _13d;
};
dojo.provide("dojo.dom");
dojo.require("dojo.lang.array");
dojo.dom.ELEMENT_NODE=1;
dojo.dom.ATTRIBUTE_NODE=2;
dojo.dom.TEXT_NODE=3;
dojo.dom.CDATA_SECTION_NODE=4;
dojo.dom.ENTITY_REFERENCE_NODE=5;
dojo.dom.ENTITY_NODE=6;
dojo.dom.PROCESSING_INSTRUCTION_NODE=7;
dojo.dom.COMMENT_NODE=8;
dojo.dom.DOCUMENT_NODE=9;
dojo.dom.DOCUMENT_TYPE_NODE=10;
dojo.dom.DOCUMENT_FRAGMENT_NODE=11;
dojo.dom.NOTATION_NODE=12;
dojo.dom.dojoml="http://www.dojotoolkit.org/2004/dojoml";
dojo.dom.xmlns={svg:"http://www.w3.org/2000/svg",smil:"http://www.w3.org/2001/SMIL20/",mml:"http://www.w3.org/1998/Math/MathML",cml:"http://www.xml-cml.org",xlink:"http://www.w3.org/1999/xlink",xhtml:"http://www.w3.org/1999/xhtml",xul:"http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul",xbl:"http://www.mozilla.org/xbl",fo:"http://www.w3.org/1999/XSL/Format",xsl:"http://www.w3.org/1999/XSL/Transform",xslt:"http://www.w3.org/1999/XSL/Transform",xi:"http://www.w3.org/2001/XInclude",xforms:"http://www.w3.org/2002/01/xforms",saxon:"http://icl.com/saxon",xalan:"http://xml.apache.org/xslt",xsd:"http://www.w3.org/2001/XMLSchema",dt:"http://www.w3.org/2001/XMLSchema-datatypes",xsi:"http://www.w3.org/2001/XMLSchema-instance",rdf:"http://www.w3.org/1999/02/22-rdf-syntax-ns#",rdfs:"http://www.w3.org/2000/01/rdf-schema#",dc:"http://purl.org/dc/elements/1.1/",dcq:"http://purl.org/dc/qualifiers/1.0","soap-env":"http://schemas.xmlsoap.org/soap/envelope/",wsdl:"http://schemas.xmlsoap.org/wsdl/",AdobeExtensions:"http://ns.adobe.com/AdobeSVGViewerExtensions/3.0/"};
dojo.dom.isNode=function(wh){
if(typeof Element=="object"){
try{
return wh instanceof Element;
}
catch(E){
}
}else{
return wh&&!isNaN(wh.nodeType);
}
};
dojo.dom.getTagName=function(node){
dojo.deprecated("dojo.dom.getTagName","use node.tagName instead","0.4");
var _141=node.tagName;
if(_141.substr(0,5).toLowerCase()!="dojo:"){
if(_141.substr(0,4).toLowerCase()=="dojo"){
return "dojo:"+_141.substring(4).toLowerCase();
}
var djt=node.getAttribute("dojoType")||node.getAttribute("dojotype");
if(djt){
return "dojo:"+djt.toLowerCase();
}
if((node.getAttributeNS)&&(node.getAttributeNS(this.dojoml,"type"))){
return "dojo:"+node.getAttributeNS(this.dojoml,"type").toLowerCase();
}
try{
djt=node.getAttribute("dojo:type");
}
catch(e){
}
if(djt){
return "dojo:"+djt.toLowerCase();
}
if((!dj_global["djConfig"])||(!djConfig["ignoreClassNames"])){
var _143=node.className||node.getAttribute("class");
if((_143)&&(_143.indexOf)&&(_143.indexOf("dojo-")!=-1)){
var _144=_143.split(" ");
for(var x=0;x<_144.length;x++){
if((_144[x].length>5)&&(_144[x].indexOf("dojo-")>=0)){
return "dojo:"+_144[x].substr(5).toLowerCase();
}
}
}
}
}
return _141.toLowerCase();
};
dojo.dom.getUniqueId=function(){
do{
var id="dj_unique_"+(++arguments.callee._idIncrement);
}while(document.getElementById(id));
return id;
};
dojo.dom.getUniqueId._idIncrement=0;
dojo.dom.firstElement=dojo.dom.getFirstChildElement=function(_147,_148){
var node=_147.firstChild;
while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE){
node=node.nextSibling;
}
if(_148&&node&&node.tagName&&node.tagName.toLowerCase()!=_148.toLowerCase()){
node=dojo.dom.nextElement(node,_148);
}
return node;
};
dojo.dom.lastElement=dojo.dom.getLastChildElement=function(_14a,_14b){
var node=_14a.lastChild;
while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE){
node=node.previousSibling;
}
if(_14b&&node&&node.tagName&&node.tagName.toLowerCase()!=_14b.toLowerCase()){
node=dojo.dom.prevElement(node,_14b);
}
return node;
};
dojo.dom.nextElement=dojo.dom.getNextSiblingElement=function(node,_14e){
if(!node){
return null;
}
do{
node=node.nextSibling;
}while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE);
if(node&&_14e&&_14e.toLowerCase()!=node.tagName.toLowerCase()){
return dojo.dom.nextElement(node,_14e);
}
return node;
};
dojo.dom.prevElement=dojo.dom.getPreviousSiblingElement=function(node,_150){
if(!node){
return null;
}
if(_150){
_150=_150.toLowerCase();
}
do{
node=node.previousSibling;
}while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE);
if(node&&_150&&_150.toLowerCase()!=node.tagName.toLowerCase()){
return dojo.dom.prevElement(node,_150);
}
return node;
};
dojo.dom.moveChildren=function(_151,_152,trim){
var _154=0;
if(trim){
while(_151.hasChildNodes()&&_151.firstChild.nodeType==dojo.dom.TEXT_NODE){
_151.removeChild(_151.firstChild);
}
while(_151.hasChildNodes()&&_151.lastChild.nodeType==dojo.dom.TEXT_NODE){
_151.removeChild(_151.lastChild);
}
}
while(_151.hasChildNodes()){
_152.appendChild(_151.firstChild);
_154++;
}
return _154;
};
dojo.dom.copyChildren=function(_155,_156,trim){
var _158=_155.cloneNode(true);
return this.moveChildren(_158,_156,trim);
};
dojo.dom.removeChildren=function(node){
var _15a=node.childNodes.length;
while(node.hasChildNodes()){
node.removeChild(node.firstChild);
}
return _15a;
};
dojo.dom.replaceChildren=function(node,_15c){
dojo.dom.removeChildren(node);
node.appendChild(_15c);
};
dojo.dom.removeNode=function(node){
if(node&&node.parentNode){
return node.parentNode.removeChild(node);
}
};
dojo.dom.getAncestors=function(node,_15f,_160){
var _161=[];
var _162=dojo.lang.isFunction(_15f);
while(node){
if(!_162||_15f(node)){
_161.push(node);
}
if(_160&&_161.length>0){
return _161[0];
}
node=node.parentNode;
}
if(_160){
return null;
}
return _161;
};
dojo.dom.getAncestorsByTag=function(node,tag,_165){
tag=tag.toLowerCase();
return dojo.dom.getAncestors(node,function(el){
return ((el.tagName)&&(el.tagName.toLowerCase()==tag));
},_165);
};
dojo.dom.getFirstAncestorByTag=function(node,tag){
return dojo.dom.getAncestorsByTag(node,tag,true);
};
dojo.dom.isDescendantOf=function(node,_16a,_16b){
if(_16b&&node){
node=node.parentNode;
}
while(node){
if(node==_16a){
return true;
}
node=node.parentNode;
}
return false;
};
dojo.dom.innerXML=function(node){
if(node.innerXML){
return node.innerXML;
}else{
if(typeof XMLSerializer!="undefined"){
return (new XMLSerializer()).serializeToString(node);
}
}
};
dojo.dom.createDocumentFromText=function(str,_16e){
if(!_16e){
_16e="text/xml";
}
if(typeof DOMParser!="undefined"){
var _16f=new DOMParser();
return _16f.parseFromString(str,_16e);
}else{
if(typeof ActiveXObject!="undefined"){
var _170=new ActiveXObject("Microsoft.XMLDOM");
if(_170){
_170.async=false;
_170.loadXML(str);
return _170;
}else{
dojo.debug("toXml didn't work?");
}
}else{
if(document.createElement){
var tmp=document.createElement("xml");
tmp.innerHTML=str;
if(document.implementation&&document.implementation.createDocument){
var _172=document.implementation.createDocument("foo","",null);
for(var i=0;i<tmp.childNodes.length;i++){
_172.importNode(tmp.childNodes.item(i),true);
}
return _172;
}
return tmp.document&&tmp.document.firstChild?tmp.document.firstChild:tmp;
}
}
}
return null;
};
dojo.dom.prependChild=function(node,_175){
if(_175.firstChild){
_175.insertBefore(node,_175.firstChild);
}else{
_175.appendChild(node);
}
return true;
};
dojo.dom.insertBefore=function(node,ref,_178){
if(_178!=true&&(node===ref||node.nextSibling===ref)){
return false;
}
var _179=ref.parentNode;
_179.insertBefore(node,ref);
return true;
};
dojo.dom.insertAfter=function(node,ref,_17c){
var pn=ref.parentNode;
if(ref==pn.lastChild){
if((_17c!=true)&&(node===ref)){
return false;
}
pn.appendChild(node);
}else{
return this.insertBefore(node,ref.nextSibling,_17c);
}
return true;
};
dojo.dom.insertAtPosition=function(node,ref,_180){
if((!node)||(!ref)||(!_180)){
return false;
}
switch(_180.toLowerCase()){
case "before":
return dojo.dom.insertBefore(node,ref);
case "after":
return dojo.dom.insertAfter(node,ref);
case "first":
if(ref.firstChild){
return dojo.dom.insertBefore(node,ref.firstChild);
}else{
ref.appendChild(node);
return true;
}
break;
default:
ref.appendChild(node);
return true;
}
};
dojo.dom.insertAtIndex=function(node,_182,_183){
var _184=_182.childNodes;
if(!_184.length){
_182.appendChild(node);
return true;
}
var _185=null;
for(var i=0;i<_184.length;i++){
var _187=_184.item(i)["getAttribute"]?parseInt(_184.item(i).getAttribute("dojoinsertionindex")):-1;
if(_187<_183){
_185=_184.item(i);
}
}
if(_185){
return dojo.dom.insertAfter(node,_185);
}else{
return dojo.dom.insertBefore(node,_184.item(0));
}
};
dojo.dom.textContent=function(node,text){
if(text){
dojo.dom.replaceChildren(node,document.createTextNode(text));
return text;
}else{
var _18a="";
if(node==null){
return _18a;
}
for(var i=0;i<node.childNodes.length;i++){
switch(node.childNodes[i].nodeType){
case 1:
case 5:
_18a+=dojo.dom.textContent(node.childNodes[i]);
break;
case 3:
case 2:
case 4:
_18a+=node.childNodes[i].nodeValue;
break;
default:
break;
}
}
return _18a;
}
};
dojo.dom.collectionToArray=function(_18c){
dojo.deprecated("dojo.dom.collectionToArray","use dojo.lang.toArray instead","0.4");
return dojo.lang.toArray(_18c);
};
dojo.dom.hasParent=function(node){
return node&&node.parentNode&&dojo.dom.isNode(node.parentNode);
};
dojo.dom.isTag=function(node){
if(node&&node.tagName){
var arr=dojo.lang.toArray(arguments,1);
return arr[dojo.lang.find(node.tagName,arr)]||"";
}
return "";
};
dojo.provide("dojo.undo.browser");
dojo.require("dojo.io");
try{
if((!djConfig["preventBackButtonFix"])&&(!dojo.hostenv.post_load_)){
document.write("<iframe style='border: 0px; width: 1px; height: 1px; position: absolute; bottom: 0px; right: 0px; visibility: visible;' name='djhistory' id='djhistory' src='"+(dojo.hostenv.getBaseScriptUri()+"iframe_history.html")+"'></iframe>");
}
}
catch(e){
}
dojo.undo.browser={initialHref:window.location.href,initialHash:window.location.hash,moveForward:false,historyStack:[],forwardStack:[],historyIframe:null,bookmarkAnchor:null,locationTimer:null,setInitialState:function(args){
this.initialState={"url":this.initialHref,"kwArgs":args,"urlHash":this.initialHash};
},addToHistory:function(args){
var hash=null;
if(!this.historyIframe){
this.historyIframe=window.frames["djhistory"];
}
if(!this.bookmarkAnchor){
this.bookmarkAnchor=document.createElement("a");
(document.body||document.getElementsByTagName("body")[0]).appendChild(this.bookmarkAnchor);
this.bookmarkAnchor.style.display="none";
}
if((!args["changeUrl"])||(dojo.render.html.ie)){
var url=dojo.hostenv.getBaseScriptUri()+"iframe_history.html?"+(new Date()).getTime();
this.moveForward=true;
dojo.io.setIFrameSrc(this.historyIframe,url,false);
}
if(args["changeUrl"]){
this.changingUrl=true;
hash="#"+((args["changeUrl"]!==true)?args["changeUrl"]:(new Date()).getTime());
setTimeout("window.location.href = '"+hash+"'; dojo.undo.browser.changingUrl = false;",1);
this.bookmarkAnchor.href=hash;
if(dojo.render.html.ie){
var _194=args["back"]||args["backButton"]||args["handle"];
var tcb=function(_196){
if(window.location.hash!=""){
setTimeout("window.location.href = '"+hash+"';",1);
}
_194.apply(this,[_196]);
};
if(args["back"]){
args.back=tcb;
}else{
if(args["backButton"]){
args.backButton=tcb;
}else{
if(args["handle"]){
args.handle=tcb;
}
}
}
this.forwardStack=[];
var _197=args["forward"]||args["forwardButton"]||args["handle"];
var tfw=function(_199){
if(window.location.hash!=""){
window.location.href=hash;
}
if(_197){
_197.apply(this,[_199]);
}
};
if(args["forward"]){
args.forward=tfw;
}else{
if(args["forwardButton"]){
args.forwardButton=tfw;
}else{
if(args["handle"]){
args.handle=tfw;
}
}
}
}else{
if(dojo.render.html.moz){
if(!this.locationTimer){
this.locationTimer=setInterval("dojo.undo.browser.checkLocation();",200);
}
}
}
}
this.historyStack.push({"url":url,"kwArgs":args,"urlHash":hash});
},checkLocation:function(){
if(!this.changingUrl){
var hsl=this.historyStack.length;
if((window.location.hash==this.initialHash)||(window.location.href==this.initialHref)&&(hsl==1)){
this.handleBackButton();
return;
}
if(this.forwardStack.length>0){
if(this.forwardStack[this.forwardStack.length-1].urlHash==window.location.hash){
this.handleForwardButton();
return;
}
}
if((hsl>=2)&&(this.historyStack[hsl-2])){
if(this.historyStack[hsl-2].urlHash==window.location.hash){
this.handleBackButton();
return;
}
}
}
},iframeLoaded:function(evt,_19c){
var _19d=this._getUrlQuery(_19c.href);
if(_19d==null){
if(this.historyStack.length==1){
this.handleBackButton();
}
return;
}
if(this.moveForward){
this.moveForward=false;
return;
}
if(this.historyStack.length>=2&&_19d==this._getUrlQuery(this.historyStack[this.historyStack.length-2].url)){
this.handleBackButton();
}else{
if(this.forwardStack.length>0&&_19d==this._getUrlQuery(this.forwardStack[this.forwardStack.length-1].url)){
this.handleForwardButton();
}
}
},handleBackButton:function(){
var _19e=this.historyStack.pop();
if(!_19e){
return;
}
var last=this.historyStack[this.historyStack.length-1];
if(!last&&this.historyStack.length==0){
last=this.initialState;
}
if(last){
if(last.kwArgs["back"]){
last.kwArgs["back"]();
}else{
if(last.kwArgs["backButton"]){
last.kwArgs["backButton"]();
}else{
if(last.kwArgs["handle"]){
last.kwArgs.handle("back");
}
}
}
}
this.forwardStack.push(_19e);
},handleForwardButton:function(){
var last=this.forwardStack.pop();
if(!last){
return;
}
if(last.kwArgs["forward"]){
last.kwArgs.forward();
}else{
if(last.kwArgs["forwardButton"]){
last.kwArgs.forwardButton();
}else{
if(last.kwArgs["handle"]){
last.kwArgs.handle("forward");
}
}
}
this.historyStack.push(last);
},_getUrlQuery:function(url){
var _1a2=url.split("?");
if(_1a2.length<2){
return null;
}else{
return _1a2[1];
}
}};
dojo.provide("dojo.io.BrowserIO");
dojo.require("dojo.io");
dojo.require("dojo.lang.array");
dojo.require("dojo.lang.func");
dojo.require("dojo.string.extras");
dojo.require("dojo.dom");
dojo.require("dojo.undo.browser");
dojo.io.checkChildrenForFile=function(node){
var _1a4=false;
var _1a5=node.getElementsByTagName("input");
dojo.lang.forEach(_1a5,function(_1a6){
if(_1a4){
return;
}
if(_1a6.getAttribute("type")=="file"){
_1a4=true;
}
});
return _1a4;
};
dojo.io.formHasFile=function(_1a7){
return dojo.io.checkChildrenForFile(_1a7);
};
dojo.io.updateNode=function(node,_1a9){
node=dojo.byId(node);
var args=_1a9;
if(dojo.lang.isString(_1a9)){
args={url:_1a9};
}
args.mimetype="text/html";
args.load=function(t,d,e){
while(node.firstChild){
if(dojo["event"]){
try{
dojo.event.browser.clean(node.firstChild);
}
catch(e){
}
}
node.removeChild(node.firstChild);
}
node.innerHTML=d;
};
dojo.io.bind(args);
};
dojo.io.formFilter=function(node){
var type=(node.type||"").toLowerCase();
return !node.disabled&&node.name&&!dojo.lang.inArray(type,["file","submit","image","reset","button"]);
};
dojo.io.encodeForm=function(_1b0,_1b1,_1b2){
if((!_1b0)||(!_1b0.tagName)||(!_1b0.tagName.toLowerCase()=="form")){
dojo.raise("Attempted to encode a non-form element.");
}
if(!_1b2){
_1b2=dojo.io.formFilter;
}
var enc=/utf/i.test(_1b1||"")?encodeURIComponent:dojo.string.encodeAscii;
var _1b4=[];
for(var i=0;i<_1b0.elements.length;i++){
var elm=_1b0.elements[i];
if(!elm||elm.tagName.toLowerCase()=="fieldset"||!_1b2(elm)){
continue;
}
var name=enc(elm.name);
var type=elm.type.toLowerCase();
if(type=="select-multiple"){
for(var j=0;j<elm.options.length;j++){
if(elm.options[j].selected){
_1b4.push(name+"="+enc(elm.options[j].value));
}
}
}else{
if(dojo.lang.inArray(type,["radio","checkbox"])){
if(elm.checked){
_1b4.push(name+"="+enc(elm.value));
}
}else{
_1b4.push(name+"="+enc(elm.value));
}
}
}
var _1ba=_1b0.getElementsByTagName("input");
for(var i=0;i<_1ba.length;i++){
var _1bb=_1ba[i];
if(_1bb.type.toLowerCase()=="image"&&_1bb.form==_1b0&&_1b2(_1bb)){
var name=enc(_1bb.name);
_1b4.push(name+"="+enc(_1bb.value));
_1b4.push(name+".x=0");
_1b4.push(name+".y=0");
}
}
return _1b4.join("&")+"&";
};
dojo.io.FormBind=function(args){
this.bindArgs={};
if(args&&args.formNode){
this.init(args);
}else{
if(args){
this.init({formNode:args});
}
}
};
dojo.lang.extend(dojo.io.FormBind,{form:null,bindArgs:null,clickedButton:null,init:function(args){
var form=dojo.byId(args.formNode);
if(!form||!form.tagName||form.tagName.toLowerCase()!="form"){
throw new Error("FormBind: Couldn't apply, invalid form");
}else{
if(this.form==form){
return;
}else{
if(this.form){
throw new Error("FormBind: Already applied to a form");
}
}
}
dojo.lang.mixin(this.bindArgs,args);
this.form=form;
this.connect(form,"onsubmit","submit");
for(var i=0;i<form.elements.length;i++){
var node=form.elements[i];
if(node&&node.type&&dojo.lang.inArray(node.type.toLowerCase(),["submit","button"])){
this.connect(node,"onclick","click");
}
}
var _1c1=form.getElementsByTagName("input");
for(var i=0;i<_1c1.length;i++){
var _1c2=_1c1[i];
if(_1c2.type.toLowerCase()=="image"&&_1c2.form==form){
this.connect(_1c2,"onclick","click");
}
}
},onSubmit:function(form){
return true;
},submit:function(e){
e.preventDefault();
if(this.onSubmit(this.form)){
dojo.io.bind(dojo.lang.mixin(this.bindArgs,{formFilter:dojo.lang.hitch(this,"formFilter")}));
}
},click:function(e){
var node=e.currentTarget;
if(node.disabled){
return;
}
this.clickedButton=node;
},formFilter:function(node){
var type=(node.type||"").toLowerCase();
var _1c9=false;
if(node.disabled||!node.name){
_1c9=false;
}else{
if(dojo.lang.inArray(type,["submit","button","image"])){
if(!this.clickedButton){
this.clickedButton=node;
}
_1c9=node==this.clickedButton;
}else{
_1c9=!dojo.lang.inArray(type,["file","submit","reset","button"]);
}
}
return _1c9;
},connect:function(_1ca,_1cb,_1cc){
if(dojo.evalObjPath("dojo.event.connect")){
dojo.event.connect(_1ca,_1cb,this,_1cc);
}else{
var fcn=dojo.lang.hitch(this,_1cc);
_1ca[_1cb]=function(e){
if(!e){
e=window.event;
}
if(!e.currentTarget){
e.currentTarget=e.srcElement;
}
if(!e.preventDefault){
e.preventDefault=function(){
window.event.returnValue=false;
};
}
fcn(e);
};
}
}});
dojo.io.XMLHTTPTransport=new function(){
var _1cf=this;
var _1d0={};
this.useCache=false;
this.preventCache=false;
function getCacheKey(url,_1d2,_1d3){
return url+"|"+_1d2+"|"+_1d3.toLowerCase();
}
function addToCache(url,_1d5,_1d6,http){
_1d0[getCacheKey(url,_1d5,_1d6)]=http;
}
function getFromCache(url,_1d9,_1da){
return _1d0[getCacheKey(url,_1d9,_1da)];
}
this.clearCache=function(){
_1d0={};
};
function doLoad(_1db,http,url,_1de,_1df){
if((http.status==200)||(http.status==304)||(http.status==204)||(location.protocol=="file:"&&(http.status==0||http.status==undefined))||(location.protocol=="chrome:"&&(http.status==0||http.status==undefined))){
var ret;
if(_1db.method.toLowerCase()=="head"){
var _1e1=http.getAllResponseHeaders();
ret={};
ret.toString=function(){
return _1e1;
};
var _1e2=_1e1.split(/[\r\n]+/g);
for(var i=0;i<_1e2.length;i++){
var pair=_1e2[i].match(/^([^:]+)\s*:\s*(.+)$/i);
if(pair){
ret[pair[1]]=pair[2];
}
}
}else{
if(_1db.mimetype=="text/javascript"){
try{
ret=dj_eval(http.responseText);
}
catch(e){
dojo.debug(e);
dojo.debug(http.responseText);
ret=null;
}
}else{
if(_1db.mimetype=="text/json"){
try{
ret=dj_eval("("+http.responseText+")");
}
catch(e){
dojo.debug(e);
dojo.debug(http.responseText);
ret=false;
}
}else{
if((_1db.mimetype=="application/xml")||(_1db.mimetype=="text/xml")){
ret=http.responseXML;
if(!ret||typeof ret=="string"){
ret=dojo.dom.createDocumentFromText(http.responseText);
}
}else{
ret=http.responseText;
}
}
}
}
if(_1df){
addToCache(url,_1de,_1db.method,http);
}
_1db[(typeof _1db.load=="function")?"load":"handle"]("load",ret,http,_1db);
}else{
var _1e5=new dojo.io.Error("XMLHttpTransport Error: "+http.status+" "+http.statusText);
_1db[(typeof _1db.error=="function")?"error":"handle"]("error",_1e5,http,_1db);
}
}
function setHeaders(http,_1e7){
if(_1e7["headers"]){
for(var _1e8 in _1e7["headers"]){
if(_1e8.toLowerCase()=="content-type"&&!_1e7["contentType"]){
_1e7["contentType"]=_1e7["headers"][_1e8];
}else{
http.setRequestHeader(_1e8,_1e7["headers"][_1e8]);
}
}
}
}
this.inFlight=[];
this.inFlightTimer=null;
this.startWatchingInFlight=function(){
if(!this.inFlightTimer){
this.inFlightTimer=setInterval("dojo.io.XMLHTTPTransport.watchInFlight();",10);
}
};
this.watchInFlight=function(){
var now=null;
for(var x=this.inFlight.length-1;x>=0;x--){
var tif=this.inFlight[x];
if(!tif){
this.inFlight.splice(x,1);
continue;
}
if(4==tif.http.readyState){
this.inFlight.splice(x,1);
doLoad(tif.req,tif.http,tif.url,tif.query,tif.useCache);
}else{
if(tif.startTime){
if(!now){
now=(new Date()).getTime();
}
if(tif.startTime+(tif.req.timeoutSeconds*1000)<now){
if(typeof tif.http.abort=="function"){
tif.http.abort();
}
this.inFlight.splice(x,1);
tif.req[(typeof tif.req.timeout=="function")?"timeout":"handle"]("timeout",null,tif.http,tif.req);
}
}
}
}
if(this.inFlight.length==0){
clearInterval(this.inFlightTimer);
this.inFlightTimer=null;
}
};
var _1ec=dojo.hostenv.getXmlhttpObject()?true:false;
this.canHandle=function(_1ed){
return _1ec&&dojo.lang.inArray((_1ed["mimetype"].toLowerCase()||""),["text/plain","text/html","application/xml","text/xml","text/javascript","text/json"])&&!(_1ed["formNode"]&&dojo.io.formHasFile(_1ed["formNode"]));
};
this.multipartBoundary="45309FFF-BD65-4d50-99C9-36986896A96F";
this.bind=function(_1ee){
if(!_1ee["url"]){
if(!_1ee["formNode"]&&(_1ee["backButton"]||_1ee["back"]||_1ee["changeUrl"]||_1ee["watchForURL"])&&(!djConfig.preventBackButtonFix)){
dojo.deprecated("Using dojo.io.XMLHTTPTransport.bind() to add to browser history without doing an IO request is deprecated. Use dojo.undo.browser.addToHistory() instead.");
dojo.undo.browser.addToHistory(_1ee);
return true;
}
}
var url=_1ee.url;
var _1f0="";
if(_1ee["formNode"]){
var ta=_1ee.formNode.getAttribute("action");
if((ta)&&(!_1ee["url"])){
url=ta;
}
var tp=_1ee.formNode.getAttribute("method");
if((tp)&&(!_1ee["method"])){
_1ee.method=tp;
}
_1f0+=dojo.io.encodeForm(_1ee.formNode,_1ee.encoding,_1ee["formFilter"]);
}
if(url.indexOf("#")>-1){
dojo.debug("Warning: dojo.io.bind: stripping hash values from url:",url);
url=url.split("#")[0];
}
if(_1ee["file"]){
_1ee.method="post";
}
if(!_1ee["method"]){
_1ee.method="get";
}
if(_1ee.method.toLowerCase()=="get"){
_1ee.multipart=false;
}else{
if(_1ee["file"]){
_1ee.multipart=true;
}else{
if(!_1ee["multipart"]){
_1ee.multipart=false;
}
}
}
if(_1ee["backButton"]||_1ee["back"]||_1ee["changeUrl"]){
dojo.undo.browser.addToHistory(_1ee);
}
var _1f3=_1ee["content"]||{};
if(_1ee.sendTransport){
_1f3["dojo.transport"]="xmlhttp";
}
do{
if(_1ee.postContent){
_1f0=_1ee.postContent;
break;
}
if(_1f3){
_1f0+=dojo.io.argsFromMap(_1f3,_1ee.encoding);
}
if(_1ee.method.toLowerCase()=="get"||!_1ee.multipart){
break;
}
var t=[];
if(_1f0.length){
var q=_1f0.split("&");
for(var i=0;i<q.length;++i){
if(q[i].length){
var p=q[i].split("=");
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+p[0]+"\"","",p[1]);
}
}
}
if(_1ee.file){
if(dojo.lang.isArray(_1ee.file)){
for(var i=0;i<_1ee.file.length;++i){
var o=_1ee.file[i];
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+o.name+"\"; filename=\""+("fileName" in o?o.fileName:o.name)+"\"","Content-Type: "+("contentType" in o?o.contentType:"application/octet-stream"),"",o.content);
}
}else{
var o=_1ee.file;
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+o.name+"\"; filename=\""+("fileName" in o?o.fileName:o.name)+"\"","Content-Type: "+("contentType" in o?o.contentType:"application/octet-stream"),"",o.content);
}
}
if(t.length){
t.push("--"+this.multipartBoundary+"--","");
_1f0=t.join("\r\n");
}
}while(false);
var _1f9=_1ee["sync"]?false:true;
var _1fa=_1ee["preventCache"]||(this.preventCache==true&&_1ee["preventCache"]!=false);
var _1fb=_1ee["useCache"]==true||(this.useCache==true&&_1ee["useCache"]!=false);
if(!_1fa&&_1fb){
var _1fc=getFromCache(url,_1f0,_1ee.method);
if(_1fc){
doLoad(_1ee,_1fc,url,_1f0,false);
return;
}
}
var http=dojo.hostenv.getXmlhttpObject(_1ee);
var _1fe=false;
if(_1f9){
var _1ff=this.inFlight.push({"req":_1ee,"http":http,"url":url,"query":_1f0,"useCache":_1fb,"startTime":_1ee.timeoutSeconds?(new Date()).getTime():0});
this.startWatchingInFlight();
}
if(_1ee.method.toLowerCase()=="post"){
http.open("POST",url,_1f9);
setHeaders(http,_1ee);
http.setRequestHeader("Content-Type",_1ee.multipart?("multipart/form-data; boundary="+this.multipartBoundary):(_1ee.contentType||"application/x-www-form-urlencoded"));
try{
http.send(_1f0);
}
catch(e){
if(typeof http.abort=="function"){
http.abort();
}
doLoad(_1ee,{status:404},url,_1f0,_1fb);
}
}else{
var _200=url;
if(_1f0!=""){
_200+=(_200.indexOf("?")>-1?"&":"?")+_1f0;
}
if(_1fa){
_200+=(dojo.string.endsWithAny(_200,"?","&")?"":(_200.indexOf("?")>-1?"&":"?"))+"dojo.preventCache="+new Date().valueOf();
}
http.open(_1ee.method.toUpperCase(),_200,_1f9);
setHeaders(http,_1ee);
try{
http.send(null);
}
catch(e){
if(typeof http.abort=="function"){
http.abort();
}
doLoad(_1ee,{status:404},url,_1f0,_1fb);
}
}
if(!_1f9){
doLoad(_1ee,http,url,_1f0,_1fb);
}
_1ee.abort=function(){
return http.abort();
};
return;
};
dojo.io.transports.addTransport("XMLHTTPTransport");
};
dojo.provide("dojo.lang.extras");
dojo.require("dojo.lang.common");
dojo.lang.setTimeout=function(func,_202){
var _203=window,argsStart=2;
if(!dojo.lang.isFunction(func)){
_203=func;
func=_202;
_202=arguments[2];
argsStart++;
}
if(dojo.lang.isString(func)){
func=_203[func];
}
var args=[];
for(var i=argsStart;i<arguments.length;i++){
args.push(arguments[i]);
}
return setTimeout(function(){
func.apply(_203,args);
},_202);
};
dojo.lang.getNameInObj=function(ns,item){
if(!ns){
ns=dj_global;
}
for(var x in ns){
if(ns[x]===item){
return new String(x);
}
}
return null;
};
dojo.lang.shallowCopy=function(obj){
var ret={},key;
for(key in obj){
if(dojo.lang.isUndefined(ret[key])){
ret[key]=obj[key];
}
}
return ret;
};
dojo.lang.firstValued=function(){
for(var i=0;i<arguments.length;i++){
if(typeof arguments[i]!="undefined"){
return arguments[i];
}
}
return undefined;
};
dojo.lang.getObjPathValue=function(_20c,_20d,_20e){
with(dojo.parseObjPath(_20c,_20d,_20e)){
return dojo.evalProp(prop,obj,_20e);
}
};
dojo.lang.setObjPathValue=function(_20f,_210,_211,_212){
if(arguments.length<4){
_212=true;
}
with(dojo.parseObjPath(_20f,_211,_212)){
if(obj&&(_212||(prop in obj))){
obj[prop]=_210;
}
}
};
dojo.provide("dojo.event");
dojo.require("dojo.lang.array");
dojo.require("dojo.lang.extras");
dojo.require("dojo.lang.func");
dojo.event=new function(){
this.canTimeout=dojo.lang.isFunction(dj_global["setTimeout"])||dojo.lang.isAlien(dj_global["setTimeout"]);
function interpolateArgs(args){
var dl=dojo.lang;
var ao={srcObj:dj_global,srcFunc:null,adviceObj:dj_global,adviceFunc:null,aroundObj:null,aroundFunc:null,adviceType:(args.length>2)?args[0]:"after",precedence:"last",once:false,delay:null,rate:0,adviceMsg:false};
switch(args.length){
case 0:
return;
case 1:
return;
case 2:
ao.srcFunc=args[0];
ao.adviceFunc=args[1];
break;
case 3:
if((dl.isObject(args[0]))&&(dl.isString(args[1]))&&(dl.isString(args[2]))){
ao.adviceType="after";
ao.srcObj=args[0];
ao.srcFunc=args[1];
ao.adviceFunc=args[2];
}else{
if((dl.isString(args[1]))&&(dl.isString(args[2]))){
ao.srcFunc=args[1];
ao.adviceFunc=args[2];
}else{
if((dl.isObject(args[0]))&&(dl.isString(args[1]))&&(dl.isFunction(args[2]))){
ao.adviceType="after";
ao.srcObj=args[0];
ao.srcFunc=args[1];
var _216=dojo.lang.nameAnonFunc(args[2],ao.adviceObj);
ao.adviceFunc=_216;
}else{
if((dl.isFunction(args[0]))&&(dl.isObject(args[1]))&&(dl.isString(args[2]))){
ao.adviceType="after";
ao.srcObj=dj_global;
var _216=dojo.lang.nameAnonFunc(args[0],ao.srcObj);
ao.srcFunc=_216;
ao.adviceObj=args[1];
ao.adviceFunc=args[2];
}
}
}
}
break;
case 4:
if((dl.isObject(args[0]))&&(dl.isObject(args[2]))){
ao.adviceType="after";
ao.srcObj=args[0];
ao.srcFunc=args[1];
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isString(args[1]))&&(dl.isObject(args[2]))){
ao.adviceType=args[0];
ao.srcObj=dj_global;
ao.srcFunc=args[1];
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isFunction(args[1]))&&(dl.isObject(args[2]))){
ao.adviceType=args[0];
ao.srcObj=dj_global;
var _216=dojo.lang.nameAnonFunc(args[1],dj_global);
ao.srcFunc=_216;
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isObject(args[1]))&&(dl.isString(args[2]))&&(dl.isFunction(args[3]))){
ao.srcObj=args[1];
ao.srcFunc=args[2];
var _216=dojo.lang.nameAnonFunc(args[3],dj_global);
ao.adviceObj=dj_global;
ao.adviceFunc=_216;
}else{
if(dl.isObject(args[1])){
ao.srcObj=args[1];
ao.srcFunc=args[2];
ao.adviceObj=dj_global;
ao.adviceFunc=args[3];
}else{
if(dl.isObject(args[2])){
ao.srcObj=dj_global;
ao.srcFunc=args[1];
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
ao.srcObj=ao.adviceObj=ao.aroundObj=dj_global;
ao.srcFunc=args[1];
ao.adviceFunc=args[2];
ao.aroundFunc=args[3];
}
}
}
}
}
}
break;
case 6:
ao.srcObj=args[1];
ao.srcFunc=args[2];
ao.adviceObj=args[3];
ao.adviceFunc=args[4];
ao.aroundFunc=args[5];
ao.aroundObj=dj_global;
break;
default:
ao.srcObj=args[1];
ao.srcFunc=args[2];
ao.adviceObj=args[3];
ao.adviceFunc=args[4];
ao.aroundObj=args[5];
ao.aroundFunc=args[6];
ao.once=args[7];
ao.delay=args[8];
ao.rate=args[9];
ao.adviceMsg=args[10];
break;
}
if(dl.isFunction(ao.aroundFunc)){
var _216=dojo.lang.nameAnonFunc(ao.aroundFunc,ao.aroundObj);
ao.aroundFunc=_216;
}
if(!dl.isString(ao.srcFunc)){
ao.srcFunc=dojo.lang.getNameInObj(ao.srcObj,ao.srcFunc);
}
if(!dl.isString(ao.adviceFunc)){
ao.adviceFunc=dojo.lang.getNameInObj(ao.adviceObj,ao.adviceFunc);
}
if((ao.aroundObj)&&(!dl.isString(ao.aroundFunc))){
ao.aroundFunc=dojo.lang.getNameInObj(ao.aroundObj,ao.aroundFunc);
}
if(!ao.srcObj){
dojo.raise("bad srcObj for srcFunc: "+ao.srcFunc);
}
if(!ao.adviceObj){
dojo.raise("bad adviceObj for adviceFunc: "+ao.adviceFunc);
}
return ao;
}
this.connect=function(){
if(arguments.length==1){
var ao=arguments[0];
}else{
var ao=interpolateArgs(arguments);
}
if(dojo.lang.isArray(ao.srcObj)&&ao.srcObj!=""){
var _218={};
for(var x in ao){
_218[x]=ao[x];
}
var mjps=[];
dojo.lang.forEach(ao.srcObj,function(src){
if((dojo.render.html.capable)&&(dojo.lang.isString(src))){
src=dojo.byId(src);
}
_218.srcObj=src;
mjps.push(dojo.event.connect.call(dojo.event,_218));
});
return mjps;
}
var mjp=dojo.event.MethodJoinPoint.getForMethod(ao.srcObj,ao.srcFunc);
if(ao.adviceFunc){
var mjp2=dojo.event.MethodJoinPoint.getForMethod(ao.adviceObj,ao.adviceFunc);
}
mjp.kwAddAdvice(ao);
return mjp;
};
this.log=function(a1,a2){
var _220;
if((arguments.length==1)&&(typeof a1=="object")){
_220=a1;
}else{
_220={srcObj:a1,srcFunc:a2};
}
_220.adviceFunc=function(){
var _221=[];
for(var x=0;x<arguments.length;x++){
_221.push(arguments[x]);
}
dojo.debug("("+_220.srcObj+")."+_220.srcFunc,":",_221.join(", "));
};
this.kwConnect(_220);
};
this.connectBefore=function(){
var args=["before"];
for(var i=0;i<arguments.length;i++){
args.push(arguments[i]);
}
return this.connect.apply(this,args);
};
this.connectAround=function(){
var args=["around"];
for(var i=0;i<arguments.length;i++){
args.push(arguments[i]);
}
return this.connect.apply(this,args);
};
this.connectOnce=function(){
var ao=interpolateArgs(arguments);
ao.once=true;
return this.connect(ao);
};
this._kwConnectImpl=function(_228,_229){
var fn=(_229)?"disconnect":"connect";
if(typeof _228["srcFunc"]=="function"){
_228.srcObj=_228["srcObj"]||dj_global;
var _22b=dojo.lang.nameAnonFunc(_228.srcFunc,_228.srcObj);
_228.srcFunc=_22b;
}
if(typeof _228["adviceFunc"]=="function"){
_228.adviceObj=_228["adviceObj"]||dj_global;
var _22b=dojo.lang.nameAnonFunc(_228.adviceFunc,_228.adviceObj);
_228.adviceFunc=_22b;
}
return dojo.event[fn]((_228["type"]||_228["adviceType"]||"after"),_228["srcObj"]||dj_global,_228["srcFunc"],_228["adviceObj"]||_228["targetObj"]||dj_global,_228["adviceFunc"]||_228["targetFunc"],_228["aroundObj"],_228["aroundFunc"],_228["once"],_228["delay"],_228["rate"],_228["adviceMsg"]||false);
};
this.kwConnect=function(_22c){
return this._kwConnectImpl(_22c,false);
};
this.disconnect=function(){
var ao=interpolateArgs(arguments);
if(!ao.adviceFunc){
return;
}
var mjp=dojo.event.MethodJoinPoint.getForMethod(ao.srcObj,ao.srcFunc);
return mjp.removeAdvice(ao.adviceObj,ao.adviceFunc,ao.adviceType,ao.once);
};
this.kwDisconnect=function(_22f){
return this._kwConnectImpl(_22f,true);
};
};
dojo.event.MethodInvocation=function(_230,obj,args){
this.jp_=_230;
this.object=obj;
this.args=[];
for(var x=0;x<args.length;x++){
this.args[x]=args[x];
}
this.around_index=-1;
};
dojo.event.MethodInvocation.prototype.proceed=function(){
this.around_index++;
if(this.around_index>=this.jp_.around.length){
return this.jp_.object[this.jp_.methodname].apply(this.jp_.object,this.args);
}else{
var ti=this.jp_.around[this.around_index];
var mobj=ti[0]||dj_global;
var meth=ti[1];
return mobj[meth].call(mobj,this);
}
};
dojo.event.MethodJoinPoint=function(obj,_238){
this.object=obj||dj_global;
this.methodname=_238;
this.methodfunc=this.object[_238];
this.before=[];
this.after=[];
this.around=[];
};
dojo.event.MethodJoinPoint.getForMethod=function(obj,_23a){
if(!obj){
obj=dj_global;
}
if(!obj[_23a]){
obj[_23a]=function(){
};
}else{
if((!dojo.lang.isFunction(obj[_23a]))&&(!dojo.lang.isAlien(obj[_23a]))){
return null;
}
}
var _23b=_23a+"$joinpoint";
var _23c=_23a+"$joinpoint$method";
var _23d=obj[_23b];
if(!_23d){
var _23e=false;
if(dojo.event["browser"]){
if((obj["attachEvent"])||(obj["nodeType"])||(obj["addEventListener"])){
_23e=true;
dojo.event.browser.addClobberNodeAttrs(obj,[_23b,_23c,_23a]);
}
}
var _23f=obj[_23a].length;
obj[_23c]=obj[_23a];
_23d=obj[_23b]=new dojo.event.MethodJoinPoint(obj,_23c);
obj[_23a]=function(){
var args=[];
if((_23e)&&(!arguments.length)){
var evt=null;
try{
if(obj.ownerDocument){
evt=obj.ownerDocument.parentWindow.event;
}else{
if(obj.documentElement){
evt=obj.documentElement.ownerDocument.parentWindow.event;
}else{
evt=window.event;
}
}
}
catch(e){
evt=window.event;
}
if(evt){
args.push(dojo.event.browser.fixEvent(evt,this));
}
}else{
for(var x=0;x<arguments.length;x++){
if((x==0)&&(_23e)&&(dojo.event.browser.isEvent(arguments[x]))){
args.push(dojo.event.browser.fixEvent(arguments[x],this));
}else{
args.push(arguments[x]);
}
}
}
return _23d.run.apply(_23d,args);
};
obj[_23a].__preJoinArity=_23f;
}
return _23d;
};
dojo.lang.extend(dojo.event.MethodJoinPoint,{unintercept:function(){
this.object[this.methodname]=this.methodfunc;
this.before=[];
this.after=[];
this.around=[];
},disconnect:dojo.lang.forward("unintercept"),run:function(){
var obj=this.object||dj_global;
var args=arguments;
var _245=[];
for(var x=0;x<args.length;x++){
_245[x]=args[x];
}
var _247=function(marr){
if(!marr){
dojo.debug("Null argument to unrollAdvice()");
return;
}
var _249=marr[0]||dj_global;
var _24a=marr[1];
if(!_249[_24a]){
dojo.raise("function \""+_24a+"\" does not exist on \""+_249+"\"");
}
var _24b=marr[2]||dj_global;
var _24c=marr[3];
var msg=marr[6];
var _24e;
var to={args:[],jp_:this,object:obj,proceed:function(){
return _249[_24a].apply(_249,to.args);
}};
to.args=_245;
var _250=parseInt(marr[4]);
var _251=((!isNaN(_250))&&(marr[4]!==null)&&(typeof marr[4]!="undefined"));
if(marr[5]){
var rate=parseInt(marr[5]);
var cur=new Date();
var _254=false;
if((marr["last"])&&((cur-marr.last)<=rate)){
if(dojo.event.canTimeout){
if(marr["delayTimer"]){
clearTimeout(marr.delayTimer);
}
var tod=parseInt(rate*2);
var mcpy=dojo.lang.shallowCopy(marr);
marr.delayTimer=setTimeout(function(){
mcpy[5]=0;
_247(mcpy);
},tod);
}
return;
}else{
marr.last=cur;
}
}
if(_24c){
_24b[_24c].call(_24b,to);
}else{
if((_251)&&((dojo.render.html)||(dojo.render.svg))){
dj_global["setTimeout"](function(){
if(msg){
_249[_24a].call(_249,to);
}else{
_249[_24a].apply(_249,args);
}
},_250);
}else{
if(msg){
_249[_24a].call(_249,to);
}else{
_249[_24a].apply(_249,args);
}
}
}
};
if(this.before.length>0){
dojo.lang.forEach(this.before,_247);
}
var _257;
if(this.around.length>0){
var mi=new dojo.event.MethodInvocation(this,obj,args);
_257=mi.proceed();
}else{
if(this.methodfunc){
_257=this.object[this.methodname].apply(this.object,args);
}
}
if(this.after.length>0){
dojo.lang.forEach(this.after,_247);
}
return (this.methodfunc)?_257:null;
},getArr:function(kind){
var arr=this.after;
if((typeof kind=="string")&&(kind.indexOf("before")!=-1)){
arr=this.before;
}else{
if(kind=="around"){
arr=this.around;
}
}
return arr;
},kwAddAdvice:function(args){
this.addAdvice(args["adviceObj"],args["adviceFunc"],args["aroundObj"],args["aroundFunc"],args["adviceType"],args["precedence"],args["once"],args["delay"],args["rate"],args["adviceMsg"]);
},addAdvice:function(_25c,_25d,_25e,_25f,_260,_261,once,_263,rate,_265){
var arr=this.getArr(_260);
if(!arr){
dojo.raise("bad this: "+this);
}
var ao=[_25c,_25d,_25e,_25f,_263,rate,_265];
if(once){
if(this.hasAdvice(_25c,_25d,_260,arr)>=0){
return;
}
}
if(_261=="first"){
arr.unshift(ao);
}else{
arr.push(ao);
}
},hasAdvice:function(_268,_269,_26a,arr){
if(!arr){
arr=this.getArr(_26a);
}
var ind=-1;
for(var x=0;x<arr.length;x++){
if((arr[x][0]==_268)&&(arr[x][1]==_269)){
ind=x;
}
}
return ind;
},removeAdvice:function(_26e,_26f,_270,once){
var arr=this.getArr(_270);
var ind=this.hasAdvice(_26e,_26f,_270,arr);
if(ind==-1){
return false;
}
while(ind!=-1){
arr.splice(ind,1);
if(once){
break;
}
ind=this.hasAdvice(_26e,_26f,_270,arr);
}
return true;
}});
dojo.require("dojo.event");
dojo.provide("dojo.event.topic");
dojo.event.topic=new function(){
this.topics={};
this.getTopic=function(_274){
if(!this.topics[_274]){
this.topics[_274]=new this.TopicImpl(_274);
}
return this.topics[_274];
};
this.registerPublisher=function(_275,obj,_277){
var _275=this.getTopic(_275);
_275.registerPublisher(obj,_277);
};
this.subscribe=function(_278,obj,_27a){
var _278=this.getTopic(_278);
_278.subscribe(obj,_27a);
};
this.unsubscribe=function(_27b,obj,_27d){
var _27b=this.getTopic(_27b);
_27b.unsubscribe(obj,_27d);
};
this.destroy=function(_27e){
this.getTopic(_27e).destroy();
delete this.topics[_27e];
};
this.publish=function(_27f,_280){
var _27f=this.getTopic(_27f);
var args=[];
if(arguments.length==2&&(dojo.lang.isArray(_280)||_280.callee)){
args=_280;
}else{
var args=[];
for(var x=1;x<arguments.length;x++){
args.push(arguments[x]);
}
}
_27f.sendMessage.apply(_27f,args);
};
};
dojo.event.topic.TopicImpl=function(_283){
this.topicName=_283;
this.subscribe=function(_284,_285){
var tf=_285||_284;
var to=(!_285)?dj_global:_284;
dojo.event.kwConnect({srcObj:this,srcFunc:"sendMessage",adviceObj:to,adviceFunc:tf});
};
this.unsubscribe=function(_288,_289){
var tf=(!_289)?_288:_289;
var to=(!_289)?null:_288;
dojo.event.kwDisconnect({srcObj:this,srcFunc:"sendMessage",adviceObj:to,adviceFunc:tf});
};
this.destroy=function(){
dojo.event.MethodJoinPoint.getForMethod(this,"sendMessage").disconnect();
};
this.registerPublisher=function(_28c,_28d){
dojo.event.connect(_28c,_28d,this,"sendMessage");
};
this.sendMessage=function(_28e){
};
};
dojo.provide("dojo.event.browser");
dojo.require("dojo.event");
dojo_ie_clobber=new function(){
this.clobberNodes=[];
function nukeProp(node,prop){
try{
node[prop]=null;
}
catch(e){
}
try{
delete node[prop];
}
catch(e){
}
try{
node.removeAttribute(prop);
}
catch(e){
}
}
this.clobber=function(_291){
var na;
var tna;
if(_291){
tna=_291.all||_291.getElementsByTagName("*");
na=[_291];
for(var x=0;x<tna.length;x++){
if(tna[x]["__doClobber__"]){
na.push(tna[x]);
}
}
}else{
try{
window.onload=null;
}
catch(e){
}
na=(this.clobberNodes.length)?this.clobberNodes:document.all;
}
tna=null;
var _295={};
for(var i=na.length-1;i>=0;i=i-1){
var el=na[i];
if(el["__clobberAttrs__"]){
for(var j=0;j<el.__clobberAttrs__.length;j++){
nukeProp(el,el.__clobberAttrs__[j]);
}
nukeProp(el,"__clobberAttrs__");
nukeProp(el,"__doClobber__");
}
}
na=null;
};
};
if(dojo.render.html.ie){
window.onunload=function(){
dojo_ie_clobber.clobber();
try{
if((dojo["widget"])&&(dojo.widget["manager"])){
dojo.widget.manager.destroyAll();
}
}
catch(e){
}
try{
window.onload=null;
}
catch(e){
}
try{
window.onunload=null;
}
catch(e){
}
dojo_ie_clobber.clobberNodes=[];
};
}
dojo.event.browser=new function(){
var _299=0;
this.clean=function(node){
if(dojo.render.html.ie){
dojo_ie_clobber.clobber(node);
}
};
this.addClobberNode=function(node){
if(!node["__doClobber__"]){
node.__doClobber__=true;
dojo_ie_clobber.clobberNodes.push(node);
node.__clobberAttrs__=[];
}
};
this.addClobberNodeAttrs=function(node,_29d){
this.addClobberNode(node);
for(var x=0;x<_29d.length;x++){
node.__clobberAttrs__.push(_29d[x]);
}
};
this.removeListener=function(node,_2a0,fp,_2a2){
if(!_2a2){
var _2a2=false;
}
_2a0=_2a0.toLowerCase();
if(_2a0.substr(0,2)=="on"){
_2a0=_2a0.substr(2);
}
if(node.removeEventListener){
node.removeEventListener(_2a0,fp,_2a2);
}
};
this.addListener=function(node,_2a4,fp,_2a6,_2a7){
if(!node){
return;
}
if(!_2a6){
var _2a6=false;
}
_2a4=_2a4.toLowerCase();
if(_2a4.substr(0,2)!="on"){
_2a4="on"+_2a4;
}
if(!_2a7){
var _2a8=function(evt){
if(!evt){
evt=window.event;
}
var ret=fp(dojo.event.browser.fixEvent(evt,this));
if(_2a6){
dojo.event.browser.stopEvent(evt);
}
return ret;
};
}else{
_2a8=fp;
}
if(node.addEventListener){
node.addEventListener(_2a4.substr(2),_2a8,_2a6);
return _2a8;
}else{
if(typeof node[_2a4]=="function"){
var _2ab=node[_2a4];
node[_2a4]=function(e){
_2ab(e);
return _2a8(e);
};
}else{
node[_2a4]=_2a8;
}
if(dojo.render.html.ie){
this.addClobberNodeAttrs(node,[_2a4]);
}
return _2a8;
}
};
this.isEvent=function(obj){
return (typeof obj!="undefined")&&(typeof Event!="undefined")&&(obj.eventPhase);
};
this.currentEvent=null;
this.callListener=function(_2ae,_2af){
if(typeof _2ae!="function"){
dojo.raise("listener not a function: "+_2ae);
}
dojo.event.browser.currentEvent.currentTarget=_2af;
return _2ae.call(_2af,dojo.event.browser.currentEvent);
};
this.stopPropagation=function(){
dojo.event.browser.currentEvent.cancelBubble=true;
};
this.preventDefault=function(){
dojo.event.browser.currentEvent.returnValue=false;
};
this.keys={KEY_BACKSPACE:8,KEY_TAB:9,KEY_ENTER:13,KEY_SHIFT:16,KEY_CTRL:17,KEY_ALT:18,KEY_PAUSE:19,KEY_CAPS_LOCK:20,KEY_ESCAPE:27,KEY_SPACE:32,KEY_PAGE_UP:33,KEY_PAGE_DOWN:34,KEY_END:35,KEY_HOME:36,KEY_LEFT_ARROW:37,KEY_UP_ARROW:38,KEY_RIGHT_ARROW:39,KEY_DOWN_ARROW:40,KEY_INSERT:45,KEY_DELETE:46,KEY_LEFT_WINDOW:91,KEY_RIGHT_WINDOW:92,KEY_SELECT:93,KEY_F1:112,KEY_F2:113,KEY_F3:114,KEY_F4:115,KEY_F5:116,KEY_F6:117,KEY_F7:118,KEY_F8:119,KEY_F9:120,KEY_F10:121,KEY_F11:122,KEY_F12:123,KEY_NUM_LOCK:144,KEY_SCROLL_LOCK:145};
this.revKeys=[];
for(var key in this.keys){
this.revKeys[this.keys[key]]=key;
}
this.fixEvent=function(evt,_2b2){
if((!evt)&&(window["event"])){
var evt=window.event;
}
if((evt["type"])&&(evt["type"].indexOf("key")==0)){
evt.keys=this.revKeys;
for(var key in this.keys){
evt[key]=this.keys[key];
}
if((dojo.render.html.ie)&&(evt["type"]=="keypress")){
evt.charCode=evt.keyCode;
}
}
if(dojo.render.html.ie){
if(!evt.target){
evt.target=evt.srcElement;
}
if(!evt.currentTarget){
evt.currentTarget=(_2b2?_2b2:evt.srcElement);
}
if(!evt.layerX){
evt.layerX=evt.offsetX;
}
if(!evt.layerY){
evt.layerY=evt.offsetY;
}
if(!evt.pageX){
evt.pageX=evt.clientX+(window.pageXOffset||document.documentElement.scrollLeft||document.body.scrollLeft||0);
}
if(!evt.pageY){
evt.pageY=evt.clientY+(window.pageYOffset||document.documentElement.scrollTop||document.body.scrollTop||0);
}
if(evt.type=="mouseover"){
evt.relatedTarget=evt.fromElement;
}
if(evt.type=="mouseout"){
evt.relatedTarget=evt.toElement;
}
this.currentEvent=evt;
evt.callListener=this.callListener;
evt.stopPropagation=this.stopPropagation;
evt.preventDefault=this.preventDefault;
}
return evt;
};
this.stopEvent=function(ev){
if(window.event){
ev.returnValue=false;
ev.cancelBubble=true;
}else{
ev.preventDefault();
ev.stopPropagation();
}
};
};
dojo.kwCompoundRequire({common:["dojo.event","dojo.event.topic"],browser:["dojo.event.browser"],dashboard:["dojo.event.browser"]});
dojo.provide("dojo.event.*");
dojo.provide("dojo.lfx.Animation");
dojo.provide("dojo.lfx.Line");
dojo.require("dojo.lang.func");
dojo.lfx.Line=function(_2b5,end){
this.start=_2b5;
this.end=end;
if(dojo.lang.isArray(_2b5)){
var diff=[];
dojo.lang.forEach(this.start,function(s,i){
diff[i]=this.end[i]-s;
},this);
this.getValue=function(n){
var res=[];
dojo.lang.forEach(this.start,function(s,i){
res[i]=(diff[i]*n)+s;
},this);
return res;
};
}else{
var diff=end-_2b5;
this.getValue=function(n){
return (diff*n)+this.start;
};
}
};
dojo.lfx.easeIn=function(n){
return Math.pow(n,3);
};
dojo.lfx.easeOut=function(n){
return (1-Math.pow(1-n,3));
};
dojo.lfx.easeInOut=function(n){
return ((3*Math.pow(n,2))-(2*Math.pow(n,3)));
};
dojo.lfx.IAnimation=function(){
};
dojo.lang.extend(dojo.lfx.IAnimation,{curve:null,duration:1000,easing:null,repeatCount:0,rate:25,handler:null,beforeBegin:null,onBegin:null,onAnimate:null,onEnd:null,onPlay:null,onPause:null,onStop:null,play:null,pause:null,stop:null,fire:function(evt,args){
if(this[evt]){
if(args){
this[evt].apply(this,args);
}else{
this[evt].apply(this);
}
}
},_active:false,_paused:false});
dojo.lfx.Animation=function(_2c4,_2c5,_2c6,_2c7,_2c8,rate){
dojo.lfx.IAnimation.call(this);
if(dojo.lang.isNumber(_2c4)||(!_2c4&&_2c5.getValue)){
rate=_2c8;
_2c8=_2c7;
_2c7=_2c6;
_2c6=_2c5;
_2c5=_2c4;
_2c4=null;
}else{
if(_2c4.getValue||dojo.lang.isArray(_2c4)){
rate=_2c7;
_2c8=_2c6;
_2c7=_2c5;
_2c6=_2c4;
_2c5=null;
_2c4=null;
}
}
if(dojo.lang.isArray(_2c6)){
this.curve=new dojo.lfx.Line(_2c6[0],_2c6[1]);
}else{
this.curve=_2c6;
}
if(_2c5!=null&&_2c5>0){
this.duration=_2c5;
}
if(_2c8){
this.repeatCount=_2c8;
}
if(rate){
this.rate=rate;
}
if(_2c4){
this.handler=_2c4.handler;
this.beforeBegin=_2c4.beforeBegin;
this.onBegin=_2c4.onBegin;
this.onEnd=_2c4.onEnd;
this.onPlay=_2c4.onPlay;
this.onPause=_2c4.onPause;
this.onStop=_2c4.onStop;
this.onAnimate=_2c4.onAnimate;
}
if(_2c7&&dojo.lang.isFunction(_2c7)){
this.easing=_2c7;
}
};
dojo.inherits(dojo.lfx.Animation,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Animation,{_startTime:null,_endTime:null,_timer:null,_percent:0,_startRepeatCount:0,play:function(_2ca,_2cb){
if(_2cb){
clearTimeout(this._timer);
this._active=false;
this._paused=false;
this._percent=0;
}else{
if(this._active&&!this._paused){
return;
}
}
this.fire("beforeBegin");
if(_2ca>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_2cb);
}),_2ca);
return;
}
this._startTime=new Date().valueOf();
if(this._paused){
this._startTime-=(this.duration*this._percent/100);
}
this._endTime=this._startTime+this.duration;
this._active=true;
this._paused=false;
var step=this._percent/100;
var _2cd=this.curve.getValue(step);
if(this._percent==0){
if(!this._startRepeatCount){
this._startRepeatCount=this.repeatCount;
}
this.fire("handler",["begin",_2cd]);
this.fire("onBegin",[_2cd]);
}
this.fire("handler",["play",_2cd]);
this.fire("onPlay",[_2cd]);
this._cycle();
},pause:function(){
clearTimeout(this._timer);
if(!this._active){
return;
}
this._paused=true;
var _2ce=this.curve.getValue(this._percent/100);
this.fire("handler",["pause",_2ce]);
this.fire("onPause",[_2ce]);
},gotoPercent:function(pct,_2d0){
clearTimeout(this._timer);
this._active=true;
this._paused=true;
this._percent=pct;
if(_2d0){
this.play();
}
},stop:function(_2d1){
clearTimeout(this._timer);
var step=this._percent/100;
if(_2d1){
step=1;
}
var _2d3=this.curve.getValue(step);
this.fire("handler",["stop",_2d3]);
this.fire("onStop",[_2d3]);
this._active=false;
this._paused=false;
},status:function(){
if(this._active){
return this._paused?"paused":"playing";
}else{
return "stopped";
}
},_cycle:function(){
clearTimeout(this._timer);
if(this._active){
var curr=new Date().valueOf();
var step=(curr-this._startTime)/(this._endTime-this._startTime);
if(step>=1){
step=1;
this._percent=100;
}else{
this._percent=step*100;
}
if(this.easing&&dojo.lang.isFunction(this.easing)){
step=this.easing(step);
}
var _2d6=this.curve.getValue(step);
this.fire("handler",["animate",_2d6]);
this.fire("onAnimate",[_2d6]);
if(step<1){
this._timer=setTimeout(dojo.lang.hitch(this,"_cycle"),this.rate);
}else{
this._active=false;
this.fire("handler",["end"]);
this.fire("onEnd");
if(this.repeatCount>0){
this.repeatCount--;
this.play(null,true);
}else{
if(this.repeatCount==-1){
this.play(null,true);
}else{
if(this._startRepeatCount){
this.repeatCount=this._startRepeatCount;
this._startRepeatCount=0;
}
}
}
}
}
}});
dojo.lfx.Combine=function(){
dojo.lfx.IAnimation.call(this);
this._anims=[];
this._animsEnded=0;
var _2d7=arguments;
if(_2d7.length==1&&(dojo.lang.isArray(_2d7[0])||dojo.lang.isArrayLike(_2d7[0]))){
_2d7=_2d7[0];
}
var _2d8=this;
dojo.lang.forEach(_2d7,function(anim){
_2d8._anims.push(anim);
dojo.event.connect(anim,"onEnd",function(){
_2d8._onAnimsEnded();
});
});
};
dojo.inherits(dojo.lfx.Combine,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Combine,{_animsEnded:0,play:function(_2da,_2db){
if(!this._anims.length){
return;
}
this.fire("beforeBegin");
if(_2da>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_2db);
}),_2da);
return;
}
if(_2db||this._anims[0].percent==0){
this.fire("onBegin");
}
this.fire("onPlay");
this._animsCall("play",null,_2db);
},pause:function(){
this.fire("onPause");
this._animsCall("pause");
},stop:function(_2dc){
this.fire("onStop");
this._animsCall("stop",_2dc);
},_onAnimsEnded:function(){
this._animsEnded++;
if(this._animsEnded>=this._anims.length){
this.fire("onEnd");
}
},_animsCall:function(_2dd){
var args=[];
if(arguments.length>1){
for(var i=1;i<arguments.length;i++){
args.push(arguments[i]);
}
}
var _2e0=this;
dojo.lang.forEach(this._anims,function(anim){
anim[_2dd](args);
},_2e0);
}});
dojo.lfx.Chain=function(){
dojo.lfx.IAnimation.call(this);
this._anims=[];
this._currAnim=-1;
var _2e2=arguments;
if(_2e2.length==1&&(dojo.lang.isArray(_2e2[0])||dojo.lang.isArrayLike(_2e2[0]))){
_2e2=_2e2[0];
}
var _2e3=this;
dojo.lang.forEach(_2e2,function(anim,i,_2e6){
_2e3._anims.push(anim);
if(i<_2e6.length-1){
dojo.event.connect(anim,"onEnd",function(){
_2e3._playNext();
});
}else{
dojo.event.connect(anim,"onEnd",function(){
_2e3.fire("onEnd");
});
}
},_2e3);
};
dojo.inherits(dojo.lfx.Chain,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Chain,{_currAnim:-1,play:function(_2e7,_2e8){
if(!this._anims.length){
return;
}
if(_2e8||!this._anims[this._currAnim]){
this._currAnim=0;
}
this.fire("beforeBegin");
if(_2e7>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_2e8);
}),_2e7);
return;
}
if(this._anims[this._currAnim]){
if(this._currAnim==0){
this.fire("handler",["begin",this._currAnim]);
this.fire("onBegin",[this._currAnim]);
}
this.fire("onPlay",[this._currAnim]);
this._anims[this._currAnim].play(null,_2e8);
}
},pause:function(){
if(this._anims[this._currAnim]){
this._anims[this._currAnim].pause();
this.fire("onPause",[this._currAnim]);
}
},playPause:function(){
if(this._anims.length==0){
return;
}
if(this._currAnim==-1){
this._currAnim=0;
}
var _2e9=this._anims[this._currAnim];
if(_2e9){
if(!_2e9._active||_2e9._paused){
this.play();
}else{
this.pause();
}
}
},stop:function(){
if(this._anims[this._currAnim]){
this._anims[this._currAnim].stop();
this.fire("onStop",[this._currAnim]);
}
},_playNext:function(){
if(this._currAnim==-1||this._anims.length==0){
return;
}
this._currAnim++;
if(this._anims[this._currAnim]){
this._anims[this._currAnim].play(null,true);
}
}});
dojo.lfx.combine=function(){
var _2ea=arguments;
if(dojo.lang.isArray(arguments[0])){
_2ea=arguments[0];
}
return new dojo.lfx.Combine(_2ea);
};
dojo.lfx.chain=function(){
var _2eb=arguments;
if(dojo.lang.isArray(arguments[0])){
_2eb=arguments[0];
}
return new dojo.lfx.Chain(_2eb);
};
dojo.provide("dojo.graphics.color");
dojo.require("dojo.lang.array");
dojo.graphics.color.Color=function(r,g,b,a){
if(dojo.lang.isArray(r)){
this.r=r[0];
this.g=r[1];
this.b=r[2];
this.a=r[3]||1;
}else{
if(dojo.lang.isString(r)){
var rgb=dojo.graphics.color.extractRGB(r);
this.r=rgb[0];
this.g=rgb[1];
this.b=rgb[2];
this.a=g||1;
}else{
if(r instanceof dojo.graphics.color.Color){
this.r=r.r;
this.b=r.b;
this.g=r.g;
this.a=r.a;
}else{
this.r=r;
this.g=g;
this.b=b;
this.a=a;
}
}
}
};
dojo.graphics.color.Color.fromArray=function(arr){
return new dojo.graphics.color.Color(arr[0],arr[1],arr[2],arr[3]);
};
dojo.lang.extend(dojo.graphics.color.Color,{toRgb:function(_2f2){
if(_2f2){
return this.toRgba();
}else{
return [this.r,this.g,this.b];
}
},toRgba:function(){
return [this.r,this.g,this.b,this.a];
},toHex:function(){
return dojo.graphics.color.rgb2hex(this.toRgb());
},toCss:function(){
return "rgb("+this.toRgb().join()+")";
},toString:function(){
return this.toHex();
},blend:function(_2f3,_2f4){
return dojo.graphics.color.blend(this.toRgb(),new Color(_2f3).toRgb(),_2f4);
}});
dojo.graphics.color.named={white:[255,255,255],black:[0,0,0],red:[255,0,0],green:[0,255,0],blue:[0,0,255],navy:[0,0,128],gray:[128,128,128],silver:[192,192,192]};
dojo.graphics.color.blend=function(a,b,_2f7){
if(typeof a=="string"){
return dojo.graphics.color.blendHex(a,b,_2f7);
}
if(!_2f7){
_2f7=0;
}else{
if(_2f7>1){
_2f7=1;
}else{
if(_2f7<-1){
_2f7=-1;
}
}
}
var c=new Array(3);
for(var i=0;i<3;i++){
var half=Math.abs(a[i]-b[i])/2;
c[i]=Math.floor(Math.min(a[i],b[i])+half+(half*_2f7));
}
return c;
};
dojo.graphics.color.blendHex=function(a,b,_2fd){
return dojo.graphics.color.rgb2hex(dojo.graphics.color.blend(dojo.graphics.color.hex2rgb(a),dojo.graphics.color.hex2rgb(b),_2fd));
};
dojo.graphics.color.extractRGB=function(_2fe){
var hex="0123456789abcdef";
_2fe=_2fe.toLowerCase();
if(_2fe.indexOf("rgb")==0){
var _300=_2fe.match(/rgba*\((\d+), *(\d+), *(\d+)/i);
var ret=_300.splice(1,3);
return ret;
}else{
var _302=dojo.graphics.color.hex2rgb(_2fe);
if(_302){
return _302;
}else{
return dojo.graphics.color.named[_2fe]||[255,255,255];
}
}
};
dojo.graphics.color.hex2rgb=function(hex){
var _304="0123456789ABCDEF";
var rgb=new Array(3);
if(hex.indexOf("#")==0){
hex=hex.substring(1);
}
hex=hex.toUpperCase();
if(hex.replace(new RegExp("["+_304+"]","g"),"")!=""){
return null;
}
if(hex.length==3){
rgb[0]=hex.charAt(0)+hex.charAt(0);
rgb[1]=hex.charAt(1)+hex.charAt(1);
rgb[2]=hex.charAt(2)+hex.charAt(2);
}else{
rgb[0]=hex.substring(0,2);
rgb[1]=hex.substring(2,4);
rgb[2]=hex.substring(4);
}
for(var i=0;i<rgb.length;i++){
rgb[i]=_304.indexOf(rgb[i].charAt(0))*16+_304.indexOf(rgb[i].charAt(1));
}
return rgb;
};
dojo.graphics.color.rgb2hex=function(r,g,b){
if(dojo.lang.isArray(r)){
g=r[1]||0;
b=r[2]||0;
r=r[0]||0;
}
var ret=dojo.lang.map([r,g,b],function(x){
x=new Number(x);
var s=x.toString(16);
while(s.length<2){
s="0"+s;
}
return s;
});
ret.unshift("#");
return ret.join("");
};
dojo.provide("dojo.uri.Uri");
dojo.uri=new function(){
this.joinPath=function(){
var arr=[];
for(var i=0;i<arguments.length;i++){
arr.push(arguments[i]);
}
return arr.join("/").replace(/\/{2,}/g,"/").replace(/((https*|ftps*):)/i,"$1/");
};
this.dojoUri=function(uri){
return new dojo.uri.Uri(dojo.hostenv.getBaseScriptUri(),uri);
};
this.Uri=function(){
var uri=arguments[0];
for(var i=1;i<arguments.length;i++){
if(!arguments[i]){
continue;
}
var _312=new dojo.uri.Uri(arguments[i].toString());
var _313=new dojo.uri.Uri(uri.toString());
if(_312.path==""&&_312.scheme==null&&_312.authority==null&&_312.query==null){
if(_312.fragment!=null){
_313.fragment=_312.fragment;
}
_312=_313;
}else{
if(_312.scheme==null){
_312.scheme=_313.scheme;
if(_312.authority==null){
_312.authority=_313.authority;
if(_312.path.charAt(0)!="/"){
var path=_313.path.substring(0,_313.path.lastIndexOf("/")+1)+_312.path;
var segs=path.split("/");
for(var j=0;j<segs.length;j++){
if(segs[j]=="."){
if(j==segs.length-1){
segs[j]="";
}else{
segs.splice(j,1);
j--;
}
}else{
if(j>0&&!(j==1&&segs[0]=="")&&segs[j]==".."&&segs[j-1]!=".."){
if(j==segs.length-1){
segs.splice(j,1);
segs[j-1]="";
}else{
segs.splice(j-1,2);
j-=2;
}
}
}
}
_312.path=segs.join("/");
}
}
}
}
uri="";
if(_312.scheme!=null){
uri+=_312.scheme+":";
}
if(_312.authority!=null){
uri+="//"+_312.authority;
}
uri+=_312.path;
if(_312.query!=null){
uri+="?"+_312.query;
}
if(_312.fragment!=null){
uri+="#"+_312.fragment;
}
}
this.uri=uri.toString();
var _317="^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$";
var r=this.uri.match(new RegExp(_317));
this.scheme=r[2]||(r[1]?"":null);
this.authority=r[4]||(r[3]?"":null);
this.path=r[5];
this.query=r[7]||(r[6]?"":null);
this.fragment=r[9]||(r[8]?"":null);
if(this.authority!=null){
_317="^((([^:]+:)?([^@]+))@)?([^:]*)(:([0-9]+))?$";
r=this.authority.match(new RegExp(_317));
this.user=r[3]||null;
this.password=r[4]||null;
this.host=r[5];
this.port=r[7]||null;
}
this.toString=function(){
return this.uri;
};
};
};
dojo.provide("dojo.style");
dojo.require("dojo.graphics.color");
dojo.require("dojo.uri.Uri");
dojo.require("dojo.lang.common");
(function(){
var h=dojo.render.html;
var ds=dojo.style;
var db=document["body"]||document["documentElement"];
ds.boxSizing={MARGIN_BOX:"margin-box",BORDER_BOX:"border-box",PADDING_BOX:"padding-box",CONTENT_BOX:"content-box"};
var bs=ds.boxSizing;
ds.getBoxSizing=function(node){
if((h.ie)||(h.opera)){
var cm=document["compatMode"];
if((cm=="BackCompat")||(cm=="QuirksMode")){
return bs.BORDER_BOX;
}else{
return bs.CONTENT_BOX;
}
}else{
if(arguments.length==0){
node=document.documentElement;
}
var _31f=ds.getStyle(node,"-moz-box-sizing");
if(!_31f){
_31f=ds.getStyle(node,"box-sizing");
}
return (_31f?_31f:bs.CONTENT_BOX);
}
};
ds.isBorderBox=function(node){
return (ds.getBoxSizing(node)==bs.BORDER_BOX);
};
ds.getUnitValue=function(node,_322,_323){
var s=ds.getComputedStyle(node,_322);
if((!s)||((s=="auto")&&(_323))){
return {value:0,units:"px"};
}
if(dojo.lang.isUndefined(s)){
return ds.getUnitValue.bad;
}
var _325=s.match(/(\-?[\d.]+)([a-z%]*)/i);
if(!_325){
return ds.getUnitValue.bad;
}
return {value:Number(_325[1]),units:_325[2].toLowerCase()};
};
ds.getUnitValue.bad={value:NaN,units:""};
ds.getPixelValue=function(node,_327,_328){
var _329=ds.getUnitValue(node,_327,_328);
if(isNaN(_329.value)){
return 0;
}
if((_329.value)&&(_329.units!="px")){
return NaN;
}
return _329.value;
};
ds.getNumericStyle=function(){
dojo.deprecated("dojo.(style|html).getNumericStyle","in favor of dojo.(style|html).getPixelValue","0.4");
return ds.getPixelValue.apply(this,arguments);
};
ds.setPositivePixelValue=function(node,_32b,_32c){
if(isNaN(_32c)){
return false;
}
node.style[_32b]=Math.max(0,_32c)+"px";
return true;
};
ds._sumPixelValues=function(node,_32e,_32f){
var _330=0;
for(x=0;x<_32e.length;x++){
_330+=ds.getPixelValue(node,_32e[x],_32f);
}
return _330;
};
ds.isPositionAbsolute=function(node){
return (ds.getComputedStyle(node,"position")=="absolute");
};
ds.getBorderExtent=function(node,side){
return (ds.getStyle(node,"border-"+side+"-style")=="none"?0:ds.getPixelValue(node,"border-"+side+"-width"));
};
ds.getMarginWidth=function(node){
return ds._sumPixelValues(node,["margin-left","margin-right"],ds.isPositionAbsolute(node));
};
ds.getBorderWidth=function(node){
return ds.getBorderExtent(node,"left")+ds.getBorderExtent(node,"right");
};
ds.getPaddingWidth=function(node){
return ds._sumPixelValues(node,["padding-left","padding-right"],true);
};
ds.getPadBorderWidth=function(node){
return ds.getPaddingWidth(node)+ds.getBorderWidth(node);
};
ds.getContentBoxWidth=function(node){
node=dojo.byId(node);
return node.offsetWidth-ds.getPadBorderWidth(node);
};
ds.getBorderBoxWidth=function(node){
node=dojo.byId(node);
return node.offsetWidth;
};
ds.getMarginBoxWidth=function(node){
return ds.getInnerWidth(node)+ds.getMarginWidth(node);
};
ds.setContentBoxWidth=function(node,_33c){
node=dojo.byId(node);
if(ds.isBorderBox(node)){
_33c+=ds.getPadBorderWidth(node);
}
return ds.setPositivePixelValue(node,"width",_33c);
};
ds.setMarginBoxWidth=function(node,_33e){
node=dojo.byId(node);
if(!ds.isBorderBox(node)){
_33e-=ds.getPadBorderWidth(node);
}
_33e-=ds.getMarginWidth(node);
return ds.setPositivePixelValue(node,"width",_33e);
};
ds.getContentWidth=ds.getContentBoxWidth;
ds.getInnerWidth=ds.getBorderBoxWidth;
ds.getOuterWidth=ds.getMarginBoxWidth;
ds.setContentWidth=ds.setContentBoxWidth;
ds.setOuterWidth=ds.setMarginBoxWidth;
ds.getMarginHeight=function(node){
return ds._sumPixelValues(node,["margin-top","margin-bottom"],ds.isPositionAbsolute(node));
};
ds.getBorderHeight=function(node){
return ds.getBorderExtent(node,"top")+ds.getBorderExtent(node,"bottom");
};
ds.getPaddingHeight=function(node){
return ds._sumPixelValues(node,["padding-top","padding-bottom"],true);
};
ds.getPadBorderHeight=function(node){
return ds.getPaddingHeight(node)+ds.getBorderHeight(node);
};
ds.getContentBoxHeight=function(node){
node=dojo.byId(node);
return node.offsetHeight-ds.getPadBorderHeight(node);
};
ds.getBorderBoxHeight=function(node){
node=dojo.byId(node);
return node.offsetHeight;
};
ds.getMarginBoxHeight=function(node){
return ds.getInnerHeight(node)+ds.getMarginHeight(node);
};
ds.setContentBoxHeight=function(node,_347){
node=dojo.byId(node);
if(ds.isBorderBox(node)){
_347+=ds.getPadBorderHeight(node);
}
return ds.setPositivePixelValue(node,"height",_347);
};
ds.setMarginBoxHeight=function(node,_349){
node=dojo.byId(node);
if(!ds.isBorderBox(node)){
_349-=ds.getPadBorderHeight(node);
}
_349-=ds.getMarginHeight(node);
return ds.setPositivePixelValue(node,"height",_349);
};
ds.getContentHeight=ds.getContentBoxHeight;
ds.getInnerHeight=ds.getBorderBoxHeight;
ds.getOuterHeight=ds.getMarginBoxHeight;
ds.setContentHeight=ds.setContentBoxHeight;
ds.setOuterHeight=ds.setMarginBoxHeight;
ds.getAbsolutePosition=ds.abs=function(node,_34b){
var ret=[];
ret.x=ret.y=0;
var st=dojo.html.getScrollTop();
var sl=dojo.html.getScrollLeft();
if(h.ie){
with(node.getBoundingClientRect()){
ret.x=left-2;
ret.y=top-2;
}
}else{
if(node["offsetParent"]){
var _34f;
if((h.safari)&&(node.style.getPropertyValue("position")=="absolute")&&(node.parentNode==db)){
_34f=db;
}else{
_34f=db.parentNode;
}
if(node.parentNode!=db){
ret.x-=ds.sumAncestorProperties(node,"scrollLeft");
ret.y-=ds.sumAncestorProperties(node,"scrollTop");
}
do{
var n=node["offsetLeft"];
ret.x+=isNaN(n)?0:n;
var m=node["offsetTop"];
ret.y+=isNaN(m)?0:m;
node=node.offsetParent;
}while((node!=_34f)&&(node!=null));
}else{
if(node["x"]&&node["y"]){
ret.x+=isNaN(node.x)?0:node.x;
ret.y+=isNaN(node.y)?0:node.y;
}
}
}
if(_34b){
ret.y+=st;
ret.x+=sl;
}
ret[0]=ret.x;
ret[1]=ret.y;
return ret;
};
ds.sumAncestorProperties=function(node,prop){
node=dojo.byId(node);
if(!node){
return 0;
}
var _354=0;
while(node){
var val=node[prop];
if(val){
_354+=val-0;
}
node=node.parentNode;
}
return _354;
};
ds.getTotalOffset=function(node,type,_358){
node=dojo.byId(node);
return ds.abs(node,_358)[(type=="top")?"y":"x"];
};
ds.getAbsoluteX=ds.totalOffsetLeft=function(node,_35a){
return ds.getTotalOffset(node,"left",_35a);
};
ds.getAbsoluteY=ds.totalOffsetTop=function(node,_35c){
return ds.getTotalOffset(node,"top",_35c);
};
ds.styleSheet=null;
ds.insertCssRule=function(_35d,_35e,_35f){
if(!ds.styleSheet){
if(document.createStyleSheet){
ds.styleSheet=document.createStyleSheet();
}else{
if(document.styleSheets[0]){
ds.styleSheet=document.styleSheets[0];
}else{
return null;
}
}
}
if(arguments.length<3){
if(ds.styleSheet.cssRules){
_35f=ds.styleSheet.cssRules.length;
}else{
if(ds.styleSheet.rules){
_35f=ds.styleSheet.rules.length;
}else{
return null;
}
}
}
if(ds.styleSheet.insertRule){
var rule=_35d+" { "+_35e+" }";
return ds.styleSheet.insertRule(rule,_35f);
}else{
if(ds.styleSheet.addRule){
return ds.styleSheet.addRule(_35d,_35e,_35f);
}else{
return null;
}
}
};
ds.removeCssRule=function(_361){
if(!ds.styleSheet){
dojo.debug("no stylesheet defined for removing rules");
return false;
}
if(h.ie){
if(!_361){
_361=ds.styleSheet.rules.length;
ds.styleSheet.removeRule(_361);
}
}else{
if(document.styleSheets[0]){
if(!_361){
_361=ds.styleSheet.cssRules.length;
}
ds.styleSheet.deleteRule(_361);
}
}
return true;
};
ds.insertCssFile=function(URI,doc,_364){
if(!URI){
return;
}
if(!doc){
doc=document;
}
var _365=dojo.hostenv.getText(URI);
_365=ds.fixPathsInCssText(_365,URI);
if(_364){
var _366=doc.getElementsByTagName("style");
var _367="";
for(var i=0;i<_366.length;i++){
_367=(_366[i].styleSheet&&_366[i].styleSheet.cssText)?_366[i].styleSheet.cssText:_366[i].innerHTML;
if(_365==_367){
return;
}
}
}
var _369=ds.insertCssText(_365);
if(_369&&djConfig.isDebug){
_369.setAttribute("dbgHref",URI);
}
return _369;
};
ds.insertCssText=function(_36a,doc,URI){
if(!_36a){
return;
}
if(!doc){
doc=document;
}
if(URI){
_36a=ds.fixPathsInCssText(_36a,URI);
}
var _36d=doc.createElement("style");
_36d.setAttribute("type","text/css");
if(_36d.styleSheet){
_36d.styleSheet.cssText=_36a;
}else{
var _36e=doc.createTextNode(_36a);
_36d.appendChild(_36e);
}
var head=doc.getElementsByTagName("head")[0];
if(!head){
dojo.debug("No head tag in document, aborting styles");
}else{
head.appendChild(_36d);
}
return _36d;
};
ds.fixPathsInCssText=function(_370,URI){
if(!_370||!URI){
return;
}
var pos=0;
var str="";
var url="";
while(pos!=-1){
pos=0;
url="";
pos=_370.indexOf("url(",pos);
if(pos<0){
break;
}
str+=_370.slice(0,pos+4);
_370=_370.substring(pos+4,_370.length);
url+=_370.match(/^[\t\s\w()\/.\\'"-:#=&?]*\)/)[0];
_370=_370.substring(url.length-1,_370.length);
url=url.replace(/^[\s\t]*(['"]?)([\w()\/.\\'"-:#=&?]*)\1[\s\t]*?\)/,"$2");
if(url.search(/(file|https?|ftps?):\/\//)==-1){
url=(new dojo.uri.Uri(URI,url).toString());
}
str+=url;
}
return str+_370;
};
ds.getBackgroundColor=function(node){
node=dojo.byId(node);
var _376;
do{
_376=ds.getStyle(node,"background-color");
if(_376.toLowerCase()=="rgba(0, 0, 0, 0)"){
_376="transparent";
}
if(node==document.getElementsByTagName("body")[0]){
node=null;
break;
}
node=node.parentNode;
}while(node&&dojo.lang.inArray(_376,["transparent",""]));
if(_376=="transparent"){
_376=[255,255,255,0];
}else{
_376=dojo.graphics.color.extractRGB(_376);
}
return _376;
};
ds.getComputedStyle=function(node,_378,_379){
node=dojo.byId(node);
var _378=ds.toSelectorCase(_378);
var _37a=ds.toCamelCase(_378);
if(!node||!node.style){
return _379;
}else{
if(document.defaultView){
try{
var cs=document.defaultView.getComputedStyle(node,"");
if(cs){
return cs.getPropertyValue(_378);
}
}
catch(e){
if(node.style.getPropertyValue){
return node.style.getPropertyValue(_378);
}else{
return _379;
}
}
}else{
if(node.currentStyle){
return node.currentStyle[_37a];
}
}
}
if(node.style.getPropertyValue){
return node.style.getPropertyValue(_378);
}else{
return _379;
}
};
ds.getStyleProperty=function(node,_37d){
node=dojo.byId(node);
return (node&&node.style?node.style[ds.toCamelCase(_37d)]:undefined);
};
ds.getStyle=function(node,_37f){
var _380=ds.getStyleProperty(node,_37f);
return (_380?_380:ds.getComputedStyle(node,_37f));
};
ds.setStyle=function(node,_382,_383){
node=dojo.byId(node);
if(node&&node.style){
var _384=ds.toCamelCase(_382);
node.style[_384]=_383;
}
};
ds.toCamelCase=function(_385){
var arr=_385.split("-"),cc=arr[0];
for(var i=1;i<arr.length;i++){
cc+=arr[i].charAt(0).toUpperCase()+arr[i].substring(1);
}
return cc;
};
ds.toSelectorCase=function(_388){
return _388.replace(/([A-Z])/g,"-$1").toLowerCase();
};
ds.setOpacity=function setOpacity(node,_38a,_38b){
node=dojo.byId(node);
if(!_38b){
if(_38a>=1){
if(h.ie){
ds.clearOpacity(node);
return;
}else{
_38a=0.999999;
}
}else{
if(_38a<0){
_38a=0;
}
}
}
if(h.ie){
if(node.nodeName.toLowerCase()=="tr"){
var tds=node.getElementsByTagName("td");
for(var x=0;x<tds.length;x++){
tds[x].style.filter="Alpha(Opacity="+_38a*100+")";
}
}
node.style.filter="Alpha(Opacity="+_38a*100+")";
}else{
if(h.moz){
node.style.opacity=_38a;
node.style.MozOpacity=_38a;
}else{
if(h.safari){
node.style.opacity=_38a;
node.style.KhtmlOpacity=_38a;
}else{
node.style.opacity=_38a;
}
}
}
};
ds.getOpacity=function getOpacity(node){
node=dojo.byId(node);
if(h.ie){
var opac=(node.filters&&node.filters.alpha&&typeof node.filters.alpha.opacity=="number"?node.filters.alpha.opacity:100)/100;
}else{
var opac=node.style.opacity||node.style.MozOpacity||node.style.KhtmlOpacity||1;
}
return opac>=0.999999?1:Number(opac);
};
ds.clearOpacity=function clearOpacity(node){
node=dojo.byId(node);
var ns=node.style;
if(h.ie){
try{
if(node.filters&&node.filters.alpha){
ns.filter="";
}
}
catch(e){
}
}else{
if(h.moz){
ns.opacity=1;
ns.MozOpacity=1;
}else{
if(h.safari){
ns.opacity=1;
ns.KhtmlOpacity=1;
}else{
ns.opacity=1;
}
}
}
};
ds._toggle=function(node,_393,_394){
node=dojo.byId(node);
_394(node,!_393(node));
return _393(node);
};
ds.show=function(node){
node=dojo.byId(node);
if(ds.getStyleProperty(node,"display")=="none"){
ds.setStyle(node,"display",(node.dojoDisplayCache||""));
node.dojoDisplayCache=undefined;
}
};
ds.hide=function(node){
node=dojo.byId(node);
if(typeof node["dojoDisplayCache"]=="undefined"){
var d=ds.getStyleProperty(node,"display");
if(d!="none"){
node.dojoDisplayCache=d;
}
}
ds.setStyle(node,"display","none");
};
ds.setShowing=function(node,_399){
ds[(_399?"show":"hide")](node);
};
ds.isShowing=function(node){
return (ds.getStyleProperty(node,"display")!="none");
};
ds.toggleShowing=function(node){
return ds._toggle(node,ds.isShowing,ds.setShowing);
};
ds.displayMap={tr:"",td:"",th:"",img:"inline",span:"inline",input:"inline",button:"inline"};
ds.suggestDisplayByTagName=function(node){
node=dojo.byId(node);
if(node&&node.tagName){
var tag=node.tagName.toLowerCase();
return (tag in ds.displayMap?ds.displayMap[tag]:"block");
}
};
ds.setDisplay=function(node,_39f){
ds.setStyle(node,"display",(dojo.lang.isString(_39f)?_39f:(_39f?ds.suggestDisplayByTagName(node):"none")));
};
ds.isDisplayed=function(node){
return (ds.getComputedStyle(node,"display")!="none");
};
ds.toggleDisplay=function(node){
return ds._toggle(node,ds.isDisplayed,ds.setDisplay);
};
ds.setVisibility=function(node,_3a3){
ds.setStyle(node,"visibility",(dojo.lang.isString(_3a3)?_3a3:(_3a3?"visible":"hidden")));
};
ds.isVisible=function(node){
return (ds.getComputedStyle(node,"visibility")!="hidden");
};
ds.toggleVisibility=function(node){
return ds._toggle(node,ds.isVisible,ds.setVisibility);
};
ds.toCoordinateArray=function(_3a6,_3a7){
if(dojo.lang.isArray(_3a6)){
while(_3a6.length<4){
_3a6.push(0);
}
while(_3a6.length>4){
_3a6.pop();
}
var ret=_3a6;
}else{
var node=dojo.byId(_3a6);
var pos=ds.getAbsolutePosition(node,_3a7);
var ret=[pos.x,pos.y,ds.getBorderBoxWidth(node),ds.getBorderBoxHeight(node)];
}
ret.x=ret[0];
ret.y=ret[1];
ret.w=ret[2];
ret.h=ret[3];
return ret;
};
})();
dojo.provide("dojo.html");
dojo.require("dojo.lang.func");
dojo.require("dojo.dom");
dojo.require("dojo.style");
dojo.require("dojo.string");
dojo.lang.mixin(dojo.html,dojo.dom);
dojo.lang.mixin(dojo.html,dojo.style);
dojo.html.clearSelection=function(){
try{
if(window["getSelection"]){
if(dojo.render.html.safari){
window.getSelection().collapse();
}else{
window.getSelection().removeAllRanges();
}
}else{
if(document.selection){
if(document.selection.empty){
document.selection.empty();
}else{
if(document.selection.clear){
document.selection.clear();
}
}
}
}
return true;
}
catch(e){
dojo.debug(e);
return false;
}
};
dojo.html.disableSelection=function(_3ab){
_3ab=dojo.byId(_3ab)||document.body;
var h=dojo.render.html;
if(h.mozilla){
_3ab.style.MozUserSelect="none";
}else{
if(h.safari){
_3ab.style.KhtmlUserSelect="none";
}else{
if(h.ie){
_3ab.unselectable="on";
}else{
return false;
}
}
}
return true;
};
dojo.html.enableSelection=function(_3ad){
_3ad=dojo.byId(_3ad)||document.body;
var h=dojo.render.html;
if(h.mozilla){
_3ad.style.MozUserSelect="";
}else{
if(h.safari){
_3ad.style.KhtmlUserSelect="";
}else{
if(h.ie){
_3ad.unselectable="off";
}else{
return false;
}
}
}
return true;
};
dojo.html.selectElement=function(_3af){
_3af=dojo.byId(_3af);
if(document.selection&&document.body.createTextRange){
var _3b0=document.body.createTextRange();
_3b0.moveToElementText(_3af);
_3b0.select();
}else{
if(window["getSelection"]){
var _3b1=window.getSelection();
if(_3b1["selectAllChildren"]){
_3b1.selectAllChildren(_3af);
}
}
}
};
dojo.html.selectInputText=function(_3b2){
_3b2=dojo.byId(_3b2);
if(document.selection&&document.body.createTextRange){
var _3b3=_3b2.createTextRange();
_3b3.moveStart("character",0);
_3b3.moveEnd("character",_3b2.value.length);
_3b3.select();
}else{
if(window["getSelection"]){
var _3b4=window.getSelection();
_3b2.setSelectionRange(0,_3b2.value.length);
}
}
_3b2.focus();
};
dojo.html.isSelectionCollapsed=function(){
if(document["selection"]){
return document.selection.createRange().text=="";
}else{
if(window["getSelection"]){
var _3b5=window.getSelection();
if(dojo.lang.isString(_3b5)){
return _3b5=="";
}else{
return _3b5.isCollapsed;
}
}
}
};
dojo.html.getEventTarget=function(evt){
if(!evt){
evt=window.event||{};
}
var t=(evt.srcElement?evt.srcElement:(evt.target?evt.target:null));
while((t)&&(t.nodeType!=1)){
t=t.parentNode;
}
return t;
};
dojo.html.getDocumentWidth=function(){
dojo.deprecated("dojo.html.getDocument* has been deprecated in favor of dojo.html.getViewport*");
return dojo.html.getViewportWidth();
};
dojo.html.getDocumentHeight=function(){
dojo.deprecated("dojo.html.getDocument* has been deprecated in favor of dojo.html.getViewport*");
return dojo.html.getViewportHeight();
};
dojo.html.getDocumentSize=function(){
dojo.deprecated("dojo.html.getDocument* has been deprecated in favor of dojo.html.getViewport*");
return dojo.html.getViewportSize();
};
dojo.html.getViewportWidth=function(){
var w=0;
if(window.innerWidth){
w=window.innerWidth;
}
if(dojo.exists(document,"documentElement.clientWidth")){
var w2=document.documentElement.clientWidth;
if(!w||w2&&w2<w){
w=w2;
}
return w;
}
if(document.body){
return document.body.clientWidth;
}
return 0;
};
dojo.html.getViewportHeight=function(){
if(window.innerHeight){
return window.innerHeight;
}
if(dojo.exists(document,"documentElement.clientHeight")){
return document.documentElement.clientHeight;
}
if(document.body){
return document.body.clientHeight;
}
return 0;
};
dojo.html.getViewportSize=function(){
var ret=[dojo.html.getViewportWidth(),dojo.html.getViewportHeight()];
ret.w=ret[0];
ret.h=ret[1];
return ret;
};
dojo.html.getScrollTop=function(){
return window.pageYOffset||document.documentElement.scrollTop||document.body.scrollTop||0;
};
dojo.html.getScrollLeft=function(){
return window.pageXOffset||document.documentElement.scrollLeft||document.body.scrollLeft||0;
};
dojo.html.getScrollOffset=function(){
var off=[dojo.html.getScrollLeft(),dojo.html.getScrollTop()];
off.x=off[0];
off.y=off[1];
return off;
};
dojo.html.getParentOfType=function(node,type){
dojo.deprecated("dojo.html.getParentOfType has been deprecated in favor of dojo.html.getParentByType*");
return dojo.html.getParentByType(node,type);
};
dojo.html.getParentByType=function(node,type){
var _3c0=dojo.byId(node);
type=type.toLowerCase();
while((_3c0)&&(_3c0.nodeName.toLowerCase()!=type)){
if(_3c0==(document["body"]||document["documentElement"])){
return null;
}
_3c0=_3c0.parentNode;
}
return _3c0;
};
dojo.html.getAttribute=function(node,attr){
node=dojo.byId(node);
if((!node)||(!node.getAttribute)){
return null;
}
var ta=typeof attr=="string"?attr:new String(attr);
var v=node.getAttribute(ta.toUpperCase());
if((v)&&(typeof v=="string")&&(v!="")){
return v;
}
if(v&&v.value){
return v.value;
}
if((node.getAttributeNode)&&(node.getAttributeNode(ta))){
return (node.getAttributeNode(ta)).value;
}else{
if(node.getAttribute(ta)){
return node.getAttribute(ta);
}else{
if(node.getAttribute(ta.toLowerCase())){
return node.getAttribute(ta.toLowerCase());
}
}
}
return null;
};
dojo.html.hasAttribute=function(node,attr){
node=dojo.byId(node);
return dojo.html.getAttribute(node,attr)?true:false;
};
dojo.html.getClass=function(node){
node=dojo.byId(node);
if(!node){
return "";
}
var cs="";
if(node.className){
cs=node.className;
}else{
if(dojo.html.hasAttribute(node,"class")){
cs=dojo.html.getAttribute(node,"class");
}
}
return dojo.string.trim(cs);
};
dojo.html.getClasses=function(node){
var c=dojo.html.getClass(node);
return (c=="")?[]:c.split(/\s+/g);
};
dojo.html.hasClass=function(node,_3cc){
return dojo.lang.inArray(dojo.html.getClasses(node),_3cc);
};
dojo.html.prependClass=function(node,_3ce){
_3ce+=" "+dojo.html.getClass(node);
return dojo.html.setClass(node,_3ce);
};
dojo.html.addClass=function(node,_3d0){
if(dojo.html.hasClass(node,_3d0)){
return false;
}
_3d0=dojo.string.trim(dojo.html.getClass(node)+" "+_3d0);
return dojo.html.setClass(node,_3d0);
};
dojo.html.setClass=function(node,_3d2){
node=dojo.byId(node);
var cs=new String(_3d2);
try{
if(typeof node.className=="string"){
node.className=cs;
}else{
if(node.setAttribute){
node.setAttribute("class",_3d2);
node.className=cs;
}else{
return false;
}
}
}
catch(e){
dojo.debug("dojo.html.setClass() failed",e);
}
return true;
};
dojo.html.removeClass=function(node,_3d5,_3d6){
var _3d5=dojo.string.trim(new String(_3d5));
try{
var cs=dojo.html.getClasses(node);
var nca=[];
if(_3d6){
for(var i=0;i<cs.length;i++){
if(cs[i].indexOf(_3d5)==-1){
nca.push(cs[i]);
}
}
}else{
for(var i=0;i<cs.length;i++){
if(cs[i]!=_3d5){
nca.push(cs[i]);
}
}
}
dojo.html.setClass(node,nca.join(" "));
}
catch(e){
dojo.debug("dojo.html.removeClass() failed",e);
}
return true;
};
dojo.html.replaceClass=function(node,_3db,_3dc){
dojo.html.removeClass(node,_3dc);
dojo.html.addClass(node,_3db);
};
dojo.html.classMatchType={ContainsAll:0,ContainsAny:1,IsOnly:2};
dojo.html.getElementsByClass=function(_3dd,_3de,_3df,_3e0){
_3de=dojo.byId(_3de)||document;
var _3e1=_3dd.split(/\s+/g);
var _3e2=[];
if(_3e0!=1&&_3e0!=2){
_3e0=0;
}
var _3e3=new RegExp("(\\s|^)(("+_3e1.join(")|(")+"))(\\s|$)");
if(!_3df){
_3df="*";
}
var _3e4=_3de.getElementsByTagName(_3df);
var node,i=0;
outer:
while(node=_3e4[i++]){
var _3e6=dojo.html.getClasses(node);
if(_3e6.length==0){
continue outer;
}
var _3e7=0;
for(var j=0;j<_3e6.length;j++){
if(_3e3.test(_3e6[j])){
if(_3e0==dojo.html.classMatchType.ContainsAny){
_3e2.push(node);
continue outer;
}else{
_3e7++;
}
}else{
if(_3e0==dojo.html.classMatchType.IsOnly){
continue outer;
}
}
}
if(_3e7==_3e1.length){
if(_3e0==dojo.html.classMatchType.IsOnly&&_3e7==_3e6.length){
_3e2.push(node);
}else{
if(_3e0==dojo.html.classMatchType.ContainsAll){
_3e2.push(node);
}
}
}
}
return _3e2;
};
dojo.html.getElementsByClassName=dojo.html.getElementsByClass;
dojo.html.getCursorPosition=function(e){
e=e||window.event;
var _3ea={x:0,y:0};
if(e.pageX||e.pageY){
_3ea.x=e.pageX;
_3ea.y=e.pageY;
}else{
var de=document.documentElement;
var db=document.body;
_3ea.x=e.clientX+((de||db)["scrollLeft"])-((de||db)["clientLeft"]);
_3ea.y=e.clientY+((de||db)["scrollTop"])-((de||db)["clientTop"]);
}
return _3ea;
};
dojo.html.overElement=function(_3ed,e){
_3ed=dojo.byId(_3ed);
var _3ef=dojo.html.getCursorPosition(e);
with(dojo.html){
var top=getAbsoluteY(_3ed,true);
var _3f1=top+getInnerHeight(_3ed);
var left=getAbsoluteX(_3ed,true);
var _3f3=left+getInnerWidth(_3ed);
}
return (_3ef.x>=left&&_3ef.x<=_3f3&&_3ef.y>=top&&_3ef.y<=_3f1);
};
dojo.html.setActiveStyleSheet=function(_3f4){
var i=0,a,els=document.getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("title")){
a.disabled=true;
if(a.getAttribute("title")==_3f4){
a.disabled=false;
}
}
}
};
dojo.html.getActiveStyleSheet=function(){
var i=0,a,els=document.getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("title")&&!a.disabled){
return a.getAttribute("title");
}
}
return null;
};
dojo.html.getPreferredStyleSheet=function(){
var i=0,a,els=document.getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("rel").indexOf("alt")==-1&&a.getAttribute("title")){
return a.getAttribute("title");
}
}
return null;
};
dojo.html.body=function(){
dojo.deprecated("dojo.html.body","use document.body instead");
return document.body||document.getElementsByTagName("body")[0];
};
dojo.html.isTag=function(node){
node=dojo.byId(node);
if(node&&node.tagName){
var arr=dojo.lang.map(dojo.lang.toArray(arguments,1),function(a){
return String(a).toLowerCase();
});
return arr[dojo.lang.find(node.tagName.toLowerCase(),arr)]||"";
}
return "";
};
dojo.html._callExtrasDeprecated=function(_3fb,args){
var _3fd="dojo.html.extras";
dojo.deprecated("dojo.html."+_3fb+" has been moved to "+_3fd);
dojo["require"](_3fd);
return dojo.html[_3fb].apply(dojo.html,args);
};
dojo.html.createNodesFromText=function(){
return dojo.html._callExtrasDeprecated("createNodesFromText",arguments);
};
dojo.html.gravity=function(){
return dojo.html._callExtrasDeprecated("gravity",arguments);
};
dojo.html.placeOnScreen=function(){
return dojo.html._callExtrasDeprecated("placeOnScreen",arguments);
};
dojo.html.placeOnScreenPoint=function(){
return dojo.html._callExtrasDeprecated("placeOnScreenPoint",arguments);
};
dojo.html.renderedTextContent=function(){
return dojo.html._callExtrasDeprecated("renderedTextContent",arguments);
};
dojo.html.BackgroundIframe=function(){
return dojo.html._callExtrasDeprecated("BackgroundIframe",arguments);
};
dojo.provide("dojo.lfx.html");
dojo.require("dojo.lfx.Animation");
dojo.require("dojo.html");
dojo.require("dojo.event");
dojo.require("dojo.lang.func");
dojo.lfx.html._byId=function(_3fe){
if(dojo.lang.isArrayLike(_3fe)){
if(!_3fe.alreadyChecked){
var n=[];
dojo.lang.forEach(_3fe,function(node){
n.push(dojo.byId(node));
});
n.alreadyChecked=true;
return n;
}else{
return _3fe;
}
}else{
return [dojo.byId(_3fe)];
}
};
dojo.lfx.html.propertyAnimation=function(_401,_402,_403,_404){
_401=dojo.lfx.html._byId(_401);
if(_401.length==1){
dojo.lang.forEach(_402,function(prop){
if(typeof prop["start"]=="undefined"){
prop.start=parseInt(dojo.style.getComputedStyle(_401[0],prop.property));
if(isNaN(prop.start)&&(prop.property=="opacity")){
prop.start=1;
}
}
});
}
var _406=function(_407){
var _408=new Array(_407.length);
for(var i=0;i<_407.length;i++){
_408[i]=Math.round(_407[i]);
}
return _408;
};
var _40a=function(n,_40c){
n=dojo.byId(n);
if(!n||!n.style){
return;
}
for(s in _40c){
if(s=="opacity"){
dojo.style.setOpacity(n,_40c[s]);
}else{
n.style[dojo.style.toCamelCase(s)]=_40c[s];
}
}
};
var _40d=function(_40e){
this._properties=_40e;
this.diffs=new Array(_40e.length);
dojo.lang.forEach(_40e,function(prop,i){
if(dojo.lang.isArray(prop.start)){
this.diffs[i]=null;
}else{
this.diffs[i]=prop.end-prop.start;
}
},this);
this.getValue=function(n){
var ret={};
dojo.lang.forEach(this._properties,function(prop,i){
var _415=null;
if(dojo.lang.isArray(prop.start)){
_415=(prop.units||"rgb")+"(";
for(var j=0;j<prop.start.length;j++){
_415+=Math.round(((prop.end[j]-prop.start[j])*n)+prop.start[j])+(j<prop.start.length-1?",":"");
}
_415+=")";
}else{
_415=((this.diffs[i])*n)+prop.start+(prop.property!="opacity"?prop.units||"px":"");
}
ret[prop.property]=_415;
},this);
return ret;
};
};
var anim=new dojo.lfx.Animation(_403,new _40d(_402),_404);
dojo.event.connect(anim,"onAnimate",function(_418){
dojo.lang.forEach(_401,function(node){
_40a(node,_418);
});
});
return anim;
};
dojo.lfx.html._makeFadeable=function(_41a){
var _41b=function(node){
if(dojo.render.html.ie){
if((node.style.zoom.length==0)&&(dojo.style.getStyle(node,"zoom")=="normal")){
node.style.zoom="1";
}
if((node.style.width.length==0)&&(dojo.style.getStyle(node,"width")=="auto")){
node.style.width="auto";
}
}
};
if(dojo.lang.isArrayLike(_41a)){
dojo.lang.forEach(_41a,_41b);
}else{
_41b(_41a);
}
};
dojo.lfx.html.fadeIn=function(_41d,_41e,_41f,_420){
_41d=dojo.lfx.html._byId(_41d);
dojo.lfx.html._makeFadeable(_41d);
var anim=dojo.lfx.propertyAnimation(_41d,[{property:"opacity",start:dojo.style.getOpacity(_41d[0]),end:1}],_41e,_41f);
if(_420){
dojo.event.connect(anim,"onEnd",function(){
_420(_41d,anim);
});
}
return anim;
};
dojo.lfx.html.fadeOut=function(_422,_423,_424,_425){
_422=dojo.lfx.html._byId(_422);
dojo.lfx.html._makeFadeable(_422);
var anim=dojo.lfx.propertyAnimation(_422,[{property:"opacity",start:dojo.style.getOpacity(_422[0]),end:0}],_423,_424);
if(_425){
dojo.event.connect(anim,"onEnd",function(){
_425(_422,anim);
});
}
return anim;
};
dojo.lfx.html.fadeShow=function(_427,_428,_429,_42a){
var anim=dojo.lfx.html.fadeIn(_427,_428,_429,_42a);
dojo.event.connect(anim,"beforeBegin",function(){
if(dojo.lang.isArrayLike(_427)){
dojo.lang.forEach(_427,dojo.style.show);
}else{
dojo.style.show(_427);
}
});
return anim;
};
dojo.lfx.html.fadeHide=function(_42c,_42d,_42e,_42f){
var anim=dojo.lfx.html.fadeOut(_42c,_42d,_42e,function(){
if(dojo.lang.isArrayLike(_42c)){
dojo.lang.forEach(_42c,dojo.style.hide);
}else{
dojo.style.hide(_42c);
}
if(_42f){
_42f(_42c,anim);
}
});
return anim;
};
dojo.lfx.html.wipeIn=function(_431,_432,_433,_434){
_431=dojo.lfx.html._byId(_431);
var _435=[];
var init=function(node,_438){
if(_438=="visible"){
node.style.overflow="hidden";
}
dojo.style.show(node);
node.style.height=0;
};
dojo.lang.forEach(_431,function(node){
var _43a=dojo.style.getStyle(node,"overflow");
var _43b=function(){
init(node,_43a);
};
_43b();
var anim=dojo.lfx.propertyAnimation(node,[{property:"height",start:0,end:node.scrollHeight}],_432,_433);
dojo.event.connect(anim,"beforeBegin",_43b);
dojo.event.connect(anim,"onEnd",function(){
node.style.overflow=_43a;
node.style.height="auto";
if(_434){
_434(node,anim);
}
});
_435.push(anim);
});
if(_431.length>1){
return dojo.lfx.combine(_435);
}else{
return _435[0];
}
};
dojo.lfx.html.wipeOut=function(_43d,_43e,_43f,_440){
_43d=dojo.lfx.html._byId(_43d);
var _441=[];
var init=function(node,_444){
dojo.style.show(node);
if(_444=="visible"){
node.style.overflow="hidden";
}
};
dojo.lang.forEach(_43d,function(node){
var _446=dojo.style.getStyle(node,"overflow");
var _447=function(){
init(node,_446);
};
_447();
var anim=dojo.lfx.propertyAnimation(node,[{property:"height",start:node.offsetHeight,end:0}],_43e,_43f);
dojo.event.connect(anim,"beforeBegin",_447);
dojo.event.connect(anim,"onEnd",function(){
dojo.style.hide(node);
node.style.overflow=_446;
if(_440){
_440(node,anim);
}
});
_441.push(anim);
});
if(_43d.length>1){
return dojo.lfx.combine(_441);
}else{
return _441[0];
}
};
dojo.lfx.html.slideTo=function(_449,_44a,_44b,_44c,_44d){
_449=dojo.lfx.html._byId(_449);
var _44e=[];
dojo.lang.forEach(_449,function(node){
var top=null;
var left=null;
var pos=null;
var init=(function(){
var _454=node;
return function(){
top=node.offsetTop;
left=node.offsetLeft;
pos=dojo.style.getComputedStyle(node,"position");
if(pos=="relative"||pos=="static"){
top=parseInt(dojo.style.getComputedStyle(node,"top"))||0;
left=parseInt(dojo.style.getComputedStyle(node,"left"))||0;
}
};
})();
init();
var anim=dojo.lfx.propertyAnimation(node,[{property:"top",start:top,end:_44a[0]},{property:"left",start:left,end:_44a[1]}],_44b,_44c);
dojo.event.connect(anim,"beforeBegin",init);
if(_44d){
dojo.event.connect(anim,"onEnd",function(){
_44d(node,anim);
});
}
_44e.push(anim);
});
if(_449.length>1){
return dojo.lfx.combine(_44e);
}else{
return _44e[0];
}
};
dojo.lfx.html.explode=function(_456,_457,_458,_459,_45a){
var _45b=dojo.style.toCoordinateArray(_456);
var _45c=document.createElement("div");
with(_45c.style){
position="absolute";
border="1px solid black";
display="none";
}
document.body.appendChild(_45c);
_457=dojo.byId(_457);
with(_457.style){
visibility="hidden";
display="block";
}
var _45d=dojo.style.toCoordinateArray(_457);
with(_457.style){
display="none";
visibility="visible";
}
var anim=new dojo.lfx.Animation({beforeBegin:function(){
dojo.style.show(_45c);
},onAnimate:function(_45f){
with(_45c.style){
left=_45f[0]+"px";
top=_45f[1]+"px";
width=_45f[2]+"px";
height=_45f[3]+"px";
}
},onEnd:function(){
dojo.style.show(_457);
_45c.parentNode.removeChild(_45c);
}},_458,new dojo.lfx.Line(_45b,_45d),_459);
if(_45a){
dojo.event.connect(anim,"onEnd",function(){
_45a(_457,anim);
});
}
return anim;
};
dojo.lfx.html.implode=function(_460,end,_462,_463,_464){
var _465=dojo.style.toCoordinateArray(_460);
var _466=dojo.style.toCoordinateArray(end);
_460=dojo.byId(_460);
var _467=document.createElement("div");
with(_467.style){
position="absolute";
border="1px solid black";
display="none";
}
document.body.appendChild(_467);
var anim=new dojo.lfx.Animation({beforeBegin:function(){
dojo.style.hide(_460);
dojo.style.show(_467);
},onAnimate:function(_469){
with(_467.style){
left=_469[0]+"px";
top=_469[1]+"px";
width=_469[2]+"px";
height=_469[3]+"px";
}
},onEnd:function(){
_467.parentNode.removeChild(_467);
}},_462,new dojo.lfx.Line(_465,_466),_463);
if(_464){
dojo.event.connect(anim,"onEnd",function(){
_464(_460,anim);
});
}
return anim;
};
dojo.lfx.html.highlight=function(_46a,_46b,_46c,_46d,_46e){
_46a=dojo.lfx.html._byId(_46a);
var _46f=[];
dojo.lang.forEach(_46a,function(node){
var _471=dojo.style.getBackgroundColor(node);
var bg=dojo.style.getStyle(node,"background-color").toLowerCase();
var _473=(bg=="transparent"||bg=="rgba(0, 0, 0, 0)");
while(_471.length>3){
_471.pop();
}
var rgb=new dojo.graphics.color.Color(_46b).toRgb();
var _475=new dojo.graphics.color.Color(_471).toRgb();
var anim=dojo.lfx.propertyAnimation(node,[{property:"background-color",start:rgb,end:_475}],_46c,_46d);
dojo.event.connect(anim,"beforeBegin",function(){
node.style.backgroundColor="rgb("+rgb.join(",")+")";
});
dojo.event.connect(anim,"onEnd",function(){
if(_473){
node.style.backgroundColor="transparent";
}
if(_46e){
_46e(node,anim);
}
});
_46f.push(anim);
});
if(_46a.length>1){
return dojo.lfx.combine(_46f);
}else{
return _46f[0];
}
};
dojo.lfx.html.unhighlight=function(_477,_478,_479,_47a,_47b){
_477=dojo.lfx.html._byId(_477);
var _47c=[];
dojo.lang.forEach(_477,function(node){
var _47e=new dojo.graphics.color.Color(dojo.style.getBackgroundColor(node)).toRgb();
var rgb=new dojo.graphics.color.Color(_478).toRgb();
var anim=dojo.lfx.propertyAnimation(node,[{property:"background-color",start:_47e,end:rgb}],_479,_47a);
dojo.event.connect(anim,"beforeBegin",function(){
node.style.backgroundColor="rgb("+_47e.join(",")+")";
});
if(_47b){
dojo.event.connect(anim,"onEnd",function(){
_47b(node,anim);
});
}
_47c.push(anim);
});
if(_477.length>1){
return dojo.lfx.combine(_47c);
}else{
return _47c[0];
}
};
dojo.lang.mixin(dojo.lfx,dojo.lfx.html);
dojo.kwCompoundRequire({browser:["dojo.lfx.html"],dashboard:["dojo.lfx.html"]});
dojo.provide("dojo.lfx.*");

