package com.BoardiesITSolutions.CrashCatchLib;

/**
 * Copyright (C) Chris Board - Boardies IT Solutions
 * August 2019
 * https://critimon.com
 * https://support.boardiesitsolutions.com
 */

public interface IInternalCrashCatchResponseHandler
{
    void retryCrashAfterInitialisation();
    void retryInitialisation();
}
