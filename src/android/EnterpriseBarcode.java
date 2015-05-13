package com.symbol.enterprisebarcode;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.symbol.emdk.barcode.ScanDataCollection;

import android.content.Context;
import android.util.Log;

/**
 * Main interface class with the Cordova plugin, proxies all calls to the EMDK
 */
public class EnterpriseBarcode extends CordovaPlugin {
    public static final String LOG_TAG = "EnterpriseBarcode";
    private EMDKProxy emdkProxy = null;
    //  Need to maintain instance variables for the arguments as they are sent to sub classes
    private JSONObject m_arguments = null;
    private JSONObject m_argumentsToSetProps = null;

    /**
     * Static helper function to return a Failure to the user
     * @param callbackContext
     * @param message
     */
    public static void FailureCallback(CallbackContext callbackContext, String message)
    {
        if (callbackContext != null) {
            JSONObject failureMessage = new JSONObject();
            try {
                failureMessage.put("message", message);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Error");
            }
            callbackContext.error(failureMessage);
        }
    }

    /**
     * Static helper function to return a success message to the user
     * @param callbackContext
     * @param message
     */
    public static void SuccessCallback(CallbackContext callbackContext, String message)
    {
        if (callbackContext != null) {
            JSONObject successMessage = new JSONObject();
            try {
                successMessage.put("message", message);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Error");
            }
            callbackContext.success(successMessage);
        }
    }

    /**
     * Helper function for the Enable callback, can be called either to inform the user that
     * the scanner has finished enabling or that a barcode has just been scanned.
     * @param callbackContext
     * @param enableMessage
     */
    public static void EnableCallback(CallbackContext callbackContext, String enableMessage, String szData, ScanDataCollection.LabelType eType, String szTimestamp)
    {
        JSONObject scanDataResponse = new JSONObject();
        try {
            scanDataResponse.put("status", enableMessage);
            scanDataResponse.put("data", szData);
            scanDataResponse.put("type", eType);
            scanDataResponse.put("timestamp", szTimestamp);
        }
        catch (JSONException e)
        {}
        PluginResult result = new PluginResult(PluginResult.Status.OK, scanDataResponse);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    public EnterpriseBarcode() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

	public void onResume(boolean multitasking)
	{
		Log.d(LOG_TAG, "On Resume");
        //  EMDK seems reliable without having to re-enable it on resume
//        InitializeEMDKProxy(null);
	}
	
	public void onPause(boolean multitasking)
	{
		Log.d(LOG_TAG, "On Pause");
        //  EMDK Seems reliable without having to disable it on pause
//        emdkProxy.destroy();
	}

    /**
     * Attempt to initialise the EMDK, this will fail gracefully if the EMDK is not installed on the
     * device
     * @param callbackContext
     */
    public void InitializeEMDKProxy(CallbackContext callbackContext)
    {
        Context c = this.cordova.getActivity().getApplicationContext();
        if (EMDKProxy.isEMDKAvailable(c))
            emdkProxy = new EMDKProxy(c, callbackContext);
        else
            FailureCallback(callbackContext, "EMDK not available");
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(LOG_TAG, "Args: " + args.length());
        if (action.equals("initializeBarcode")) {
            JSONObject r = new JSONObject();
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    InitializeEMDKProxy(callbackContext);
                }
            });
        }
        else if (action.equals("enable"))
        {
            if (args.length() > 0)
            {
                //  Process arguments
                try {
                    JSONObject arguments = args.getJSONObject(0);
                    arguments = arguments.getJSONObject("options");
                    m_arguments = arguments;
                    Log.d(LOG_TAG, String.valueOf(arguments));
                }
                catch (JSONException je)
                {
                    callbackContext.error("Arguments is not a valid JSON object");
                }
            }
            Log.d(LOG_TAG, "Enable Scanner");
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ScannerEnable(callbackContext, m_arguments);
                }
            });
        }
        else if (action.equalsIgnoreCase("disable")) {
            Log.d(LOG_TAG, "Disable Scanner");
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ScannerDisable(callbackContext);
                }
            });
        }
        else if (action.equals("enumerate"))
        {
            Log.d(LOG_TAG, "Enumerate");
            JSONObject scanners = enumerate(callbackContext);
        }
        else if (action.equals("getProperties"))
        {
            Log.d(LOG_TAG, "Get Properties");
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ScannerGetProperties(callbackContext);
                }
            });
        }
        else if (action.equals("setProperties"))
        {
            Log.d(LOG_TAG, "Set Properties");
            if (args.length() > 0)
            {
                //  Process arguments
                m_argumentsToSetProps = null;
                try {
                    JSONObject arguments = args.getJSONObject(0);
                    arguments = arguments.getJSONObject("options");
                    m_argumentsToSetProps = arguments;
                    Log.d(LOG_TAG, String.valueOf(m_argumentsToSetProps));
                }
                catch (JSONException je)
                {
                    callbackContext.error("Arguments is not a valid JSON object");
                }
            }
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ScannerSetProperties(callbackContext, m_argumentsToSetProps);
                }
            });
        }
        else {

            return false;
        }
        return true;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    public JSONObject ScannerEnable(CallbackContext callbackContext, JSONObject userSpecifiedArgumentsToEnable)
    {
        if (emdkProxy == null || !emdkProxy.isReady())
        {
            Log.w(LOG_TAG, "EMDK Was not ready or not available");
            return null;
        }
        else
            return emdkProxy.enableScanner(callbackContext, userSpecifiedArgumentsToEnable);
    }

    public JSONObject ScannerDisable(CallbackContext callbackContext)
    {
        if (emdkProxy == null || !emdkProxy.isReady())
        {
            Log.w(LOG_TAG, "EMDK Was not ready or not available");
            return null;
        }
        else
            return emdkProxy.disableScanner(callbackContext);
    }

    public JSONObject ScannerGetProperties(CallbackContext callbackContext)
    {
        if (emdkProxy == null || !emdkProxy.isReady())
        {
            Log.w(LOG_TAG, "EMDK Was not ready or not available");
            return null;
        }
        else
            return emdkProxy.getProperties(callbackContext);
    }

        public void ScannerSetProperties(CallbackContext callbackContext, JSONObject userSpecifiedArgumentsToSetProps)
        {
            if (emdkProxy == null || !emdkProxy.isReady())
            {
                Log.w(LOG_TAG, "EMDK Was not ready or not available");
                return;
            }
            else
                emdkProxy.setProperties(callbackContext, userSpecifiedArgumentsToSetProps);
        }

    public JSONObject enumerate(CallbackContext callbackContext)
    {
        if (emdkProxy == null || !emdkProxy.isReady())
        {
            Log.w(LOG_TAG, "EMDK Was not ready or not available");
            return null;
        }
        else
            return emdkProxy.enumerateScanners(callbackContext);
    }

}





