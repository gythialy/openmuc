(function () {

    var injectParams = ['$http', 'DriverDataService', 'RestServerAuthService', 'SETTINGS'];

    var DriversService = function ($http, DriverDataService, RestServerAuthService, SETTINGS) {

        this.getDrivers = function () {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                // add basic data
                var drivers = response.data.drivers.map(value => {
                    var driver = {id: value};

                    // add additional data
                    DriverDataService.getDriverData(driver).then(d => driver.data = d);
                    DriverDataService.getDriverConfigs(driver).then(c => driver.configs = c);
                    return driver;
                });

                return drivers;
            });
        };

        this.getDriver = function (id) {
            var driver = {
                id: id,
                configs: []
            };
            DriverDataService.getDriverConfigs(driver).then(configs => driver.configs = configs);

            return driver;
        };

        this.getInfos = function (id) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + id + SETTINGS.INFOS_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(r => r.data.infos);
        };

        this.scan = function (driver, settings) {
            var url = SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/' + SETTINGS.SCAN_URL;

            if (settings) {
                url = url + '?settings=' + settings;
            }

            var req = {
                method: 'GET',
                url: url,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(r => r.data);
        };

        this.scanInterrupt = function (driver) {
            var req = {
                method: 'PUT',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/' + SETTINGS.SCAN_INTERRUPT_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data;
            });
        };

        this.scanProgressInfo = function (driver) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/' + SETTINGS.SCAN_PROGRESS_INFO_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(r => r.data);
        };

        this.destroy = function (id) {
            var req = {
                method: 'DELETE',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + id,
                dataType: 'json',
                data: '',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(r => r.data);
        };

        this.update = function (driver) {
            var req = {
                method: 'PUT',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + SETTINGS.CONFIGS_URL,
                dataType: 'json',
                data: {configs: driver.configs},
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(r => r.data);
        };

        this.create = function (driver) {
            var req = {
                method: 'POST',
                url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id,
                dataType: 'json',
                data: driver,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(r => r.data);
        };

    };

    DriversService.$inject = injectParams;

    angular.module('openmuc.drivers').service('DriversService', DriversService);

})();
