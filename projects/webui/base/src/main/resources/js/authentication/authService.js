(function(){
	
	var injectParams = ['$rootScope', '$http', '$state', '$cookieStore', '$state', 'SETTINGS'];
	
	var AuthService = function($rootScope, $http, $state, $cookieStore, $state, SETTINGS) {
		
		this.login = function(credentials) {

    		var req = {
            		method: 'POST',
            		url: SETTINGS.LOGIN_URL,
            		data: $.param({user: credentials.user, pwd: credentials.pwd}),
            		headers: {
            			'Content-Type': 'application/x-www-form-urlencoded', 
            		},
            	};

    		return $http(req).then(function(response) {
				return response.data;
			});
		};
		
		this.isLoggedIn = function() {
			if ($cookieStore.get('user')) {
				return true;
			} else {
				return false;
			}
		};
		
		this.currentUsername = function() {
			return $cookieStore.get('user').user;
		}
		
		this.setCurrentUser = function (user) {
			$cookieStore.put('user', user);
		};
		
		this.redirectToLogin = function() {
			$state.go('home');
		};
		
		this.logout = function() {
			$rootScope.currentUser = null;
			$cookieStore.remove('user');
		};
		
	};

	AuthService.$inject = injectParams;

	angular.module('openmuc.auth').service('AuthService', AuthService);    
	
})();
