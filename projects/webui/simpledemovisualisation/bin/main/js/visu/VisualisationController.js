(function(){

    var injectParams = ['$scope', '$interval', 'ChannelsService'];

    var VisualisationController = function($scope, $interval, ChannelsService) {
        var svg_document;

        display_visualisation = function() {

            svg_document = document.getElementById('simpleDemoGraphic').contentDocument;

            $scope.interval = "";
            $interval.cancel($scope.interval);
            $scope.interval = $interval(function(){
                ChannelsService.getAllChannels().then(async function(channels) {
                    $scope.channels = await channels.records;
                });
                if ($scope.channels != undefined){
                    $scope.channels.forEach(function(channel){
                        if (channel.id === "power_heatpump"){
                            textHeatPump = svg_document.getElementById("textHeatPump");
                            textHeatPump.textContent = channel.record.value + " kW";
                        }
                        if (channel.id === "power_electric_vehicle"){
                            textChargingStation = svg_document.getElementById("textChargingStation");
                            textChargingStation.textContent = channel.record.value + " kW";
                        }
                        if (channel.id === "power_photovoltaics"){
                            textPv = svg_document.getElementById("textPv");
                            textPv.textContent = channel.record.value + " kW";
                        }
                        if (channel.id === "power_grid"){
                            textGrid = svg_document.getElementById("textGrid");
                            textGrid.textContent = channel.record.value + " kW";
                        }
                    });
                }
            }, 500);    
        };           

        $scope.$on('$destroy', function () {
            $interval.cancel($scope.interval);
        });

        };

    VisualisationController.$inject = injectParams;

    angular.module('openmuc.openmuc-visu').controller('VisualisationController', VisualisationController);

})();
