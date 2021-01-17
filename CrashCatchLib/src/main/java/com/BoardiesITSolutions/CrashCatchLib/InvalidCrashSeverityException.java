package com.BoardiesITSolutions.CrashCatchLib;

/**
 * Copyright (C) Chris Board - Boardies IT Solutions
 * August 2019
 * https://crashcatch.com
 * https://support.boardiesitsolutions.com
 */

public class InvalidCrashSeverityException extends Exception
{
    public InvalidCrashSeverityException()
    {
        super("Invalid crash severity provided. Use CrashCatch.CrashSeverity enum");
    }
}
