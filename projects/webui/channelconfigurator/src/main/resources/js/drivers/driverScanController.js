(function(){

	var injectParams = ['$scope', '$state', '$stateParams', '$translate', 'notify', 'DriversService', 'DevicesService'];

	var DriverScanController = function($scope, $state, $stateParams, $translate, notify, DriversService, DevicesService) {

		$translate('DRIVER_SCAN_DEVICE_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});

		$translate('DRIVER_SCAN_DEVICE_CREATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});

		$translate('DRIVER_SCAN_NOT_SUPPORTED').then(function(text) {
			$scope.deviceWarningrText = text;
		});

        $translate('DRIVER_SCAN_NOT_INTERRUPTED').then(function(text) {
            $scope.deviceWarningrText = text;
        });

		$scope.driver = DriversService.getDriver($stateParams.id);
		$scope.devices = [];
		$scope.selectedDevices = [];
		$scope.settings = "";
        $scope.scanProgress = -1;
        $scope.scanInterrupted = false;
		$scope.deviceScanSettingsSyntax = "";
		$scope.scanError = "";

		$scope.getDeviceScanSettingsSyntax = function() {
			DriversService.getInfos($scope.driver.id).then(function(driverInfo) {
				$scope.deviceScanSettingsSyntax = driverInfo.deviceScanSettingsSyntax;
			});
		};

		$scope.scanDriver = function() {
            progressClear();
            progress();
            $scope.scanInterrupted = false;
			$scope.scanDriverForm.submitted = true;
			DriversService.scan($scope.driver, $scope.settings).then(function(response) {
				$scope.devices = [];
    			$.each(response.devices, function(i, device) {
    				$scope.devices.push({configs: device});
    			});

                $scope.scanProgress = 100; // kills scanProgress intervall
				$scope.scanDriverForm.submitted = false;
			}, function(error) {
				notify({message: $scope.deviceWarningrText, position: "right", classes: "alert-warning"});
				return $state.go('channelconfigurator.drivers.index');
			});
		};

        $scope.interruptScan = function() {
            DriversService.scanInterrupt($scope.driver).then(function(response){
            }, function(error) {
				notify({message: $scope.deviceErrorText, position: "right", classes: "alert-warning"});
            });
        };

		$scope.addDevices = function() {
			$.each($scope.selectedDevices, function(i, d) {
				var device = {driver: $scope.driver.id, configs: d.configs};
				DevicesService.create(device).then(function(response){
					notify({message: $scope.deviceOKText, position: "right", classes: "alert-success"});
				}, function(error) {
					notify({message: $scope.deviceErrorText, position: "right", classes: "alert-warning"});
				});
			});

			return $state.go('channelconfigurator.devices.index');
		};

		$scope.checkAll = function() {
            var elements = document.getElementsByName('checkboxes');

			if ($scope.master) {
                angular.forEach(elements, function(value, key) {
                    value.checked = false;
                });
				$scope.selectedDevices.length = 0;
			}
			else {
				angular.forEach(elements, function(value, key) {
                    value.checked = true;
                    $scope.selectedDevices[key] = $scope.devices[key];
                });
			}
		};

        var progress = function() {
                var elem = document.getElementById("progressBarForeground");
                var id = setInterval(frame, 1500);
                function frame() {
                    DriversService.scanProgressInfo($scope.driver).then(function (response) {
						var scanInfo = response.scanProgressInfo;
                        $scope.scanProgress = scanInfo.scanProgress;

						if (scanInfo.scanError || scanInfo.isScanInterrupted) {
                            $scope.scanInterrupted = true;
							$scope.scanProgress = 100;
							$scope.scanDriverForm.submitted = false;
							$scope.scanError = scanInfo.scanError;
						}
                    }, function (error) {
                    });
                    if ($scope.scanProgress >= 0) {
                        if ($scope.scanProgress === 100) {
								clearInterval(id);
                        } else {
                            elem.style.width = $scope.scanProgress + '%';
                            document.getElementById("progressBarLabel").innerHTML = $scope.scanProgress * 1 + '%';
                    	}
                    }
                }
            };

		var progressClear = function() {
            if ($scope.scanProgress > 0) {
                $scope.scanProgress = 0;
                var elem = document.getElementById("progressBarForeground");
                elem.style.width = $scope.scanProgress + '%';
                document.getElementById("progressBarLabel").innerHTML = $scope.scanProgress * 1 + '%';
            }
		}
	};

	DriverScanController.$inject = injectParams;

	angular.module('openmuc.drivers').controller('DriverScanController', DriverScanController);

})();
