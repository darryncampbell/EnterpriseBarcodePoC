package com.symbol.enterprisebarcode;

import android.util.Log;

import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is responsible for managing property setting and retrieval from the Scanner.
 * Only a subset of all properties are supported in this plugin by way of an example.
 */
public class ScannerConfiguration {
    public static final String LOG_TAG = "EnterpriseBarcode";

    /**
     * Before setting or retrieving properties it is necessary to create a configuration instance
     * @param emdkConfig The Configuration settings provided by the EMDK
     */
    public ScannerConfiguration(ScannerConfig emdkConfig)
    {
        this.emdkConfig = emdkConfig;
        code11Enabled = emdkConfig.decoderParams.code11.enabled;
        code128Enabled = emdkConfig.decoderParams.code128.enabled;
        code39Enabled = emdkConfig.decoderParams.code39.enabled;
        code93Enabled = emdkConfig.decoderParams.code93.enabled;
        dataMatrixEnabled = emdkConfig.decoderParams.dataMatrix.enabled;
        ean8Enabled = emdkConfig.decoderParams.ean8.enabled;
        ean13Enabled = emdkConfig.decoderParams.ean13.enabled;
        upcaEnabled = emdkConfig.decoderParams.upca.enabled;
        upce1Enabled = emdkConfig.decoderParams.upce1.enabled;
        pdf417Enabled = emdkConfig.decoderParams.pdf417.enabled;
    }

    /**
     * Return all properties available (supported) in a JSON object
     * @return
     */
    public JSONObject getJSON()
    {
        JSONObject currentProperties = new JSONObject();
        try{
            currentProperties.put("code11Enabled", code11Enabled);
            currentProperties.put("code128Enabled", code128Enabled);
            currentProperties.put("code39Enabled", code39Enabled);
            currentProperties.put("code93Enabled", code93Enabled);
            currentProperties.put("dataMatrixEnabled", dataMatrixEnabled);
            currentProperties.put("ean8Enabled", ean8Enabled);
            currentProperties.put("ean13Enabled", ean13Enabled);
            currentProperties.put("upcaEnabled", upcaEnabled);
            currentProperties.put("upce1Enabled", upce1Enabled);
            currentProperties.put("pdf417Enabled", pdf417Enabled);
        }
        catch (JSONException je) {}
        return currentProperties;
    }

    /**
     * Sets all newly specified objects to the EMDK and commits the setting.  Only supports
     * decoder properties (i.e. friendlyName is ignored)
     * @param scanner
     * @param arguments
     * @return
     * @throws ScannerException
     */
    public boolean setNewConfig(Scanner scanner, JSONObject arguments) throws ScannerException, JSONException
    {
            if (arguments == null)
                return false;
            if (arguments.has("code11Enabled")) {
                this.emdkConfig.decoderParams.code11.enabled = arguments.getBoolean("code11Enabled");
            }
            else if (arguments.has("code128Enabled")) {
                this.emdkConfig.decoderParams.code128.enabled = arguments.getBoolean("code128Enabled");
            }
            else if (arguments.has("code39Enabled")) {
                this.emdkConfig.decoderParams.code39.enabled = arguments.getBoolean("code39Enabled");
            }
            else if (arguments.has("code93Enabled")) {
                this.emdkConfig.decoderParams.code93.enabled = arguments.getBoolean("code93Enabled");
            }
            else if (arguments.has("dataMatrixEnabled")) {
                this.emdkConfig.decoderParams.dataMatrix.enabled = arguments.getBoolean("dataMatrixEnabled");
            }
            else if (arguments.has("ean8Enabled")) {
                this.emdkConfig.decoderParams.ean8.enabled = arguments.getBoolean("ean8Enabled");
            }
            else if (arguments.has("ean13Enabled")) {
                this.emdkConfig.decoderParams.ean13.enabled = arguments.getBoolean("ean13Enabled");
            }
            else if (arguments.has("upcaEnabled")) {
                this.emdkConfig.decoderParams.upca.enabled = arguments.getBoolean("upcaEnabled");
            }
            else if (arguments.has("upce1Enabled")) {
                this.emdkConfig.decoderParams.upce1.enabled = arguments.getBoolean("upce1Enabled");
            }
            else if (arguments.has("pdf417Enabled")) {
                this.emdkConfig.decoderParams.pdf417.enabled = arguments.getBoolean("pdf417Enabled");
            }

            scanner.setConfig(this.emdkConfig);
            return true;

    }

    private boolean code11Enabled;
    private boolean code128Enabled;
    private boolean code39Enabled;
    private boolean code93Enabled;
    private boolean dataMatrixEnabled;
    private boolean ean8Enabled;
    private boolean ean13Enabled;
    private boolean upcaEnabled;
    private boolean upce1Enabled;
    private boolean pdf417Enabled;
    private ScannerConfig emdkConfig;


}
