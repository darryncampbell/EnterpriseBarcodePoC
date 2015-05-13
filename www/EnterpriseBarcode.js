//exports.coolMethod = function(arg0, success, error) {
//    exec(success, error, "EnterpriseBarcode", "coolMethod", [arg0]);
//};

var argscheck = require('cordova/argscheck'),
    channel = require('cordova/channel'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    cordova = require('cordova');

channel.createSticky('onEnterpriseBarcodeReady');
// Tell cordova channel to wait on the CordovaInfoReady event
channel.waitForInitialization('onEnterpriseBarcodeReady');

/**
 * This represents the mobile device, and provides properties for inspecting the model, version, UUID of the
 * phone, etc.
 * @constructor
 */
function EnterpriseBarcode() {
    this.available = false;
    var me = this;

    channel.onCordovaReady.subscribe(function() {
        me.getInfo(function(scannersObj) {
            //ignoring info.cordova returning from native, we should use value from cordova.version defined in cordova.js
            //TODO: CB-5105 native implementations should not return info.cordova
            me.available = true;
            me.scanners = scannersObj.scanners;
            channel.onEnterpriseBarcodeReady.fire();
        },function(e) {
            me.available = false;
            utils.alert("[ERROR] This device does not support EMDK");
        });
    });
}

/**
 * Get EnterpriseBarcode info
 *
 * @param {Function} successCallback The function to call when the heading data is available
 * @param {Function} errorCallback The function to call when there is an error getting the heading data. (OPTIONAL)
 */
EnterpriseBarcode.prototype.getInfo = function(successCallback, errorCallback) {
    argscheck.checkArgs('fF', 'EnterpriseBarcode.getInfo', arguments);
    exec(successCallback, errorCallback, "EnterpriseBarcode", "initializeBarcode", []);
};

EnterpriseBarcode.prototype.enumerate = function(successCallback, errorCallback)
{
	argscheck.checkArgs('fF', 'EnterpriseBarcode.enumerate', arguments);
    exec(successCallback, errorCallback, "EnterpriseBarcode", "enumerate", []);
};

EnterpriseBarcode.prototype.enable = function(successCallback, errorCallback, options)
{
	argscheck.checkArgs('fFO', 'EnterpriseBarcode.enable', arguments);
    exec(successCallback, errorCallback, "EnterpriseBarcode", "enable", [{"options":options}]);
};

EnterpriseBarcode.prototype.disable = function(successCallback, errorCallback)
{
	argscheck.checkArgs('fF', 'EnterpriseBarcode.disable', arguments);
    exec(successCallback, errorCallback, "EnterpriseBarcode", "disable", []);
};

EnterpriseBarcode.prototype.getProperties = function(successCallback, errorCallback)
{
	argscheck.checkArgs('fF', 'EnterpriseBarcode.getProperties', arguments);
    exec(successCallback, errorCallback, "EnterpriseBarcode", "getProperties", []);
};

EnterpriseBarcode.prototype.setProperties = function(successCallback, errorCallback, options)
{
	argscheck.checkArgs('fFO', 'EnterpriseBarcode.setProperties', arguments);
    exec(successCallback, errorCallback, "EnterpriseBarcode", "setProperties", [{"options":options}]);
};

EnterpriseBarcode.prototype.scanners = {"key":"val"};

module.exports = new EnterpriseBarcode();



