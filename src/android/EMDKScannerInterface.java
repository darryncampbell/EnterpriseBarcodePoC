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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Darryn_2 on 13/05/2015.
 */
class EMDKScannerInterface implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener
{
    private static Boolean m_bReady = false;            ///<  EMDK is supported and open and ready
    private boolean scannerEnabled = false;             ///<  Keep track of whether a scanner is enabled to avoid duplicate enabling
    private boolean continuouslyReading = false;        ///<  This plugin assumes that any enabled scanner is also continuously reading
    private EMDKManager emdkManager = null;             ///<  If the EMDK is available for scanning, this property will be non-null
    private BarcodeManager barcodeManager = null;       ///<  EMDK Barcode Manager object
    private Scanner scanner = null;                         ///<  The scanner currently in use
    private List<ScannerInfo> scannersOnDevice = null;      ///<  The available scanners, as returned from Enumerate
    private static final String LOG_TAG = "Enterprise Barcode";
    private ScannerInfo m_userRequestedScanner = null;      ///<  The scanner requested by the user
    private ScannerInfo m_deviceDefaultScanner = null;      ///<  The scanner which the driver reports as the default
    private CallbackContext scanCallbackContext = null;     ///<  The Cordova callback context for each scan
    private CallbackContext initialisationCallbackContext = null;   ///<  The Cordova callback for our first plugin initialisation

    /**
     * Constructor creates an interface to the EMDK
     * @param c
     * @param createManager
     * @param callbackContext
     */
    public EMDKScannerInterface(Context c, boolean createManager, CallbackContext callbackContext)
    {
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

    /**
     * Shut down the EMDK
     */
    public void Destroy()
    {
        if (emdkManager != null)
            emdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE);
    }

    /**
     * Called when the EMDK is open and ready for scanning.  The plugin can now be allowed to complete
     * initialising
     * @param emdkManager
     */
    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        this.barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        JSONObject scanners = enumerateScanners(null);
        //  If the user had previously enable the scanner then re-enable it here
        if (this.initialisationCallbackContext != null) {
            this.initialisationCallbackContext.success(scanners);
            //  Only want to call it once, not for every subsequent re-entry
            this.initialisationCallbackContext = null;
        }
        Log.i(LOG_TAG, "EMDK has opened and will now be ready");
    }

    /**
     * This plugin is not fault-tolerant of the EMDK unexpectedly closing
     */
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

    /**
     * EnumerateScanners can be called in one of two ways, either through the interface call to
     * enumerate or on initialise when it is used to pre-populate the enterpriseBarcode.scanners
     * property
     * @param callbackContext only used in the enumerate call, notifies the user of success or failure
     *                        of enumerate
     * @return JSONObject of scanners, only used when the plugin is initialising to populate the
     * enterpriseBrowser.scanners array
     */
    public JSONObject enumerateScanners(CallbackContext callbackContext)
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
                        m_deviceDefaultScanner = scnInfo;
                    try {
                        currentScanner.put("friendlyName", scnInfo.getFriendlyName());
                        currentScanner.put("connectionType", scnInfo.getConnectionType());
                        currentScanner.put("decoderType", scnInfo.getDecoderType());
                        currentScanner.put("deviceType", scnInfo.getDeviceType());
                        String modelNumber = scnInfo.getModelNumber();
                        if (modelNumber.equalsIgnoreCase(""))
                            currentScanner.put("modelNumber", "undefined");
                        else
                            currentScanner.put("modelNumber", scnInfo.getModelNumber());
                        currentScanner.put("connected", scnInfo.isConnected());
                        currentScanner.put("default", scnInfo.isDefaultScanner());
                    } catch (JSONException e) {
                        EnterpriseBarcode.FailureCallback(callbackContext, "Error retrieving scanner properties: " + e.getMessage());
                    }
                    scannersArray.put(currentScanner);
                }
            }
        }
        try {
            scanners.put("scanners", scannersArray);
        } catch (JSONException e) {
            EnterpriseBarcode.FailureCallback(callbackContext, "Error enumerating Scanners");
            Log.w(LOG_TAG, "Error populating scanner friendly names");
            return null;
        }

        if (callbackContext != null)
            callbackContext.success(scanners);
        return scanners;
    }

    /**
     * Some properties need to be specified before the scanner is enabled like friendlyName
     * (used to specify which scanner should be enabled)
     * @param arguments
     */
    private void ProcessPropertiesPreEnable(JSONObject arguments) {
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

    /**
     * Some properties need to be specified after the scanner is enabled, e.g. Decoder properties
     * @param arguments arguments to set
     * @param scanner Which scanner the properties are being set on, this scanner must be enabled and
     *                not have a read pending.
     * @throws ScannerException
     */
    private void ProcessPropertiesPostEnable(JSONObject arguments, Scanner scanner) throws ScannerException, JSONException
    {
        if (arguments == null)
            return;
        ScannerConfig scannerConfig = scanner.getConfig();
        ScannerConfiguration myConfig = new ScannerConfiguration(scannerConfig);
        myConfig.setNewConfig(scanner, arguments);
    }

    /**
     * Given a friendly name, set the Scanner member variable as appropriate
     * @param friendlyName
     * @return
     */
    private boolean setUserRequestedScanner(String friendlyName)
    {
        if (scannersOnDevice == null)
            return false;

        if (scannersOnDevice.size() != 0)
        {
            Iterator<ScannerInfo> it = scannersOnDevice.iterator();
            while(it.hasNext()) {
                ScannerInfo scnInfo = it.next();
                if (scnInfo.getFriendlyName().equalsIgnoreCase(friendlyName)) {
                    m_userRequestedScanner = scnInfo;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Enable the scanner so it responds to the hardware trigger, this combines the EMDK concepts
     * of enabling and making the scanner ready (submitting a read) since this plugin assumes the
     * two are analogous.
     * @param callbackContext
     * @param userSpecifiedArgumentsToEnable
     * @return
     */
    public JSONObject enableScanner(CallbackContext callbackContext, JSONObject userSpecifiedArgumentsToEnable) {
        ProcessPropertiesPreEnable(userSpecifiedArgumentsToEnable);
        //  Handle the case where the user is switching scanners
        if (scannerEnabled)
            disableScanner(false, null);
        scanCallbackContext = callbackContext;
        if (scannersOnDevice == null || (m_userRequestedScanner == null && m_deviceDefaultScanner == null)) {
            EnterpriseBarcode.FailureCallback(callbackContext, "Scanners not present on device or did not initialise properly");
            return null;
        }
        //  Logic to ensure the correct scanner gets enabled
        if (m_userRequestedScanner != null)
            scanner = barcodeManager.getDevice(m_userRequestedScanner);
        else
            scanner = barcodeManager.getDevice(m_deviceDefaultScanner);
        scanner.addDataListener(this);
        scanner.addStatusListener(this);
        try
        {
            scanner.enable();
            //  A read can not be pending whilst properties are set on the scanner, do not allow
            //  one to be set (including the logic of setting it when the Scanner enters IDLE state)
            try {
                ProcessPropertiesPostEnable(userSpecifiedArgumentsToEnable, scanner);
            }
            catch (JSONException e)
            {
                EnterpriseBarcode.FailureCallback(callbackContext, "Malformed JSON in Enable properties");
            }
            if (!scanner.isReadPending())
                scanner.read();
            scannerEnabled = true;
            continuouslyReading = true;
            //  Enable callback has two purposes, 1 is to alert the user when the scanner has finished
            //  enabling and 2 is to provide the user with scanned data.
            EnterpriseBarcode.EnableCallback(callbackContext, "enabled", "", ScanDataCollection.LabelType.UNDEFINED, "");
        }
        catch (ScannerException e)
        {
            Log.e(LOG_TAG, "Exception enabling Scanner: " + e.getMessage());
            EnterpriseBarcode.FailureCallback(callbackContext, "Exception enabling Scanner: " + e.getMessage());
        }
        return null;
    }

    /**
     * Scanner can either be disabled from the user interface enterpriseBarcode.disable() or
     * as part of the Scanner Enable logic, to prevent two scanners being enabled simultaneously.
     * If invoked from the interface then the callbackContext is non-null.
     * @param fromUi
     * @param callbackContext
     */
    public void disableScanner(boolean fromUi, CallbackContext callbackContext)
    {
        scannerEnabled = false;
        continuouslyReading = false;
        if (scanner != null)
        {
            try {
                scanner.cancelRead();
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                if (fromUi)
                    EnterpriseBarcode.SuccessCallback(callbackContext, "Scanner Disabled");
            } catch (ScannerException e) {
                Log.e(LOG_TAG, "Exception disabling Scanner");
                if (fromUi)
                    EnterpriseBarcode.FailureCallback(callbackContext, "Exception disabling Scanner: " + e.getMessage());
            }
        }
    }

    /**
     * Retrieve all properties from the scanner and return as a JSON object.  Only a subset of
     * all properties are supported and are managed by the ScannerConfiguration class.
     * @param callbackContext
     */
    public void getProperties(CallbackContext callbackContext)
    {
        if (scanner != null)
        {
            if (!scannerEnabled || !continuouslyReading)
            {
                EnterpriseBarcode.FailureCallback(callbackContext, "Unable to get the properties of a disabled scanner");
            }
            else
            {
                try {
                    //  Temporarily disable the scanner reads so they don't interfere with retrieval of properties
                    continuouslyReading = false;
                    scanner.cancelRead();
                    ScannerConfig scannerConfig = scanner.getConfig();
                    if (scannerConfig != null) {
                        ScannerConfiguration myConfig = new ScannerConfiguration(scannerConfig);
                        JSONObject currentConfig = myConfig.getJSON();
                        callbackContext.success(currentConfig);
                    } else {
                        EnterpriseBarcode.FailureCallback(callbackContext, "Could not get Scanner configuration");
                    }
                    continuouslyReading = true;
                    scanner.read();
                }
                catch (ScannerException e)
                {
                    EnterpriseBarcode.FailureCallback(callbackContext, "Exception whilst retrieving properties: " + e.getMessage());
                }
            }
        }
        else
        {
            EnterpriseBarcode.FailureCallback(callbackContext, "No Enabled Scanner to retrieve properties for");
        }
    }

    /**
     * Set properties on the scanner such as which symbologies are enabled.  Only a subset of
     * properties are supported and are managed by the ScannerConfiguration class.  Properties
     * cannot be set on a disabled scanner or a scanner with pending reads so reads must be
     * temporarily suspended.
     * @param callbackContext
     * @param arguments
     */
    public void setProperties(CallbackContext callbackContext, JSONObject arguments) {
        if (arguments == null)
        {
            EnterpriseBarcode.FailureCallback(callbackContext, "No properties specified to set");
            return;
        }
        if (scanner != null)
        {
            if (!scannerEnabled || !continuouslyReading)
            {
                EnterpriseBarcode.FailureCallback(callbackContext, "Unable to set the properties of a disabled scanner");
            }
            else
            {
                try {
                    //  Temporarily disable the scanner reads so they don't interfere with retrieval of properties
                    continuouslyReading = false;
                    scanner.cancelRead();

                    ScannerConfig scannerConfig = scanner.getConfig();
                    ScannerConfiguration myConfig = new ScannerConfiguration(scannerConfig);
                    try {
                        myConfig.setNewConfig(scanner, arguments);
                    }
                    catch (JSONException e)
                    {
                        EnterpriseBarcode.FailureCallback(callbackContext, "Malformed JSON");
                    }
                    continuouslyReading = true;
                    scanner.read();
                }
                catch (ScannerException e)
                {
                    EnterpriseBarcode.FailureCallback(callbackContext, "Exception whilst retrieving properties: " + e.getMessage());
                }
            }
        }
        else
        {
            EnterpriseBarcode.FailureCallback(callbackContext, "No Enabled Scanner to retrieve properties for");
        }
    }

    /**
     * Callback received from the scanner layer when a barcode is scanned.  Inform the user.
     * @param scanDataCollection
     */
    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            if (scanData.size() > 0)
            {
                EnterpriseBarcode.EnableCallback(this.scanCallbackContext,
                        "scannedData", scanData.get(0).getData(), scanData.get(0).getLabelType(),
                        scanData.get(0).getTimeStamp());
            }
        }
    }

    /**
     * Scanner status callback received from teh Scanner.  For the purpose of this plugin assume
     * the Scanner wants to continuously read barcodes so submit a pending read whenever the scanner
     * goes IDLE.
     * @param statusData
     */
    @Override
    public void onStatus(StatusData statusData) {
        StatusData.ScannerStates state = statusData.getState();
        Log.d(LOG_TAG, "Scanner State Change: " + state);
        switch (state)
        {
            case IDLE:
                //  Scanner is enabled and idle
                try {
                    if (continuouslyReading && !scanner.isReadPending())
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
