(function(){

	var injectParams = [];

	var ChannelsFormDirective = function() {
		return {
			restrict: 'E',
			templateUrl: 'channelconfigurator/html/channels/form.html'
		};
	};

	ChannelsFormDirective.$inject = injectParams;

	angular.module('openmuc.channels').directive('channelsForm', ChannelsFormDirective);

})();
