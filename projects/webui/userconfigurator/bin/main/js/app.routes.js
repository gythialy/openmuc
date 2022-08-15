(function(){

	var app = angular.module('openmuc');

	app.config(['$stateProvider', '$urlRouterProvider',
        function($stateProvider, $urlRouterProvider) {
        	$stateProvider.
	        	state('userconfigurator', {
	        		url: "/users",
	        		templateUrl: "userconfigurator/html/index.html",
	        		requireLogin: true,
	        		resolve: {
	        			users: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.users",
	                                files: ['userconfigurator/js/userconfigurator/usersController.js',
	                                        'userconfigurator/js/userconfigurator/usersService.js']
	                            }
	                        )
	                    }
	        	    }
	        	}).
	        	state('userconfigurator.index', {
	        		url: "/",
	        		templateUrl: "userconfigurator/html/list.html",
	        		controller: 'UsersController',
	        		requireLogin: true,
	        	}).
	        	state('userconfigurator.new', {
	        		url: "/new",
	        		templateUrl: "userconfigurator/html/new.html",
	        		controller: 'UserNewController',
	        		requireLogin: true,
	        		resolve: {
	        			users: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.users",
	                                files: ['userconfigurator/js/userconfigurator/userNewController.js',
	                                        'userconfigurator/js/userconfigurator/usersService.js']
	                            }
	                        )
	                    }
	        	    }
	        	}).
	        	state('userconfigurator.edit', {
	        		url: "/edit/id",
	        		templateUrl: "userconfigurator/html/edit.html",
	        		controller: 'UserEditController',
	        		requireLogin: true,
	        		resolve: {
	        			users: function ($ocLazyLoad) {
	                        return $ocLazyLoad.load(
	                            {
	                                name: "openmuc.users",
	                                files: ['openmuc/js/authentication/authService.js',
	                                        'userconfigurator/js/userconfigurator/userEditController.js',
	                                        'userconfigurator/js/userconfigurator/usersService.js']
	                            }
	                        )
	                    }
	        	    }
	        	})
	}]);

})();
