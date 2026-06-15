(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.CalibrationDevice = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    return {
        Component: typeof CalibrationDeviceComponent !== 'undefined'
            ? CalibrationDeviceComponent
            : null,
        create: function (containerId, api, options) {
            if (typeof CalibrationDeviceComponent !== 'undefined') {
                return new CalibrationDeviceComponent(containerId, api, options);
            }
            console.error('CalibrationDeviceComponent 未加载，请先引入 CalibrationDeviceComponent.js');
            return null;
        },
        version: '1.0.0'
    };
}));
