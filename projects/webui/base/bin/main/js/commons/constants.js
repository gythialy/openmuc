(function(){

	var app = angular.module('openmuc.constants');

	app.constant('SETTINGS', {
		API_URL: 				'rest/',
		DRIVERS_URL:			'drivers/',
		CHANNELS_URL: 			'channels/',
		CHANNELS_HISTORY_URL: 	'/history',
		DEVICES_URL:			'devices/',
		SCAN_URL:				'scan',
		SCAN_INTERRUPT_URL:		'scanInterrupt',
		SCAN_PROGRESS_INFO_URL: 'scanProgressInfo',
		APPS_URL: 				'applications',
		CONFIGS_URL: 			'/configs',
		INFOS_URL: 				'/infos',
		LOGIN_URL:				'login',
		USERS_URL: 				'users/',
		MEDIA_CONFIG_URL:		'conf/webui/mediaviewer',
		DATAPLOTTER_CONFIG_URL:	'conf/webui/dataplotter',
		DRIVER_ID:				'driverId',
		DEVICE_ID:				'deviceId'
	});

})();
