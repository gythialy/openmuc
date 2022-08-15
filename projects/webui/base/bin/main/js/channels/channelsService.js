(function () {

    var injectParams = ['$http', '$interval', 'SETTINGS', 'ChannelDataService', 'RestServerAuthService'];

    var ChannelsService = function ($http, $interval, SETTINGS, ChannelDataService, RestServerAuthService) {

        this.getAllChannels = function () {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data;
            });
        };

        this.getAllChannelsIds = function () {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then((response) => response.data.records.map((record) => record.id));
        };

        this.getChannelDriverId = function (channelId) {
            var driverId = '';
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channelId + '/' + SETTINGS.DRIVER_ID,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                if (response.data.driverId) {
                    return response.data.driverId;
                }
                return driverId;
            });
        };

        this.getChannelDeviceId = function (channelId) {
            var deviceId = '';
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channelId + SETTINGS.DEVICE_ID,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                if (response.data.deviceId) {
                    deviceId = response.data.deviceId;
                }
                return deviceId;
            });
        };


        this.valuesDisplayPrecision = function (numeric_value, precision) {
            //nasty way of default argument in js...
            if (typeof(precision) === 'undefined') precision = 0.001;

            if (numeric_value % 1. != 0.) {
                return Math.floor(numeric_value / precision) * precision;
            } else {
                return numeric_value;
            }
        };

        this.getChannels = function (device) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id + '/' + SETTINGS.CHANNELS_URL,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then((response) => {
                return response.data.channels.map((channelId) => {
                    var channel = {id: channelId, data: null, records: null};
                    ChannelDataService.getChannelData(channel).then((data) => channel.data = data);
                    ChannelDataService.getChannelDataValues(channel).then((records) => channel.records = records);
                    return channel;
                });
            });
        };

        this.getHistoryValues = function (channelId, from, until) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channelId + SETTINGS.CHANNELS_HISTORY_URL + '?from=' + from + '&until=' + until,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            var self = this;

            return $http(req).then(function (response) {
                var values = [];

                //regular expression matching entry of TimeSeriesString
                var timeSeriesStringRegExp = /(\d{13}),(-*\d*\.\d*);/;
                var isTimeSeriesStringChannel = false;


                //response.data.records.forEach((value) => {
                    angular.forEach(response.data.records, function (value, key) {
                    if (angular.isNumber(value.value)) {

                        //if content of value is numeric, append (timestamp, value) to array
                        values.push({x: value.timestamp, y: self.valuesDisplayPrecision(value.value, 0.001)});

                    } else if (typeof(value.value) == 'string') {

                        //if content is string, check if it matches format of TimeSeriesString
                        var match = value.value.match(timeSeriesStringRegExp);
                        if (match !== null) {
                            isTimeSeriesStringChannel = true;
                        }
                        //break the loop, i.e. only detect channel property, extend by check for flags etc.
                        return false;
                    }
                });

                if (isTimeSeriesStringChannel) {
                    //get current time
                    var now = new Date();
                    var latestTimestamp = until;
                    if (until >= now + 60 * 60000) {
                        latestTimestamp = until + 8 * 60 * 60000;
                    }
                    response.data.records.reverse().forEach((value) => {
                        var reverse_entry_list = value.value.split(";").reverse();
                        reverse_entry_list.forEach((entry) => {
                            if (!entry || entry.trim().length === 0) {
                                return;
                            }
                            var stringPair = entry.split(",");
                            var timestamp = parseInt(stringPair[0]);
                            if (timestamp < latestTimestamp && timestamp > from) {
                                var valAtTime = parseFloat(stringPair[1]);
                                values.push({x: timestamp, y: self.valuesDisplayPrecision(valAtTime, 0.001)});
                                latestTimestamp = timestamp;
                            }
                        });
                    });
                    values.reverse();
                }
                return values;
            });
        };

        this.getTSChannelValuesForDiagram = function (channelId) {
            var channel = {id: channelId};
            var values = [];

            ChannelDataService.getChannelDataValues(channel).then(function (response) {
                if (response.flag != "VALID" || response.value.length === 0) {
                    return;
                }

                var value = response.value;
                var reverse_entry_list = value.split(";").reverse();
                console.log(reverse_entry_list);
                reverse_entry_list.forEach((entry) => {
                    if (!entry || entry.trim().length === 0) {
                        return;
                    }
                    var stringPair = entry.split(",");
                    var timestamp = parseInt(stringPair[0]);

                    var valAtTime = parseFloat(stringPair[1]);
                    console.log(timestamp);
                    console.log(valAtTime);
                    values.push({x: timestamp, y: self.valuesDisplayPrecision(valAtTime, 0.001)});
                });
                values.reverse();
            });

            return values;
        };

        this.getValuesForExport = function (channelId, from, until) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channelId + SETTINGS.CHANNELS_HISTORY_URL + '?from=' + from + '&until=' + until,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then((response) => {
                var values = [];
                var timestamps = [];

                angular.forEach(response.data.records, (channel) => {
                    timestamps.push(channel.timestamp);
                    values.push(channel.value);
                });

                return [timestamps, values, channelId];
            });
        };

        this.getChannel = function (channelId) {
            var channel = {
                id: channelId,
                configs: []
            };

            ChannelDataService.getChannelData(channel).then(configs => channel.configs = configs);

            return channel;
        };

        this.getChannelCurrentValue = function (channelId) {
            var channel = [];
            channel['id'] = channelId;

            return ChannelDataService.getChannelDataValues(channel).then(function (d) {
                return d;
            });
        };

        this.writeChannel = (channel, doWrite) => writeChannel(channel.id, channel.type, channel.newValue, doWrite);

        function writeChannel(id, type, newValue, doWrite) {
            var dataType = null;
            if (type == "STRING") {
                dataType = {record: {value: newValue}};
            }
            else if (type == "BYTE_ARRAY") {
                newValue = newValue.replace('[', '').replace(']', '');

                var arrayValue = newValue.split(',').map((v) => {
                    if (v.length === 0) {
                        throw 'Illegal value.';
                    }
                    var res = parseInt(v);
                    if (res > 255 || res < 0) {
                        throw 'Byte array value out of range.';
                    }
                    return res;
                });
                dataType = {record: {value: arrayValue}};
            }
            else if (type == "INTEGER" || type == "LONG" || type == "SHORT" || type == "BYTE") {
                dataType = {record: {value: parseInt(newValue)}};
            }
            else if (type == "BOOLEAN") {
                newValue = parseFloat(newValue) == 1 || newValue == 'true';
                dataType = {record: {value: newValue}};
            }
            else {
                var val = parseFloat(newValue);
                if (isNaN(val)) {
                    throw 'Failed to convert number.';
                }
                dataType = {record: {value: val}};
            }
            var urlString;

            if (doWrite) {
                urlString = SETTINGS.API_URL + SETTINGS.CHANNELS_URL + id
            }
            else {
                urlString = SETTINGS.API_URL + SETTINGS.CHANNELS_URL + id + '/latestRecord'
            }

            var req = {
                method: 'PUT',
                url: urlString,
                dataType: 'json',
                data: dataType,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            return $http(req).then(function (response) {
                return response.data;
            });
        }

        this.destroy = function (id) {
            var req = {
                method: 'DELETE',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + id,
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

        this.update = function (channel) {
            var req = {
                method: 'PUT',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id + SETTINGS.CONFIGS_URL,
                dataType: 'json',
                data: {configs: channel.configs},
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data;
            });
        };

        this.create = function (channel) {
            var req = {
                method: 'POST',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.configs.id,
                dataType: 'json',
                data: channel,
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

    ChannelsService.$inject = injectParams;

    angular.module('openmuc.channels').service('ChannelsService', ChannelsService);

})();
