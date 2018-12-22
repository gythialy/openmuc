(function () {

    var injectParams = ['$scope', '$q', 'ChannelsService', 'ChannelDataService'];

    var DataExporterController = function ($scope, $q, ChannelsService, ChannelDataService) {

        $scope.selectedChannels = [];

        $scope.channels = [];

        $scope.startDate = new Date();
        $scope.startDate.setHours(0, 0, 0, 0);
        $scope.endDate = new Date();
        $scope.timeFormat = 1;

        $scope.disableExport = true;

        ChannelsService.getAllChannelsIds().then(channels = > {
            channels = channels.map(c = > ({id: c, historic: false})
    )
        ;

        channels.forEach(c = > {
            ChannelDataService.channelHasHistoricValues(c).then(r = > c.historic = r
    )
        ;
    })
        ;
        $scope.channels = channels;
    })
        ;

        $scope.exportData = function () {
            $scope.disableExport = true;
            var requests = [];

            $scope.selectedChannels.forEach(function (channel) {
                requests.push(ChannelsService.getValuesForExport(channel, $scope.startDate.getTime(), $scope.endDate.getTime()).then(r = > r)
            )
                ;
            });

            $q.all(requests).then(function (data) {
                $scope.data = [];
                var timestamps = [];

                /* extract all timestamps */
                data.forEach((channels) = > {
                    switch($scope.timeFormat
            )
                {
                case
                    1
                :
                    // get all unique time stamps
                    // note: the three dos convert the set back to an array
                    timestamps = [...new
                    Set(timestamps.concat(channels[0]))
                ]
                    ;
                    break;
                case
                    2
                :
                    // TODO; Unix Timestamp
                case
                    3
                :
                    // TODO: Human Readable Timestamp
                    // var a = new Date(TIMESTAMP);
                    // var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
                    // var year = a.getFullYear();
                    // var month = months[a.getMonth()];
                    // var date = a.getDate();
                    // var hour = a.getHours();
                    // var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
                    // var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
                    // var time = year + ' ' + month + ' ' + date + ' ' + hour + ':' + min + ':' + sec ;
                    // timestamps = $.unique($.merge(timestamps, channels[0]));
                    break;
                case
                    4
                :
                    // TODO: Human Readable Timestamp + Unix Timestamp
                    break;
                default:
                    timestamps = $.unique($.merge(timestamps, channels[0]));
                    break;
                }

            })
                ;

                timestamps.forEach((timestamp) = > {
                    var values = {timestamp: timestamp};

                data.forEach((channels) = > {
                    var index = channels[0].indexOf(timestamp);
                if (index !== -1) {
                    values[channels[2]] = channels[1][index];
                }
            })
                ;

                $scope.data.push(values);
            })
                ;

                $scope.disableExport = false;
            });
        };

        $scope.getHeader = function () {
            var header = [];
            switch ($scope.timeFormat) {
                case 1:
                case 2:
                    header.push('Timestamp');
                    break;
                case 3:
                    header.push('Date');
                    break;
                case 4:
                    header = ['Timestamp', 'Date'];
                    break;
                default:
                    header = ['Timestamp'];
                    break;
            }

            $scope.selectedChannels.forEach(channel = > header.push(channel)
        )
            ;

            return header;
        }
    };

    DataExporterController.$inject = injectParams;

    angular.module('openmuc.dataexporter').controller('DataExporterController', DataExporterController);

})();