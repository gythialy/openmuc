(function () {
	
	var injectParams = ['$scope', '$alert', '$state', '$translate', 'DriversService'];
	
	var DriversController = function($scope, $alert, $state, $translate, DriversService) {
		
		$translate('DRIVER_DELETED_SUCCESSFULLY').then(function(text) {
			$scope.driverOKText = text;
		});

        $translate('DELETE_CONFIRM_MESSAGE').then(function(confirmMessage) {
            $scope.confirmMessage = confirmMessage;
        });

		$scope.drivers = [];
		
		DriversService.getDrivers().then(function(drivers){
			$scope.drivers = drivers;
		});
		
		$scope.deleteDriver = function(id) {
            if (confirm($scope.confirmMessage + " " + id + "?") == true) {
				DriversService.destroy(id).then(function(data) {
					$alert({content: $scope.driverOKText, type: 'success'});
				});

				DriversService.getDrivers().then(function(drivers){
					$scope.drivers = drivers;
				});
			}
		};
		
	};

	DriversController.$inject = injectParams;

	angular.module('openmuc.drivers').controller('DriversController', DriversController);
	
}());