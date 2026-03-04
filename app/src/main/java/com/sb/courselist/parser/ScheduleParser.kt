package com.sb.courselist.parser

import android.net.Uri

interface ScheduleParser {
    suspend fun parse(uri: Uri): ParseResult
}

