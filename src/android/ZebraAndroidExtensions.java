package com.symbol.enterprisebarcode;


import android.content.Context;
import android.util.Log;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by JRQ768 on 06/01/2015.
 */
public class ZebraAndroidExtensions implements Serializable {

    private static EMDKInterface emdk = null;
    private static Context context;
    private static final String LOG_TAG = "Symbol Barcode";

    public ZebraAndroidExtensions(Context c, CallbackContext callbackContext)
    {
        if (isEMDKAvailable(c))
        {
            //  Create the EMDK object
            emdk = new EMDKInterface(c, true, callbackContext);
            this.context = c;
        }
        else
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
    }

    public static boolean isEMDKAvailable(Context c)
    {
        try {

            EMDKInterface test = new EMDKInterface(c, false, null);
            Log.i(LOG_TAG, "EMDK is available on this device");
            return true;
        }
        catch (NoClassDefFoundError e)
        {
            Log.i(LOG_TAG, "EMDK is not available on this device");
            return false;
        }
    }

    public boolean isReady()
    {
        if (emdk != null)
        {
            return emdk.IsReady();
        }
        else
            return false;
    }


    public static void destroy()
    {
        if (emdk != null)
        {
            emdk.disableScanner(false, null);
            emdk.Destroy();
        }
    }

    public JSONObject enumerateScanners()
    {
        if (emdk != null)
            return emdk.enumerateScanners();
        else
            return null;
    }


    public JSONObject enableScanner(CallbackContext callbackContext, JSONObject userSpecifiedArgumentsToEnable) {
        if (emdk != null)
            return emdk.enableScanner(callbackContext, userSpecifiedArgumentsToEnable);
        else {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return null;
        }
    }

    public JSONObject disableScanner(CallbackContext callbackContext) {
        if (emdk != null)
        {
            emdk.disableScanner(true, callbackContext);
            return null;
        }
        else
        {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return null;
        }
    }

    public void setUserRequestedScanner(String friendlyName) {
        if (emdk != null) {emdk.setUserRequestedScanner(friendlyName);}
    }
}

class EMDKInterface implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener
{
    private static Boolean m_bReady = false;  ///<  EMDK is supported and open
    private boolean scannerEnabled = false;
    private int userEnabledScanner;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private List<ScannerInfo> scannersOnDevice = null;
    private static final String LOG_TAG = "Enterprise Barcode";
    private ScannerInfo userRequestedScanner = null;    ///<  The scanner requested by the user
    private ScannerInfo defaultScanner = null;          ///<  The scanner which the driver reports as the default
    private CallbackContext scanCallbackContext = null; ///<  The Cordova callback context for each scan
    private CallbackContext initialisationCallbackContext = null;   ///<  The Cordova callback for our first plugin initialisation

    public EMDKInterface(Context c, boolean createManager, CallbackContext callbackContext)
    {
        this.userEnabledScanner = userEnabledScanner;
        if (createManager) {
            this.initialisationCallbackContext = callbackContext;
            //The EMDKManager object will be created and returned in the callback.
            EMDKResults results = EMDKManager.getEMDKManager(c, this);
            //Check the return status of getEMDKManager
            if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
                Log.i(LOG_TAG, "EMDK manager has been successfully created");

            } else {
                Log.w(LOG_TAG, "Some error has occurred creating the EMDK manager.  EMDK functionality will not be available");
            }
        }
    }

    public void Destroy()
    {
        //  Shut down the EMDK
        if (emdkManager != null)
            emdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        this.barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        JSONObject scanners = enumerateScanners();
        //  If the user had previously enable the scanner then re-enable it here
        if (this.initialisationCallbackContext != null) {
            this.initialisationCallbackContext.success(scanners);
            //  Only want to call it once, not for every subsequent re-entry
            this.initialisationCallbackContext = null;
        }
        Log.i(LOG_TAG, "EMDK has opened and will now be ready");
    }

    @Override
    public void onClosed() {
        Log.i(LOG_TAG, "EMDK has closed and is no longer ready");
        this.emdkManager = null;
        this.barcodeManager = null;
    }

    public Boolean IsReady()
    {
        return (this.emdkManager != null && this.barcodeManager != null);
    }

    public JSONObject enumerateScanners()
    {
        JSONObject scanners = new JSONObject();
        JSONArray scannersArray = new JSONArray();
        if (barcodeManager != null)
        {
            scannersOnDevice = barcodeManager.getSupportedDevicesInfo();
            if (scannersOnDevice.size() != 0)
            {
                Iterator<ScannerInfo> it = scannersOnDevice.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    JSONObject currentScanner = new JSONObject();
                    if (scnInfo.isDefaultScanner())
                        defaultScanner = scnInfo;
                    try {
                        currentScanner.put("friendlyName", scnInfo.getFriendlyName());
                        currentScanner.put("connectionType", scnInfo.getConnectionType());
                        currentScanner.put("decoderType", scnInfo.getDecoderType());
                        currentScanner.put("deviceType", scnInfo.getDeviceType());
                        currentScanner.put("modelNumber", scnInfo.getModelNumber());
                        currentScanner.put("connected", scnInfo.isConnected());
                        currentScanner.put("default", scnInfo.isDefaultScanner());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    scannersArray.put(currentScanner);
                }
            }
        }
        try {
            scanners.put("scanners", scannersArray);
        } catch (JSONException e) {
            Log.w(LOG_TAG, "Error populating scanner friendly names");
        }
        return scanners;
    }

    private void ProcessPropertiesPreEnable(JSONObject arguments) {
        //  Do the needful on the properties passed in from the user:
        if (arguments == null)
            return;
        if (arguments.has("friendlyName"))
        {
            //  Set the scanner's friendly name to be used
            try {
                setUserRequestedScanner(arguments.getString("friendlyName"));
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Malformed JSON whilst extracting friendly scanner name");
            }
        }
    }

    private void ProcessPropertiesPostEnable(JSONObject arguments, Scanner scanner) throws ScannerException
    {
        if (arguments == null)
            return;
        ScannerConfig scannerConfig = scanner.getConfig();
        try {
            if (arguments.has("ean8Enabled")) {
                scannerConfig.decoderParams.ean8.enabled = arguments.getBoolean("ean8Enabled");
            }
            //  todo - Add more here
        }
        catch (JSONException e)
        {
            Log.w(LOG_TAG, "Malformed JSON whilst processing decoders: " + e.getMessage());
        }
        scanner.setConfig(scannerConfig);
    }

    public boolean setUserRequestedScanner(String friendlyName)
    {
        if (scannersOnDevice == null)
            return false;

        if (scannersOnDevice.size() != 0)
        {
            Iterator<ScannerInfo> it = scannersOnDevice.iterator();
            while(it.hasNext()) {
                ScannerInfo scnInfo = it.next();
                if (scnInfo.getFriendlyName().equalsIgnoreCase(friendlyName)) {
                    userRequestedScanner = scnInfo;
                    return true;
                }
            }
        }
        return false;
    }


    public JSONObject enableScanner(CallbackContext callbackContext, JSONObject userSpecifiedArgumentsToEnable) {
        ProcessPropertiesPreEnable(userSpecifiedArgumentsToEnable);
        //  Handle the case where the user is switching scanners
        if (scannerEnabled)
            disableScanner(true, null);
        scanCallbackContext = callbackContext;
//        enumerateScanners();
        if (scannersOnDevice == null || (userRequestedScanner == null && defaultScanner == null)) {
            EnterpriseBarcode.FailureCallback(callbackContext, "Scanners not present on device or did not initialise properly");
            return null;
        }
        if (userRequestedScanner != null)
            scanner = barcodeManager.getDevice(userRequestedScanner);
        else
            scanner = barcodeManager.getDevice(defaultScanner);
        scanner.addDataListener(this);
        scanner.addStatusListener(this);
        try
        {
            scannerEnabled = true;
            scanner.enable();
            ProcessPropertiesPostEnable(userSpecifiedArgumentsToEnable, scanner);
            scanner.read();
        }
        catch (ScannerException e)
        {
            Log.e(LOG_TAG, "Exception enabling Scanner: " + e.getMessage());
            EnterpriseBarcode.FailureCallback(callbackContext, "Exception enabling Scanner: " + e.getMessage());
        }
        return null;
    }

    public void disableScanner(boolean fromUi, CallbackContext callbackContext)
    {
        if (fromUi)
            scannerEnabled = false;
        if (scanner != null)
        {
            try {
                scanner.cancelRead();
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                EnterpriseBarcode.SuccessCallback(callbackContext, "Scanner Disabled");
            } catch (ScannerException e) {
                Log.e(LOG_TAG, "Exception disabling Scanner");
                EnterpriseBarcode.FailureCallback(callbackContext, "Exception disabling Scanner: " + e.getMessage());
            }
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            if (scanData.size() > 0)
            {
                JSONObject scanDataResponse = new JSONObject();
                try {
                    scanDataResponse.put("data", scanData.get(0).getData());
                    scanDataResponse.put("type", scanData.get(0).getLabelType());
                    scanDataResponse.put("timestamp", scanData.get(0).getTimeStamp());
                }
                catch (JSONException e)
                {}
                PluginResult result = new PluginResult(PluginResult.Status.OK, scanDataResponse);
                result.setKeepCallback(true);
                this.scanCallbackContext.sendPluginResult(result);
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        StatusData.ScannerStates state = statusData.getState();
        Log.d(LOG_TAG, "Scanner State Change: " + state);
        switch (state)
        {
            case IDLE:
                //  Scanner is enabled and idle
                try {
                    scanner.read();
                } catch (ScannerException e) {
                    e.printStackTrace();
                }
                break;
            case WAITING:
                //  Scanner is waiting for trigger press
                break;
            case SCANNING:
                //  Scanner is scanning
                break;
            case DISABLED:
                //  Scanner is disabled
                break;
            default:
                break;
        }

    }

}
