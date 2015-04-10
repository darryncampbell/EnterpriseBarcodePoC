package com.symbol.enterprisebarcode;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;


public class EnterpriseBarcode extends CordovaPlugin {
    public static final String LOG_TAG = "EnterpriseBarcode";
    private ZebraAndroidExtensions zebraExtensions = null;
    private int userEnabledScanner = 0;

    public static void FailureCallback(CallbackContext callbackContext, String message)
    {
        JSONObject failureMessage = new JSONObject();
        try {
            failureMessage.put("message", message);
        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSON Error");
        }
        callbackContext.error(failureMessage);
    }

    /**
     * Constructor.
     */
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
        //  todo - start EMDK
        //  todo - need static log string
        InitializeZebraExtensions(null, userEnabledScanner);
        zebraExtensions.onResume();
	}
	
	public void onPause(boolean multitasking)
	{
		Log.d(LOG_TAG, "On Pause");
        //  todo - stop EMDK
        zebraExtensions.destroy();
	}

    public void InitializeZebraExtensions(CallbackContext callbackContext, int userEnabledScanner)
    {
        Context c = this.cordova.getActivity().getApplicationContext();
        if (ZebraAndroidExtensions.isEMDKAvailable(c))
        {
            //  Create the EMDK object
            zebraExtensions = new ZebraAndroidExtensions(c, callbackContext, userEnabledScanner);
        }
        else
        {
            FailureCallback(callbackContext, "EMDK not available");
        }
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
            //  todo - Initialise EMDK
            //  todo - get EMDK 3.0  jar
            //  todo - don't delete or overwrite my android platform code
            InitializeZebraExtensions(callbackContext, userEnabledScanner);
            //callbackContext.success(r);
        }
        else if (action.equals("enable"))
        {
            if (args.length() > 0)
            {
                //  Process arguments
                try {
                    JSONObject arguments = args.getJSONObject(0);
                    arguments = arguments.getJSONObject("options");
                    Log.d(LOG_TAG, String.valueOf(arguments));
                }
                catch (JSONException je)
                {
                    callbackContext.error("Arguments is not a valid JSON object");
                }
            }
            Log.d(LOG_TAG, "Enable Scanner");
            userEnabledScanner = 1;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ScannerEnable(callbackContext);
                }
            });
        }
        else if (action.equalsIgnoreCase("disable")) {
            Log.d(LOG_TAG, "Disable Scanner");
            userEnabledScanner = 0;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ScannerDisable(callbackContext);
                }
            });
        }
        else if (action.equals("enumerate"))
        {
            Log.d(LOG_TAG, "Enumerate");
            //  todo - return failure if could not enumerate or if the scanner is unavailable
            JSONObject scanners = enumerate();
            if (scanners == null)
            {
                JSONObject failureReturn = new JSONObject();
                failureReturn.put("message", "enumerate failed");
                callbackContext.error(failureReturn);
            }
            else
                callbackContext.success(scanners);
        }
        else {

            return false;
        }
        return true;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    public JSONObject ScannerEnable(CallbackContext callbackContext)
    {
        if (zebraExtensions == null)
        {
            //  EMDK could not be loaded
            return null;
        }
        else if (!zebraExtensions.isReady())
        {
            //  EMDK is not yet ready
            return null;
        }
        else
        {
            //  EMDK is ready, let's go
            return zebraExtensions.enableScanner(callbackContext);
        }
    }

    public JSONObject ScannerDisable(CallbackContext callbackContext)
    {
        if (zebraExtensions == null)
        {
            //  EMDK could not be loaded
            return null;
        }
        else if (!zebraExtensions.isReady())
        {
            //  EMDK is not yet ready
            return null;
        }
        else
        {
            //  EMDK is ready, let's go
            return zebraExtensions.disableScanner(callbackContext);
        }
    }

    public JSONObject enumerate()
    {
        if (zebraExtensions == null)
        {
            //  EMDK could not be loaded
            return null;
        }
        else if (!zebraExtensions.isReady())
        {
            //  EMDK is not yet ready
            return null;
        }
        else
        {
            //  EMDK is ready, let's go
            return zebraExtensions.enumerateScanners();
        }
    }

}





