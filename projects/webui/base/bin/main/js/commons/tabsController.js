(function(){

	var injectParams = ['$scope', '$location', '$rootScope'];

	var TabsController = function($scope, $location, $rootScope) {
		$scope.isTabActive = function(url) {
			return $location.url().search(url) > -1;
		};

		$scope.isDriversPage = function() {
			return $location.url() === "/channelconfigurator/";
		};

		$scope.isDriversNotPage = function() {
			return $location.url() !== "/channelconfigurator/";
		};

		$scope.isDevicesPage = function() {
			return $location.url() === "/channelconfigurator/devices";
		};

		$scope.isChannelsPage = function() {
			return $location.url() === "/channelconfigurator/channels";
		};

		$scope.isOptionsPage = function() {
			return $location.url() === "/channelconfigurator/options";
		};

		$scope.isDriversEditPage = function() {
			return $location.url().search('/drivers/edit') > -1;
		};

		$scope.isDevicesEditPage = function() {
			return $location.url().search('/devices/edit') > -1;
		};

		$scope.isChannelsEditPage = function() {
			return $location.url().search('/channels/edit') > -1;
		};

		$scope.isDriversInfosPage = function() {
			return $location.url().search('/drivers/infos') > -1;
		};

		$scope.isDriversNewPage = function() {
			return $location.url().search('/drivers/new') > -1;
		};

		$scope.isDevicesNewPage = function() {
			return $location.url().search('/devices/new') > -1;
		};

		$scope.isChannelsNewPage = function() {
			return $location.url().search('/channels/new') > -1;
		};

		$scope.isDriversScanPage = function() {
			return $location.url().search('/drivers/scan') > -1;
		};

		$scope.isDevicesScanPage = function() {
			return $location.url().search('/devices/scan') > -1;
		}

		$scope.isDataPlotterPage = function() {
			return $location.url() === '/dataplotter/data/';
		};

		$scope.isLivePlotterPage = function() {
			return $location.url() === '/dataplotter/live/';
		};

		$scope.isPlotterPageActive = function(type, name) {
			if ($location.url() === '/dataplotter/'+type+"/"+encodeURIComponent(name)) {
				return true;
			} else {
				return false;
			}
		};

		$rootScope.activeTabIndex;

		$scope.setTabIndex = function(index){
			$rootScope.activeTabIndex = index;
		};

		$scope.revertSelected = function(){
			$rootScope.selectedChannels = [];
		};
	};

	TabsController.$inject = injectParams;

	angular.module('openmuc.common').controller('TabsController', TabsController);

})();
