package com.symbol.enterprisebarcode;


import android.content.Context;
import android.util.Log;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by JRQ768 on 06/01/2015.
 */
public class ZebraAndroidExtensions implements Serializable {

    private static EMDKInterface emdk = null;
    private static Context context;
    private static final String LOG_TAG = "Symbol Barcode";

    public ZebraAndroidExtensions(Context c, CallbackContext callbackContext, int userEnabledScanner)
    {
        if (isEMDKAvailable(c))
        {
            //  Create the EMDK object
            emdk = new EMDKInterface(c, true, callbackContext, userEnabledScanner);
            this.context = c;
        }
        else
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
    }

    public static boolean isEMDKAvailable(Context c)
    {
        try {

            EMDKInterface test = new EMDKInterface(c, false, null, -1);
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
            emdk.disableScanner(false);
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


    public JSONObject enableScanner(CallbackContext callbackContext) {
        if (emdk != null)
            return emdk.enableScanner(callbackContext);
        else {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return null;
        }
    }

    public JSONObject disableScanner(CallbackContext callbackContext) {
        if (emdk != null)
        {
            emdk.disableScanner(true);
            return null;
        }
        else
        {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return null;
        }
    }

    public void onResume()
    {
        emdk.onResume();
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
    private List<ScannerInfo> deviceList = null;
    private static final String LOG_TAG = "Symbol Barcode";
    private HashMap ScannerConnectionTypes = new HashMap();
    private ScannerInfo defaultScanner = null;
    private CallbackContext scanCallbackContext = null;
    private CallbackContext initialisationCallbackContext = null;

    public EMDKInterface(Context c, boolean createManager, CallbackContext callbackContext, int userEnabledScanner)
    {
        this.userEnabledScanner = userEnabledScanner;
        if (createManager) {
            this.initialisationCallbackContext = callbackContext;
            //The EMDKManager object will be created and returned in the callback.
            EMDKResults results = EMDKManager.getEMDKManager(c, this);

            //Check the return status of getEMDKManager
            if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
                // EMDKManager object creation success
                Log.i(LOG_TAG, "EMDK manager has been successfully created");

            } else {
                // EMDKManager object creation failed
                Log.w(LOG_TAG, "Some error has occurred creating the EMDK manager.  EMDK functionality will not be available");

            }
        }
    }

    public void Destroy()
    {
        //  Shut down the EMDK
        if (emdkManager != null)
            emdkManager.release();
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        this.barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        //  If the user had previously enable the scanner then re-enable it here
        if (this.userEnabledScanner == 1)
            enableScanner(null);
        if (this.initialisationCallbackContext != null) {
            JSONObject scanners = enumerateScanners();
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
        return (this.emdkManager != null);
    }

    public JSONObject enumerateScanners()
    {
        JSONObject scanners = new JSONObject();
        JSONArray scannersArray = new JSONArray();
        if (barcodeManager != null)
        {
            //deviceList = new ArrayList<ScannerInfo>();
            deviceList = barcodeManager.getSupportedDevicesInfo();

            if (deviceList.size() != 0)
            {
                Iterator<ScannerInfo> it = deviceList.iterator();
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


    public JSONObject enableScanner(CallbackContext callbackContext) {
        if (scannerEnabled)
            return null;
        scanCallbackContext = callbackContext;
        enumerateScanners();
        if (deviceList == null || defaultScanner == null)
            return null;
        scanner = barcodeManager.getDevice(defaultScanner);
        scanner.addDataListener(this);
        scanner.addStatusListener(this);
        try
        {
            scannerEnabled = true;
            scanner.enable();
            scanner.read();
        }
        catch (ScannerException e)
        {
            Log.e(LOG_TAG, "Exception enabling Scanner: " + e.getMessage());
        }
        return null;
    }

    public void disableScanner(boolean fromUi)
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
            } catch (ScannerException e) {
                Log.e(LOG_TAG, "Exception disabling Scanner");
            }
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            if (scanData.size() > 0)
            {
                String dataString = scanData.get(0).getData();
                PluginResult result = new PluginResult(PluginResult.Status.OK, dataString);
//                this.scanCallbackContext.success(dataString);
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

    public void onResume() {
        //  If leave this app for another app and return to this app
    }
}
