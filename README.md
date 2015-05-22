
*This plugin is provided without guarantee or warranty*
=========================================================

# EnterpriseBarcode
This plugin defines an `enterpriseBarcode` object which provides an API for interacting with the hardware scanner on Zebra devices.  The enterpriseBarcode object is not available until after the `deviceready` event.

    document.addEventListener("deviceready", onDeviceReady, false);
    function onDeviceReady() {
        console.log(enterpriseBarcode);
    }
    
## Installation

    cordova plugin add https://github.com/darryncampbell/EnterpriseBarcodePoC.git
__Requires Cordova 5.0 or higher otherwise your application will get build errors.  When updating from a previous Cordova version it is necessary to re-add this plugin__
    
## Supported Platforms

- Android
    
## enterpriseBarcode.enumerate

Returns the available hardware scanners on the device

    enterpriseBarcode.enumerate(enumerateSuccess, enumerateFailure);
    
### Description

The `enterpriseBarcode.enumerate` function queries the device hardware for available scanners and returns them through the success callback

### Android Quirks

Barcode functionality is only available for Zebra mobile devices.

### Example

Output the available scanners to the console

    enterpriseBarcode.enumerate(
        function(scannersObj)
        {
            for(scanner in scannersObj.scanners)
            {
                console.log("Scanner: " + scanner.friendlyName);
            }
        },
        function(status)
        {
            console.log("Failed to Enumerate Scanners: " + status.message);
        }
    );

The Scanners are also available through the `enterpriseBarcode.scanners` property after the device ready event.

    for(scanner in enterpriseBarcode.scanners)
    {
        console.log("Scanner: " + scanner.friendlyName);
    }
    
## Barcode Options

Optional parameters to customize the Scanner settings can be provided either through the `setProperties` method or through the `enable` method.  These settings will persist until the application is closed.

    {   code11Enabled : true,
        code128Enabled : true,
        code39Enabled : false,
        code93Enabled : false,
        dataMatrixEnabled : false,
        ean8Enabled : true,
        ean13Enabled : true,
        upcaEnabled : true,
        upce1Enabled : false,
        pdf417Enabled : true,
        friendlyName : '2D Barcode Imager'};
        
### Options

- __code11Enabled__ : Enable or Disable recognition of the Code 11 Symbology
- __code128Enabled__ : Enable or Disable recognition of the Code 128 Symbology
- __code39Enabled__ : Enable or Disable recognition of the Code 39 Symbology
- __code93Enabled__ : Enable or Disable recognition of the Code 93 Symbology
- __dataMatrixEnabled__ : Enable or Disable recognition of the Data Matrix Symbology
- __ean8Enabled__ : Enable or Disable recognition of the EAN 8 Symbology
- __ean13Enabled__ : Enable or Disable recognition of the EAN 13 Symbology
- __upcaEnabled__ : Enable or Disable recognition of the UPCA Symbology
- __upce1Enabled__ : Enable or Disable recognition of the UPCE1 Symbology
- __pdf417Enabled__ : Enable or Disable recognition of the PDF417 Symbology
- __friendlyName__ : Specifies which scanner should be enabled.  __Only applicable to options passed to the enable method.__

### Quirks

Not all barcode scanners will support all symbologies, for instance 2D symbologies like PDF417 will not be supported on 1D laser scanners. 

## enterpriseBarcode.enable

Enables the barcode scanner hardware and the associated trigger, so pressing the hardware trigger will initiate a scan.

    enterpriseBarcode.enable(enableSuccess, enableFailure, barcodeOptions);
    
### Description

The Enable method will instruct the scanning hardware to initialise and attach the trigger that can subsequently be used to initiate a scan.  The success callback is called firstly to indicate that the scanner has successfully enabled and subsequently on each barcode scan (see example)

### Example

    enterpriseBarcode.enable(
        function(scannedObj)
        {
            if (scannedObj.status == "enabled")
                console.log("Scanner has successfully enabled");
            else
            {
                console.log("Scan Data: " + scannedObj.data);
                console.log("Scan Symbology: "  + scannedObj.type);
                console.log("Scan Time: " + scannedObj.timestamp);
            }
        },
        function(status)
        {
            console.log("Enable Failure: " + status.message);
        },
        {
            'friendlyName':'2D Barcode Imager'
        }
    );
    
## enterpriseBarcode.disable

Disables the currently enabled barcode scanner hardware and disconnects the associated trigger. 

    enterpriseBarcode.disable(disableSuccess, disableFailure);
    
### Description

The Disable method will instruct the currently enabled scanning hardware to deinitialise.  Calling `disable` without having previously enabled a scanner will have no effect.

### Example

    enterpriseBarcode.disable(
        function(status)
        {
            console.log("Disable Success: " + status.message);
        },
        function(status)
        {
            console.log("Disable Failure: " + status.message);
        }
    );
    
## enterpriseBarcode.setProperties

Sets any of the properties listed under barcodeOptions to the currently enabled scanner.  If there is no currently enabled scanner then this method has no effect.

### Example

    enterpriseBarcode.setProperties(
        function(status)
        {
            console.log("Set Properties Success: " + status.message);
        },
        function(status)
        {
            console.log("Set Properties Failure: " + status.message);
        },
        {
            'code11Enabled':true,
            'code39Enabled':false
        }
    );


## enterpriseBarcode.getProperties

Retrieves the properties listed under barcodeOptions from the currently enabled scanner.  If there is no currently enabled scanner then this method has no effect.

### Example

    enterpriseBarcode.getProperties(
        function(props)
        {
            document.getElementById("code11Check").checked = props.code11Enabled;
            document.getElementById("code39Check").checked = props.code39Enabled;
        },
        function(data)
        {
            console.log("Get Properties Failure: " + status.message);
        }
    );
