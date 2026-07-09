package com.stealthcrypt.app

import android.app.Application
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.stealthcrypt.crypto.StealthCrypto

class StealthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StealthCrypto.init(LazySodiumAndroid(SodiumAndroid()))
    }
}
