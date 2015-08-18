(function(){
	
	var app = angular.module('openmuc.constants');
	
	app.constant('SETTINGS', {
		API_URL: 				'/rest/',
		DRIVERS_URL:			'drivers/',
		CHANNELS_URL: 			'channels/',
		CHANNELS_HISTORY_URL: 	'/history',
		DEVICES_URL:			'devices/',
		SCAN_URL:				'scan',
		APPS_URL: 				'/applications',
		CONFIGS_URL: 			'/configs',
		LOGIN_URL:				'/login',
		USERS_URL: 				'users/',
		MEDIA_CONFIG_URL:		'/conf/webui/mediaviewer',
		DATAPLOTTER_CONFIG_URL:	'/conf/webui/dataplotter'
	});

})();