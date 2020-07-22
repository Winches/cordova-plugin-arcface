var exec = require('cordova/exec');

exports.activeEngine = function (success, error) {
    exec(success, error, 'Arcface', 'activeEngine');
};

exports.getFaceFeature = function (arg, success, error) {
    exec(success, error, 'Arcface', 'getFaceFeature', [arg]);
};

exports.getFaceFeatureByLocalImage = function (success, error) {
    exec(success, error, 'Arcface', 'getFaceFeatureByLocalImage', []);
};

exports.compareFeature = function (obj, success, error) {
    exec(success, error, 'Arcface', 'compareFeature', [obj]);
};

exports.getLiveness = function (obj, success, error) {
    exec(success, error, 'Arcface', 'getLiveness', [obj]);
};
