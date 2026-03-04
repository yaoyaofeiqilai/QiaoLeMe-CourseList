package com.sb.courselist

import android.app.Application
import com.sb.courselist.di.AppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CourseListApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        appContainer = AppContainer(this)
    }
}

