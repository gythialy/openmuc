(function(){

	var injectParams = [];

	var ChannelConfiguratorTabsDirective = function() {
		return {
			restrict: 'E',
			templateUrl: 'channelconfigurator/html/channelConfiguratorTabs.html',
		};
	};

	ChannelConfiguratorTabsDirective.$inject = injectParams;

	angular.module('openmuc.channels').directive('channelConfiguratorTabs', ChannelConfiguratorTabsDirective);

})();
