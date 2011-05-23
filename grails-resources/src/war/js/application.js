(function($) { 
	$('#spinner').ajaxStart(function() {
		$(this).fadeIn();
	}).ajaxStop(function() {
		$(this).fadeOut();
	});
})(jQuery);
