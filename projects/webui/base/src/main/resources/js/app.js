(function(){
	
	var app = angular.module('openmuc', ['openmuc.auth', 
	                                     'openmuc.common',
	                                     'openmuc.constants',
	                                     'openmuc.dashboard',
	                                     'openmuc.filters', 
	                                     'openmuc.i18n',
	                                     'openmuc.sessions',
	                                     'ngCookies', 
	                                     'mgcrea.ngStrap', 
	                                     'ngAnimate', 
	                                     'validation.match',
	                                     'ui.router',
	                                     'oc.lazyLoad']);

	angular.module('openmuc.auth', []);
	angular.module('openmuc.common', []);
	angular.module('openmuc.constants', []);
	angular.module('openmuc.dashboard', []);
	angular.module('openmuc.filters', []);
	angular.module('openmuc.sessions', []);
	
	// TODO: Move me to somewhere else
	
	app.config(function($alertProvider) {
		angular.extend($alertProvider.defaults, {
			placement: 'top-right',
		    animation: 'am-fade-and-slide-top',
		    duration: 5,
		    keyboard: true,
		    dismissable: true,
		    show: true
		});
	});
		
})();