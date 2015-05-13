package com.symbol.enterprisebarcode;
import android.content.Context;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * The EMDK Proxy class is responsible for determining whether the EMDK is installed on the
 * target device and failing gracefully if it is not installed.
 */
public class EMDKProxy implements Serializable {

    private static EMDKScannerInterface emdk = null;
    private static Context context;
    private static final String LOG_TAG = "Symbol Barcode";

    public EMDKProxy(Context c, CallbackContext callbackContext)
    {
        if (isEMDKAvailable(c))
        {
            //  Create the EMDK object
            emdk = new EMDKScannerInterface(c, true, callbackContext);
            this.context = c;
        }
        else
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
    }

    //  Try to link to the EMDK on the device and if it is not there, fail gracefully
    public static boolean isEMDKAvailable(Context c)
    {
        try {

            EMDKScannerInterface test = new EMDKScannerInterface(c, false, null);
            Log.i(LOG_TAG, "EMDK is available on this device");
            return true;
        }
        catch (NoClassDefFoundError e)
        {
            Log.w(LOG_TAG, "EMDK is not available on this device");
            return false;
        }
    }

    //  Return the EMDK IsReady() method, true if the EMDK is reporting it is ready
    public boolean isReady()
    {
        if (emdk != null)
            return emdk.IsReady();
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

    /**
     * Public API calls from Plugin interface, pass through to emdk object.
     */
    public JSONObject enumerateScanners(CallbackContext callbackContext)
    {
        if (emdk != null)
            return emdk.enumerateScanners(callbackContext);
        else
            return null;
    }

    /**
     * Public API calls from Plugin interface, pass through to emdk object.
     * @param callbackContext
     * @param userSpecifiedArgumentsToEnable
     * @return
     */
    public JSONObject enableScanner(CallbackContext callbackContext, JSONObject userSpecifiedArgumentsToEnable) {
        if (emdk != null)
            return emdk.enableScanner(callbackContext, userSpecifiedArgumentsToEnable);
        else {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return null;
        }
    }

    /**
     * Public API calls from Plugin interface, pass through to emdk object.
     * @param callbackContext
     * @return
     */
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

    /**
     * Public API calls from Plugin interface, pass through to emdk object.
     * @param callbackContext
     * @return
     */
    public JSONObject getProperties(CallbackContext callbackContext)
    {
        if (emdk != null)
        {
            emdk.getProperties(callbackContext);
            return null;
        }
        else
        {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return null;
        }
    }

    /**
     * Public API calls from Plugin interface, pass through to emdk object.
     * @param callbackContext
     * @param arguments
     */
    public void setProperties(CallbackContext callbackContext, JSONObject arguments)
    {
        if (emdk != null)
            emdk.setProperties(callbackContext, arguments);
        else {
            EnterpriseBarcode.FailureCallback(callbackContext, "EMDK is not available");
            return;
        }
    }
}


