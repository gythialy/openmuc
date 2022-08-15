(function(){

	var app = angular.module('openmuc');

	app.config(['$stateProvider', '$urlRouterProvider',
        function($stateProvider, $urlRouterProvider) {

        	$stateProvider.
	        	state('channelconfigurator', {
	        		url: "/channelconfigurator",
					templateUrl: 'channelconfigurator/html/index.html',
	        		requireLogin: true,
	        		resolve: {
	        			drivers: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.drivers",
	                                files: ['channelconfigurator/js/drivers/driversController.js',
	                                        'channelconfigurator/js/drivers/driversService.js',
	                                        'channelconfigurator/js/drivers/driverDataService.js',
	                                        'channelconfigurator/js/drivers/driverEditController.js',
	                                        'channelconfigurator/js/drivers/driverNewController.js',
											'channelconfigurator/js/drivers/driverInfosController.js',
	                                        'channelconfigurator/js/drivers/driverScanController.js',
	                                        'channelconfigurator/js/drivers/driversDirective.js',
	                                        'channelconfigurator/js/channels/channelConfiguratorTabsDirective.js',
	                                        'openmuc/js/libs/checklistmodel/checklist-model.min.js']
	                            }
	                        )
	                    }
	        	    }
	        	}).
	        	state('channelconfigurator.index', {
	        		url: "/",
					templateUrl: 'channelconfigurator/html/drivers/list.html',
					controller: "DriversController",
	        		requireLogin: true,
	        	}).
        		state('channelconfigurator.drivers', {
	        		url: "",
					templateUrl: 'channelconfigurator/html/drivers/index.html',
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.drivers.index', {
	        		url: "/",
					templateUrl: 'channelconfigurator/html/drivers/list.html',
					controller: "DriversController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.drivers.new', {
	        		url: "/drivers/new",
					templateUrl: 'channelconfigurator/html/drivers/new.html',
					controller: "DriverNewController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.drivers.edit', {
	        		url: "/drivers/edit/:id",
					templateUrl: 'channelconfigurator/html/drivers/edit.html',
					controller: "DriverEditController",
	        		requireLogin: true,
	        	}).
                state('channelconfigurator.drivers.infos', {
                    url: "/drivers/infos:id",
                    templateUrl: 'channelconfigurator/html/drivers/infos.html',
                    controller: "DriverInfosController",
                    requireLogin: true,
                    resolve: {
                        drivers: function ($ocLazyLoad) {
                            return $ocLazyLoad.load(
                                {
                                    name: "openmuc.drivers",
                                    files: ['channelconfigurator/js/drivers/driversService.js']
                                }
                            )
                        }
                    }
                }).
	        	state('channelconfigurator.drivers.scan', {
	        		url: "/drivers/scan/:id",
					templateUrl: 'channelconfigurator/html/drivers/scan.html',
					controller: "DriverScanController",
	        		requireLogin: true,
	        		resolve: {
	        			drivers: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.drivers",
	                                files: ['openmuc/js/devices/devicesService.js',
	                                        'openmuc/js/devices/deviceDataService.js',
	                                        'openmuc/js/libs/checklistmodel/checklist-model.min.js']
	                            }
	                        )
	                    }
	        	    }

	        	}).
	        	state('channelconfigurator.devices', {
	        		url: "",
					templateUrl: 'channelconfigurator/html/devices/index.html',
	        		requireLogin: true,
	        		resolve: {
	        			devices: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.devices",
	                                files: ['channelconfigurator/js/drivers/driversService.js',
	                                        'channelconfigurator/js/drivers/driverDataService.js',
	                                        'channelconfigurator/js/devices/devicesController.js',
	                                		'channelconfigurator/js/devices/deviceEditController.js',
	                                		'channelconfigurator/js/devices/deviceNewController.js',
	                                		'channelconfigurator/js/devices/devicesDirective.js',
	                                		'openmuc/js/devices/devicesService.js',
	                                		'openmuc/js/devices/deviceDataService.js',
	                                		'channelconfigurator/js/channels/channelConfiguratorTabsDirective.js']
	                            }
	                        )
	                    }
	        	    }
	        	}).
	        	state('channelconfigurator.devices.index', {
	        		url: "/devices",
					templateUrl: 'channelconfigurator/html/devices/list.html',
					controller: "DevicesController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.devices.new', {
	        		url: "/drivers/:driverId/devices/new",
					templateUrl: 'channelconfigurator/html/devices/new.html',
					controller: "DeviceNewController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.devices.edit', {
	        		url: "/devices/edit/:deviceId?driverId",
					templateUrl: 'channelconfigurator/html/devices/edit.html',
					controller: "DeviceEditController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.devices.scan', {
	        		url: "/devices/scan/:deviceId",
					templateUrl: 'channelconfigurator/html/devices/scan.html',
					controller: "DeviceScanController",
	        		requireLogin: true,
	        		resolve: {
	        			drivers: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.devices",
	                                files: ['channelconfigurator/js/devices/deviceScanController.js',
	                                        'openmuc/js/channels/channelsService.js',
	                                        'openmuc/js/channels/channelDataService.js',
	                                        'openmuc/js/libs/checklistmodel/checklist-model.min.js']
	                            }
	                        )
	                    }
	        	    }

	        	}).
	        	state('channelconfigurator.channels', {
	        		url: "",
					templateUrl: 'channelconfigurator/html/channels/index.html',
	        		requireLogin: true,
	        		resolve: {
	        			channels: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.channels",
	                                files: ['channelconfigurator/js/drivers/driversService.js',
	                                        'channelconfigurator/js/drivers/driverDataService.js',
	                                        'openmuc/js/devices/devicesService.js',
	                                        'openmuc/js/devices/deviceDataService.js',
	                                        'channelconfigurator/js/channels/channelsController.js',
	                                		'channelconfigurator/js/channels/channelsDirective.js',
	                                		'openmuc/js/channels/channelsService.js',
	                                		'openmuc/js/channels/channelDataService.js',
	                                		'channelconfigurator/js/channels/channelEditController.js',
	                                		'channelconfigurator/js/channels/channelNewController.js',
	                                		'channelconfigurator/js/channels/channelConfiguratorTabsDirective.js']
	                            }
	                        )
	                    }
	        	    }
	        	}).
	        	state('channelconfigurator.channels.index', {
	        		url: "/channels",
					templateUrl: 'channelconfigurator/html/channels/list.html',
					controller: "ChannelsController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.channels.new', {
	        		url: "/devices/:deviceId/channels/new",
					templateUrl: 'channelconfigurator/html/channels/new.html',
					controller: "ChannelNewController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.channels.edit', {
	        		url: "/channels/edit/:id",
					templateUrl: 'channelconfigurator/html/channels/edit.html',
					controller: "ChannelEditController",
	        		requireLogin: true,
	        	}).
	        	state('channelconfigurator.options', {
	        		url: "/options",
					templateUrl: 'channelconfigurator/html/options/index.html',
					controller: "OptionsController",
	        		requireLogin: true,
	        		resolve: {
	        			options: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.options",
	                                files: ['channelconfigurator/js/options/optionsController.js',
	                                        'channelconfigurator/js/channels/channelConfiguratorTabsDirective.js']
	                            }
	                        )
	                    }
	        	    }
	        	})

	}]);

})();
