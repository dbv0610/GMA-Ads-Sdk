package com.ads.app.gmasdk.control.config

import android.app.Application

class AzAdConfig(val application: Application) {

    companion object {
        const val ENVIRONMENT_DEVELOP = "develop"
        const val ENVIRONMENT_PRODUCTION = "production"
    }

    var isVariantDev: Boolean = false
    var adjustConfig: AdjustConfig? = null
    var taichiConfig: TaichiConfig = TaichiConfig(isEnableTaichi = true)
    var eventNamePurchase: String = ""
    var idAdResume: String? = null
    var listIdAdResume: List<String>? = null
    var listDeviceTest: List<String> = emptyList()
    var appAdId: String? = null

    constructor(application: Application, environment: String) : this(application) {
        isVariantDev = environment == ENVIRONMENT_DEVELOP
    }

    fun setEnvironment(environment: String) {
        isVariantDev = environment == ENVIRONMENT_DEVELOP
    }

    fun isEnableAdResume(): Boolean {
        return idAdResume?.isNotEmpty() == true || listIdAdResume?.isNotEmpty() == true
    }
}
