(function () {

    var injectParams = ['$http', 'SETTINGS', 'DeviceDataService', 'RestServerAuthService'];

    var DevicesService = function ($http, SETTINGS, DeviceDataService, RestServerAuthService) {

        this.getAllDevices = function () {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(function (response) {
                var devices = [];

                // add basic data
                response.data['devices'].forEach(value => devices.push({id: value}));

                return devices;
            });
        };

        this.getAllDevicesIds = function () {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data['devices'];
            });
        };

        this.getDevices = function (driver) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/' + SETTINGS.DEVICES_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(function (response) {
                var devices = [];
                // add basic data
                response.data['devices'].forEach(value => devices.push({id: value}));

                // add additional data
                devices.forEach(function (device) {
                    DeviceDataService.getDeviceData(device).then(function (d) {
                        device['data'] = d;
                    });
                });

                // add additional configs data
                devices.forEach(function (device) {
                    DeviceDataService.getDeviceConfigs(device).then(function (d) {
                        device['configs'] = d;
                    });
                });

                return devices;
            });
        };


        this.getDeviceRecords = function (device) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(function (response) {
                return response;
            });
        };

        this.getDevice = function (id) {
            var device = [];
            device['id'] = id;
            device['configs'] = [];

            DeviceDataService.getDeviceConfigs(device).then(function (response) {
                device['configs'] = response;
            });

            return device;
        };

        this.scan = function (device, settings) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id + '/' + SETTINGS.SCAN_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data;
            });

        };

        this.destroy = function (id) {
            var req = {
                method: 'DELETE',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + id,
                dataType: 'json',
                data: '',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(function (response) {
                return response.data;
            });
        };

        this.update = function (device) {
            var req = {
                method: 'PUT',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id + SETTINGS.CONFIGS_URL,
                dataType: 'json',
                data: {configs: device.configs},
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data;
            });
        };

        this.create = function (device) {
            var req = {
                method: 'POST',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.configs.id,
                dataType: 'json',
                data: device,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(function (response) {
                return response.data;
            });
        };

    };

    DevicesService.$inject = injectParams;

    angular.module('openmuc.devices').service('DevicesService', DevicesService);

})();
