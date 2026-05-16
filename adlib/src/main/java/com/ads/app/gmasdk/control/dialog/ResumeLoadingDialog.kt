package com.ads.app.gmasdk.control.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.ads.app.gmasdk.R

class ResumeLoadingDialog(context: Context) : Dialog(context, R.style.AppTheme) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_resume_loading)
    }
}
