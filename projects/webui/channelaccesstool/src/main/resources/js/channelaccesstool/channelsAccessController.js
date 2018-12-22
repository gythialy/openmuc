(function () {

    var injectParams = ['$scope', '$location', 'notify', '$translate', '$interval', 'DevicesService', 'DeviceDataService', 'ChannelsService', 'ChannelDataService'];

    var ChannelsAccessController = function ($scope, $location, notify, $translate, $interval, DevicesService, DeviceDataService, ChannelsService, ChannelDataService) {

        $translate('CHANNEL_VALUE_UPDATED_SUCCESSFULLY').then(text =>  channelWriteValueOKText = text);
        $translate('CHANNEL_VALUE_UPDATED_ERROR').then(text => channelWriteValueErrorText = text);
        $translate('CHANNEL_NO_VALUE_TO_WRITE').then(text => channelNoValueToWrite = text);

        $scope.checkedDevices = [];

        $scope.interval = '';

        function retrieveDeviceFor(deviceId) {
            var device = {id: deviceId, channels: {}};
            ChannelsService.getChannels(device).then((channels) => {
                channels.forEach(channel => {
                    channel.newValue = '';
                    device.channels[channel.id] = channel;
                });
            });
            return device;
        }

        DevicesService.getAllDevicesIds().then((devices) => {
            devices.forEach((deviceId) => {
                if ($location.search()[deviceId]) {
                    var device = retrieveDeviceFor(deviceId);
                    $scope.checkedDevices.push(device);
                }
            });

            $scope.checkedDevices.forEach((device) => {
                DeviceDataService.getDeviceConfigs(device).then(function (d) {
                    device['configs'] = d;
                });
            });    

            $scope.interval = $interval(() => {
                $scope.checkedDevices.forEach((device) => {
                    DevicesService.getDeviceRecords(device).then((result) => {
                        result.data.records.forEach(record => {
                            var channel = device.channels[record.id];
                            channel.records = record.record;
                            if (channel.data.valueType === null) {
                                channel.type = record.type;
                            } else {
                                channel.type = channel.data.valueType;
                            }
                        });
                    });
                });

            }, 1000); // 1s
        });

        $scope.channelsMapAsArray = Object.values;

        $scope.setNewValue = (channel, doWrite) => {
            if (!channel.newValue || channel.newValue.trim().length === 0) {
                notify({message: channelNoValueToWrite, position: "right", classes: "alert-warning"})
                return;
            }

            try {
                ChannelsService.writeChannel(channel, doWrite).then(
                    resp => notify({message: channelWriteValueOKText, position: "right", classes: "alert-success"})
                    , error => notify({message: channelWriteValueErrorText, position: "right", classes: "alert-warning"})
                );
            } catch (e) {
                notify({message: channelWriteValueErrorText, position: "right", classes: "alert-warning"});
            }

            channel.newValue = '';
        };

        $scope.$on('$destroy', () => $interval.cancel($scope.interval));
    };

    ChannelsAccessController.$inject = injectParams;

    angular.module('openmuc.channelaccesstool').controller('ChannelsAccessController', ChannelsAccessController);

})();
