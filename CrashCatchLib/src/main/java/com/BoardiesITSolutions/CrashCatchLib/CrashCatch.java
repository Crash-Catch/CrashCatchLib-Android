package com.BoardiesITSolutions.CrashCatchLib;

import android.content.Context;
import android.util.Log;
//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Copyright (C) Chris Board - Boardies IT Solutions
 * August 2019
 * https://crashcatch.com
 * https://support.boardiesitsolutions.com
 */

public class CrashCatch implements ICrashCatchResultHandler, IInternalCrashCatchResponseHandler
{
    protected static boolean CrashCatchInitialised = false;
    protected static String SessionID ;
    protected static Context context;

    @Override
    public void retryCrashAfterInitialisation()
    {
        //Not needed here
    }

    @Override
    public void retryInitialisation()
    {
        CrashCatch.Initialise(CrashCatch.context, CrashCatch.APIKey, CrashCatch.ProjectID, CrashCatch.ProjectVersion);
    }

    public enum CrashSeverity {Low, Medium, High}
    protected static String APIKey;
    protected static String ProjectID;
    protected static String ProjectVersion;
    private static ICrashCatchResultHandler iCrashCatchResultHandler = null;
    protected static Thread.UncaughtExceptionHandler systemUnhandledExceptionHandler = null;
    public static ArrayList<HashMap<String, String>> retryCrashInfoQueue = new ArrayList<>();


    private void InitialiseCrashCatch(Context context, String apiKey, String projectId, String projectVersion)
    {
        //this.setAPICall(API_Call.Initialise);
        //this.setICritiMonResultHandler(this);
        setUnhandledExceptionHandler();
        CrashCatch.context = context;
        CrashCatch.APIKey = apiKey;
        CrashCatch.ProjectID = projectId;
        CrashCatch.ProjectVersion = projectVersion;
        //List<NameValuePair> postData = new ArrayList<>();
        HashMap<String, String> postData = new HashMap<>();
        postData.put("APIKey", apiKey);
        postData.put("ProjectID", projectId);
        postData.put("DeviceID", Helpers.getDeviceUID(context));
        postData.put("ProjectVersion", projectVersion);
        APIHandler apiHandler = new APIHandler(APIHandler.API_Call.Initialise, this);
        apiHandler.execute(postData);
    }

    private void setUnhandledExceptionHandler()
    {
        final Thread.UncaughtExceptionHandler systemExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.UncaughtExceptionHandler myUnhandledExceptionHandler = new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, Throwable ex)
            {
                Log.e("CrashCatch", ((Exception) ex).toString(), ex);
                //CrashReporter.ReportUnhandledCrash(((Exception) ex));
                new CrashManager().ReportCrash((Exception) ex, CrashSeverity.High, CrashManager.CrashType.Unhandled);
                if (systemExceptionHandler != null)
                {
                    systemExceptionHandler.uncaughtException(thread, ex);
                }
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(myUnhandledExceptionHandler);

    }

    public static void Initialise(Context context, String apiKey, String projectId, String appVersion)
    {
        new CrashCatch().InitialiseCrashCatch(context, apiKey, projectId, appVersion);
    }

    public static void Initialise(Context context, String apiKey, String projectId, String appVersion, ICrashCatchResultHandler resultHandler)
    {
        iCrashCatchResultHandler = resultHandler;
        new CrashCatch().InitialiseCrashCatch(context, apiKey, projectId, appVersion);
    }

    public static void ReportCrash(Exception ex, CrashSeverity crashSeverity)
    {
        new CrashManager().ReportCrash(ex, crashSeverity);
    }

    public static void ReportCrash(Exception ex, CrashSeverity crashSeverity, String key, String value)
    {
        new CrashManager().ReportCrash(ex, crashSeverity, key, value);
    }

    public static void ReportCrash(Exception ex, CrashSeverity crashSeverity, JSONObject jsonObject)
    {
        new CrashManager().ReportCrash(ex, crashSeverity, jsonObject);
    }


    @Override
    public void processResult(APIHandler.API_Call api_call, JSONObject resultObj)
    {
        if (resultObj != null)
        {
            try
            {
                if (resultObj.getInt("result") == 0)
                {
                    CrashCatch.CrashCatchInitialised = true;

                    if (CrashCatch.iCrashCatchResultHandler != null)
                    {
                        CrashCatch.iCrashCatchResultHandler.processResult(api_call, resultObj);
                    }
                    CrashCatch.iCrashCatchResultHandler = null;
                }
                else
                {
                    Log.e("CrashCatch", "Failed to initialise. Error: " + resultObj.getString("message"));
                    CrashCatch.CrashCatchInitialised = false;
                }
            }
            catch (JSONException e)
            {
                Log.e("CrashCatch", "Error processing response: " + e.toString());
            }
        }
        else
        {
            Log.e("CrashCatch", "CrashCatch process result returned empty json object");
        }
    }
}
