(function(){

	var injectParams = ['$ocLazyLoad', 'AvailableAppsService'];

	var AppsDependenciesService = function($ocLazyLoad, AvailableAppsService) {

		this.loadDependencies = function() {
			var files = [];

			return AvailableAppsService.getAll().then(function(response){
				angular.forEach(response, function(value, index) {
					files.push(value.alias + '/js/app.js');
					files.push(value.alias + '/js/app.routes.js');
				});

	            return $ocLazyLoad.load(
	                    {
	                        name: "openmuc",
	                        files: files
	                    }
	            );

			}, function(data) {
			});

    	}

	};

    AppsDependenciesService.$inject = injectParams;

	angular.module('openmuc.common').service('AppsDependenciesService', AppsDependenciesService);

})();
