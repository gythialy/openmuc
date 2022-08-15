(function(){

	var app = angular.module('openmuc');

	app.config(['$stateProvider', '$urlRouterProvider',
        function($stateProvider, $urlRouterProvider) {
        	$stateProvider.
	        	state('dataexporter', {
	        		url: '/dataexporter',
				    templateUrl: 'dataexporter/html/index.html',
				    controller: 'DataExporterController',
				    requireLogin: true,
				    resolve: {
	        			openmuc: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.dataexporter",
	                                files: ['openmuc/js/channels/channelsService.js',
	                                        'openmuc/js/channels/channelDataService.js',
	                                        'dataexporter/js/dataexporter/dataExporterController.js',
	                                        'openmuc/js/libs/checklistmodel/checklist-model.min.js',
	                                        'openmuc/js/libs/angularjs/angular-sanitize.min.js',
	                                        'dataexporter/js/libs/angularjs/ng-csv.min.js']
	                            }
	                        )
	                    }
	        	    }
				}).
	        	state('dataexporter.index', {
	        		url: "/",
				    templateUrl: 'dataexporter/html/index.html',
				    controller: 'DataExporterController',
	        		requireLogin: true,
	        	})

	}]);

})();
