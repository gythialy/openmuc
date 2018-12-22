(function () {

    var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];

    var MediaViewerService = function ($http, SETTINGS, RestServerAuthService) {

        this.getAllMedia = function () {
            var url = SETTINGS.MEDIA_CONFIG_URL;

            var req = {
                method: 'GET',
                dataType: 'json',
                url: url,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data.documents;
            });
        };

    };

    MediaViewerService.$inject = injectParams;

    angular.module('openmuc.mediaviewer').service('MediaViewerService', MediaViewerService);

})();
