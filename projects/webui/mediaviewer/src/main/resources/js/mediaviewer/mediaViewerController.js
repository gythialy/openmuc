(function(){

	var injectParams = ['$scope', 'MediaViewerService'];

	var MediaViewerController = function($scope, MediaViewerService) {
		MediaViewerService.getAllMedia().then(response => $scope.media = response);
	};

	MediaViewerController.$inject = injectParams;

	angular.module('openmuc.mediaviewer').controller('MediaViewerController', MediaViewerController);

})();
