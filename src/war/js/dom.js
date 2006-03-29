/* Copyright (c) 2006 Yahoo! Inc. All rights reserved. */
YAHOO.util.Dom=new function(){this.get=function(el){if(typeof el=='string'){el=document.getElementById(el);}
return el;};this.getStyle=function(el,property){var value=null;var dv=document.defaultView;el=this.get(el);if(property=='opacity'&&el.filters){value=1;try{value=el.filters.item('DXImageTransform.Microsoft.Alpha').opacity/100;}catch(e){try{value=el.filters.item('alpha').opacity/100;}catch(e){}}}
else if(el.style[property]){value=el.style[property];}
else if(el.currentStyle&&el.currentStyle[property]){value=el.currentStyle[property];}
else if(dv&&dv.getComputedStyle)
{var converted='';for(i=0,len=property.length;i<len;++i){if(property.charAt(i)==property.charAt(i).toUpperCase()){converted=converted+'-'+property.charAt(i).toLowerCase();}else{converted=converted+property.charAt(i);}}
if(dv.getComputedStyle(el,'').getPropertyValue(converted)){value=dv.getComputedStyle(el,'').getPropertyValue(converted);}}
return value;};this.setStyle=function(el,property,val){el=this.get(el);switch(property){case'opacity':if(el.filters){el.style.filter='alpha(opacity='+val*100+')';if(!el.currentStyle.hasLayout){el.style.zoom=1;}}else{el.style.opacity=val;el.style['-moz-opacity']=val;el.style['-khtml-opacity']=val;}
break;default:el.style[property]=val;}};this.getXY=function(el){el=this.get(el);if(el.parentNode===null||this.getStyle(el,'display')=='none'){return false;}
var parent=null;var pos=[];var box;if(el.getBoundingClientRect){box=el.getBoundingClientRect();var scrollTop=document.documentElement.scrollTop||document.body.scrollTop;var scrollLeft=document.documentElement.scrollLeft||document.body.scrollLeft;return[box.left+scrollLeft,box.top+scrollTop];}
else if(document.getBoxObjectFor){box=document.getBoxObjectFor(el);pos=[box.x,box.y];}
else{pos=[el.offsetLeft,el.offsetTop];parent=el.offsetParent;if(parent!=el){while(parent){pos[0]+=parent.offsetLeft;pos[1]+=parent.offsetTop;parent=parent.offsetParent;}}
var ua=navigator.userAgent.toLowerCase();if(ua.indexOf('opera')!=-1||(ua.indexOf('safari')!=-1&&this.getStyle(el,'position')=='absolute')){pos[1]-=document.body.offsetTop;}}
if(el.parentNode){parent=el.parentNode;}
else{parent=null;}
while(parent&&parent.tagName!='BODY'&&parent.tagName!='HTML'){pos[0]-=parent.scrollLeft;pos[1]-=parent.scrollTop;if(parent.parentNode){parent=parent.parentNode;}
else{parent=null;}}
return pos;};this.getX=function(el){return this.getXY(el)[0];};this.getY=function(el){return this.getXY(el)[1];};this.setXY=function(el,pos,noRetry){el=this.get(el);var pageXY=YAHOO.util.Dom.getXY(el);if(pageXY===false){return false;}
var delta=[parseInt(YAHOO.util.Dom.getStyle(el,'left'),10),parseInt(YAHOO.util.Dom.getStyle(el,'top'),10)];if(isNaN(delta[0])){delta[0]=0;}
if(isNaN(delta[1])){delta[1]=0;}
if(pos[0]!==null){el.style.left=pos[0]-pageXY[0]+delta[0]+'px';}
if(pos[1]!==null){el.style.top=pos[1]-pageXY[1]+delta[1]+'px';}
var newXY=this.getXY(el);if(!noRetry&&(newXY[0]!=pos[0]||newXY[1]!=pos[1])){this.setXY(el,pos,true);}
return true;};this.setX=function(el,x){return this.setXY(el,[x,null]);};this.setY=function(el,y){return this.setXY(el,[null,y]);};this.getRegion=function(el){el=this.get(el);return new YAHOO.util.Region.getRegion(el);};this.getClientWidth=function(){return(document.documentElement.offsetWidth||document.body.offsetWidth);};this.getClientHeight=function(){return(self.innerHeight||document.documentElement.clientHeight||document.body.clientHeight);};};YAHOO.util.Region=function(t,r,b,l){this.top=t;this.right=r;this.bottom=b;this.left=l;};YAHOO.util.Region.prototype.contains=function(region){return(region.left>=this.left&&region.right<=this.right&&region.top>=this.top&&region.bottom<=this.bottom);};YAHOO.util.Region.prototype.getArea=function(){return((this.bottom-this.top)*(this.right-this.left));};YAHOO.util.Region.prototype.intersect=function(region){var t=Math.max(this.top,region.top);var r=Math.min(this.right,region.right);var b=Math.min(this.bottom,region.bottom);var l=Math.max(this.left,region.left);if(b>=t&&r>=l){return new YAHOO.util.Region(t,r,b,l);}else{return null;}};YAHOO.util.Region.prototype.union=function(region){var t=Math.min(this.top,region.top);var r=Math.max(this.right,region.right);var b=Math.max(this.bottom,region.bottom);var l=Math.min(this.left,region.left);return new YAHOO.util.Region(t,r,b,l);};YAHOO.util.Region.prototype.toString=function(){return("Region {"+"  t: "+this.top+", r: "+this.right+", b: "+this.bottom+", l: "+this.left+"}");}
YAHOO.util.Region.getRegion=function(el){var p=YAHOO.util.Dom.getXY(el);var t=p[1];var r=p[0]+el.offsetWidth;var b=p[1]+el.offsetHeight;var l=p[0];return new YAHOO.util.Region(t,r,b,l);};YAHOO.util.Point=function(x,y){this.x=x;this.y=y;this.top=y;this.right=x;this.bottom=y;this.left=x;};YAHOO.util.Point.prototype=new YAHOO.util.Region();