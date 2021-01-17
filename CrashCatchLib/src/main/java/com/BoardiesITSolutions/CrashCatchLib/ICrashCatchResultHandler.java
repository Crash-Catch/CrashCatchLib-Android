package com.BoardiesITSolutions.CrashCatchLib;

import org.json.JSONObject;

/**
 * Copyright (C) Chris Board - Boardies IT Solutions
 * August 2019
 * https://crashcatch.com.com
 * https://support.boardiesitsolutions.com
 */

public interface ICrashCatchResultHandler
{
    void processResult(APIHandler.API_Call api_call, JSONObject resultObj);

}

