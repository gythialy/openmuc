(function(){

	var injectParams = ['$scope', '$rootScope', '$stateParams', '$state', '$translate', 'notify', 'ChannelsService', 'DriversService'];

	var ChannelEditController = function($scope, $rootScope, $stateParams, $state, $translate, notify, ChannelsService, DriversService) {

		$rootScope.selectedChannels;
		$scope.driverInfo = {};
		var channelOKText;
		var byteArrayWarningText;
		var stringWarningText;

		$translate('DRIVER_INFO_FAILED').then(function (text) {
            deviceWarningrText = text;
		});
		$translate('CHANNEL_UPDATED_SUCCESSFULLY').then(function (text) {
            channelOKText = text;
		});
		$translate('CHANNEL_UPDATED_ERROR').then(function (text) {
            channelErrorText = text;
		});
		$translate('VALUE_TYPE_LENGTH_BYTE_ARRAY').then(function (text) {
            byteArrayWarningText = text;
		});
		$translate('VALUE_TYPE_LENGTH_STRING').then(function (text) {
            stringWarningText = text;
		});

		var channelId = $stateParams.id;
		if (channelId) {
			$scope.channel = ChannelsService.getChannel(channelId);
			$scope.channelId = channelId;
		} else {
			$scope.channel = [];
		}

		$scope.compareChannels = function(field) {
			if ($rootScope.selectedChannels.length > 1){
				var value = true;
				$rootScope.selectedChannels.forEach (function(channel) {
					var position = $rootScope.selectedChannels.indexOf(channel) + 1;
					if (position < $rootScope.selectedChannels.length) {
						if (channel.data[field] !== $rootScope.selectedChannels[position].data[field]) {
							value = false;
						}
					}	
				});
				return value;
			}
			else {
				return true;
			}
		};

		$scope.valueTypeCheck = function() {
			if($rootScope.selectedChannels.length > 1){
				var valueTypes = [];
				var strOrByte;
				var notStrOrByte;
				$rootScope.selectedChannels.forEach (function(channel) {
					if (typeof channel.data.valueType === "undefined"){
						if(valueTypes.indexOf("DOUBLE") == -1) {
							valueTypes.push("DOUBLE");
						}
					}
					else {
						if(valueTypes.indexOf(channel.data.valueType) == -1) {
							valueTypes.push(channel.data.valueType);
						}
					}
				});
				valueTypes.forEach(function(type){
					if (type === "BYTE_ARRAY" || type === "STRING"){
						strOrByte = true;
					}
					else {
						notStrOrByte = true;
					}
				});
				if (strOrByte === true && notStrOrByte === true){
					return true
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}

		$scope.checkTypeLength = function() {
			if($rootScope.selectedChannels.length > 1){
				var changedProperties = [];
				var valuetypeLengthInvalid = false;
				for (var property in $scope.channel.configs){
					if($scope.channel.configs[property] !== $rootScope.selectedChannels[0].data[property]){
						changedProperties.push(property);
					}
				}
				$rootScope.selectedChannels.forEach (function(channel) {
					changedProperties.forEach (function(property) {
						channel.data[property] = $scope.channel.configs[property]
					});
					var updateChannel = {};
					updateChannel.id = channel.id;
					updateChannel.configs = channel.data;
					if (updateChannel.configs.valueType == "BYTE_ARRAY" && 
						(typeof updateChannel.configs.valueTypeLength === "undefined" || updateChannel.configs.valueTypeLength == 0))
					{
						valuetypeLengthInvalid = true;	
					}
					else if (updateChannel.configs.valueType == "STRING" && 
						(typeof updateChannel.configs.valueTypeLength === "undefined" || updateChannel.configs.valueTypeLength == 0))
					{
						valuetypeLengthInvalid = true;	
					}
				});
				return valuetypeLengthInvalid;
			}
			else{
				if ($scope.channel.configs.valueType == "BYTE_ARRAY" && 
					(typeof $scope.channel.configs.valueTypeLength === "undefined" || $scope.channel.configs.valueTypeLength == 0))
				{
				return true;	
				}
				else if ($scope.channel.configs.valueType == "STRING" && 
					(typeof $scope.channel.configs.valueTypeLength === "undefined" || $scope.channel.configs.valueTypeLength == 0))
				{
					return true;	
				}
				else{
					return false;
				}
			}
		};

		$scope.saveChannel = function() {
			if ($scope.channelForm.$valid) {
				if($rootScope.selectedChannels.length > 1){
					var changedProperties = [];
					for (var property in $scope.channel.configs){
						if($scope.channel.configs[property] !== $rootScope.selectedChannels[0].data[property]){
							changedProperties.push(property);
						}
					}
					$rootScope.selectedChannels.forEach (function(channel) {
						changedProperties.forEach (function(property) {
							channel.data[property] = $scope.channel.configs[property]
						});
						var updateChannel = {};
						updateChannel.id = channel.id;
						updateChannel.configs = channel.data;
						ChannelsService.update(updateChannel).then(function(resp){
							notify({message: channelOKText, position: "right", classes: "alert-success"});
							return $state.go('channelconfigurator.channels.index');
						}, function(error) {
							notify({message: channelErrorText, position: "right", classes: "alert-warning"});
							return $state.go('channelconfigurator.channels.index');
						});
					});
				}
				else{
					ChannelsService.update($scope.channel).then(function(resp){
						notify({message: channelOKText, position: "right", classes: "alert-success"});
						return $state.go('channelconfigurator.channels.index');
					}, function(error) {
						notify({message: channelErrorText, position: "right", classes: "alert-warning"});
						return $state.go('channelconfigurator.channels.index');
					});
				}	
			} else {
				$scope.channelForm.submitted = true;
			}
		};

		$scope.getDriverInfo = function() {
			var channelId = $stateParams.id;

			if (channelId) {
                ChannelsService.getChannelDriverId(channelId).then(function(driverId) {
					if (!driverId || driverId.length === 0) {
						return;
					}
					DriversService.getInfos(driverId).then((driverInfo) => $scope.driverInfo = driverInfo, e => {
						notify({message: deviceWarningrText, position: "right", classes: "alert-warning"});
					});
				});
			}

			if(Object.keys($scope.driverInfo).length == 0) {
				$scope.driverInfo.channelAddressSyntax = 'N.A.';
			}
		};

	};

	ChannelEditController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelEditController', ChannelEditController);

})();
