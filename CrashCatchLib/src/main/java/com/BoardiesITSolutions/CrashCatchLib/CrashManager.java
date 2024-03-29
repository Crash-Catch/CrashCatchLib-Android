package com.BoardiesITSolutions.CrashCatchLib;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.util.Log;
//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;

/**
 * Copyright (C) Chris Board - Boardies IT Solutions
 * August 2019
 * https://crashcatch.com
 * https://support.boardiesitsolutions.com
 */

class CrashManager implements ICrashCatchResultHandler, IInternalCrashCatchResponseHandler
{

    private HashMap<String, String> postData;
    private int initialiseRetryCount = 0;

    @Override
    public void retryCrashAfterInitialisation()
    {
        Log.d("CrashManager", "Retrying crash crash after initialisation. Length is: " + CrashCatch.retryCrashInfoQueue.size());
        if (CrashCatch.CrashCatchInitialised)
        {
            Log.d("CrashManager", "Crash catch is initialised so sending requests");
            if (CrashCatch.retryCrashInfoQueue.size() > 0) {
                for (int i = CrashCatch.retryCrashInfoQueue.size() - 1; i >= 0; i--) {
                    APIHandler apiHandler = new APIHandler(APIHandler.API_Call.SendCrash, this, this);
                    apiHandler.execute(CrashCatch.retryCrashInfoQueue.get(i));
                    CrashCatch.retryCrashInfoQueue.remove(i);
                }
            }
        }
        else
        {
            Log.d("CrashManager", "Crash catch is not initialised, not sending requests");
        }
    }

    @Override
    public void retryInitialisation()
    {
        CrashCatch.Initialise(CrashCatch.context, CrashCatch.APIKey, CrashCatch.ProjectID, CrashCatch.ProjectVersion);
    }

    protected enum CrashType {Handled, Unhandled}

    protected CrashManager()
    {
        postData = new HashMap<>();
    }



    protected void ReportCrash(Exception ex, CrashCatch.CrashSeverity crashSeverity)
    {
        postData.clear();
        ReportCrash(ex, crashSeverity, CrashType.Handled);
    }

    protected void ReportCrash(Exception ex, CrashCatch.CrashSeverity crashSeverity, CrashType crashType)
    {
        postData.clear();
        try
        {
            addCrashSeverityToPostData(crashSeverity);
            addDeviceDataToPostFields();
            if (crashType == CrashType.Handled)
            {
                postData.put("CrashType", "Handled");
            }
            else
            {
                postData.put("CrashType", "Unhandled");
            }
            postData.put("ExceptionType", ex.getClass().getName());
            postData.put("ExceptionMessage", ex.toString());

            StringWriter writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            postData.put("Stacktrace", writer.toString());
            parseStacktraceAndClassFileAndLineNoToPostdata(writer.toString());
            sendCrashData();
        }
        catch (InvalidCrashSeverityException e)
        {
            Log.e("CrashCatch", e.toString());
        }
    }

    protected void ReportCrash(Exception ex, CrashCatch.CrashSeverity crashSeverity, String key, String value)
    {
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(key, value);
            this.ReportCrash(ex, crashSeverity, jsonObject);
        }
        catch (JSONException ex1)
        {
            Log.e("CrashCatch", ex1.toString());
        }
    }

    protected void ReportCrash(Exception ex, CrashCatch.CrashSeverity crashSeverity, String key, int value)
    {
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(key, value);
            this.ReportCrash(ex, crashSeverity, jsonObject);
        }
        catch (JSONException ex1)
        {
            Log.e("CrashCatch", ex1.toString());
        }
    }

    protected void ReportCrash(Exception ex, CrashCatch.CrashSeverity crashSeverity, JSONObject jsonObject)
    {
        postData.clear();
        try
        {
            addCrashSeverityToPostData(crashSeverity);
            addDeviceDataToPostFields();
            postData.put("CrashType", "Handled");
            postData.put("ExceptionType", ex.getClass().getName());
            postData.put("ExceptionMessage", ex.toString());
            StringWriter writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            postData.put("Stacktrace", writer.toString());
            parseStacktraceAndClassFileAndLineNoToPostdata(writer.toString());
            postData.put("CustomProperty", jsonObject.toString());
            sendCrashData();
        }
        catch (InvalidCrashSeverityException e)
        {
            Log.e("CrashCatch", e.toString());
        }
    }

    protected void parseStacktraceAndClassFileAndLineNoToPostdata(String stacktrace)
    {
        if (CrashCatch.context == null)
        {
            return;
        }
        String packageName = CrashCatch.context.getPackageName();
        String[] stacktraceLines = stacktrace.split("\\r?\\n");

        String lineWithPackageName = "";
        for (int i = 0; i < stacktraceLines.length; i++)
        {
            if (stacktraceLines[i].contains(packageName))
            {
                lineWithPackageName = stacktraceLines[i];
                break;
            }
        }

        if (!lineWithPackageName.isEmpty())
        {
            int startOfClassAndLineNumber = lineWithPackageName.indexOf("(") + 1;

            String classAndLineNumber = lineWithPackageName.substring(startOfClassAndLineNumber).replace(")", "");
            String[] classAndLineNumberArray = classAndLineNumber.split(":");
            postData.put("ClassFile", classAndLineNumberArray[0]);
            postData.put("LineNo", classAndLineNumberArray[1]);
        }
        else
        {
            String lineWithClassAndLineNumber = "";
            for (int i = 0; i < stacktraceLines.length; i++)
            {
                if (stacktraceLines[i].contains(":") && stacktraceLines[i].contains(":"))
                {
                    lineWithClassAndLineNumber = stacktraceLines[i];
                    break;
                }
            }
            if (!lineWithClassAndLineNumber.isEmpty())
            {
                int startOfClassAndLineNumber = lineWithClassAndLineNumber.indexOf("(");
                String classAndLineNumber = lineWithClassAndLineNumber.substring(startOfClassAndLineNumber).replace(")", "");
                String[] classAndLineNumberArray = classAndLineNumber.split(":");
                postData.put("ClassFile", classAndLineNumberArray[0]);
                postData.put("LineNo", classAndLineNumberArray[1]);
            }
        }
    }

    private void addDeviceDataToPostFields()
    {
        try
        {
            //Get the Device ID
            postData.put("DeviceID", Helpers.getDeviceUID(CrashCatch.context));

            PackageInfo pInfo = CrashCatch.context.getPackageManager().getPackageInfo(CrashCatch.context.getPackageName(), 0);
            postData.put("VersionName", pInfo.versionName);
            postData.put("DeviceType", "Android");
            postData.put("ROMBuild", android.os.Build.DISPLAY);
            postData.put("KernelVersion", System.getProperty("os.version"));
            postData.put("DeviceBrand", android.os.Build.BRAND);
            postData.put("DeviceModel", android.os.Build.MODEL);
            postData.put("APILevel", String.valueOf(android.os.Build.VERSION.SDK_INT));
            postData.put("ScreenResolution", CrashCatch.context.getResources().getDisplayMetrics().widthPixels + " x " + CrashCatch.context.getResources().getDisplayMetrics().heightPixels);
            postData.put("Locale", Locale.getDefault().getDisplayLanguage().toString());
            TelephonyManager tMgr = (TelephonyManager) CrashCatch.context.getSystemService(CrashCatch.context.TELEPHONY_SERVICE);
            String mobileOperator = tMgr.getNetworkOperatorName();
            if (mobileOperator == null || mobileOperator.equals(""))
            {
                postData.put("MobileNetwork", "N/A");
            }
            else
            {
                postData.put("MobileNetwork", mobileOperator);
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void addCrashSeverityToPostData(CrashCatch.CrashSeverity severity) throws InvalidCrashSeverityException
    {
        switch (severity)
        {
            case Low:
                postData.put("Severity", "Low");
                break;
            case Medium:
                postData.put("Severity", "Medium");
                break;
            case High:
                postData.put("Severity", "High");
                break;
            default:
                throw new InvalidCrashSeverityException();
        }
    }

    private void sendCrashData()
    {
        try
        {

            postData.put("APIKey", CrashCatch.APIKey);
            postData.put("ProjectID", CrashCatch.ProjectID);
            if (CrashCatch.CrashCatchInitialised)
            {
                APIHandler apiHandler = new APIHandler(APIHandler.API_Call.SendCrash, this, this);
                apiHandler.execute(postData);
            }
            else
            {
                CrashCatch.retryCrashInfoQueue.add(postData);
                Log.e("CrashCatch", "CrashCatch not initialised. Call CrashCatch.Initialise(context, api_key, app_id) before sending a crash. Crash added to queue so will be sent upon successful initialisation");
            }
        }
        catch (NullPointerException ex)
        {
            Log.e("CrashCatch-CrashManager", ex.toString());
        }
    }

    @Override
    public void processResult(APIHandler.API_Call api_call, JSONObject resultObj)
    {
        Log.d("Crash Manager", "Processing result");
        if (resultObj != null)
        {
            Log.d("CrashCatch", resultObj.toString());

            try
            {
                if (resultObj.getInt("result") == APIHandler.API_NOT_INITIALISED)
                {
                    //Check was the library initialised already, if so, then probably nothing happened with this user
                    //for a while so engine removed session. Reinitialise and create a new session
                    if (CrashCatch.CrashCatchInitialised)
                    {
                        //Only attempt to reinitalise 3 times, make sure we don't get stuck in a loop.
                        if (initialiseRetryCount < 3)
                        {
                            initialiseRetryCount++;
                            CrashCatch.Initialise(CrashCatch.context, CrashCatch.APIKey, CrashCatch.ProjectID, CrashCatch.ProjectVersion, new ICrashCatchResultHandler()
                            {
                                @Override
                                public void processResult(APIHandler.API_Call api_call, JSONObject resultObj)
                                {
                                    //Now resend the crash

                                    sendCrashData();
                                    retryCrashAfterInitialisation();
                                }
                            });
                        }
                    }
                }
                else
                {
                    Log.e("CrashCatch", resultObj.getString("message"));
                    retryCrashAfterInitialisation();
                }
            }
            catch (JSONException e)
            {
                Log.e("CrashCatch", e.toString());
            }
        }
        else
        {
            Log.d("CrashCatch", "No json response");
        }
    }
}
