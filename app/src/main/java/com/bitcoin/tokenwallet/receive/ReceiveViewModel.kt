package com.bitcoin.tokenwallet.receive

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bitcoin.slpwallet.util.DefaultScheduler
import com.bitcoin.slpwallet.util.Scheduler
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {
    private val PREFS_FILE_NAME: String = "com.bitcoin.tokenwallet.ReceiveViewModel"
    private val PREFS_KEY_SHOW_SLP_ADDRESS: String = "showSlpAddress"
    private val QR_CODE_SIZE: Int = 256

    lateinit var slpAddress: String
    private set

    lateinit var bchAddress: String
    private set

    private val mShowSlpAddress: MutableLiveData<Boolean> = MutableLiveData()

    private val scheduler: Scheduler = DefaultScheduler

    private val mQrCodeBch: MutableLiveData<Bitmap?> = MutableLiveData<Bitmap?>().apply { null }
    val qrCodeBch: LiveData<Bitmap?> = mQrCodeBch

    private val mQrCodeSlp: MutableLiveData<Bitmap?> = MutableLiveData<Bitmap?>().apply { null }
    val qrCodeSlp: LiveData<Bitmap?> = mQrCodeSlp

    init {
        val prefs: SharedPreferences = getApplication<Application>().getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val showSlpAddress: Boolean = prefs.getBoolean(PREFS_KEY_SHOW_SLP_ADDRESS, true)
        mShowSlpAddress.value = showSlpAddress
    }

    val showSlpAddress: LiveData<Boolean>
        get() { return mShowSlpAddress}

    fun initialize(bchAddress: String, slpAddress: String) {
        setAddresses(bchAddress, slpAddress)
    }

    fun createQrCodes() {
        val barcodeEncoder = BarcodeEncoder()

        var slpImage: Bitmap? = null
        try {
            slpImage = barcodeEncoder.encodeBitmap(slpAddress, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mQrCodeSlp.postValue(slpImage)

        var bchImage: Bitmap? = null
        try {
            bchImage = barcodeEncoder.encodeBitmap(bchAddress, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mQrCodeBch.postValue(bchImage)
    }

    fun setAddresses(bchAddress: String, slpAddress: String) {
        this.bchAddress = bchAddress
        this.slpAddress = slpAddress
        scheduler.execute { createQrCodes() }
    }

    fun swapAddressDisplay() {
        val localShowSlp: Boolean? = mShowSlpAddress.value
        if (localShowSlp != null) {
            mShowSlpAddress.postValue(!localShowSlp)
        } else {
            mShowSlpAddress.postValue(true)
        }

    }

    override fun onCleared() {
        val prefs: SharedPreferences = getApplication<Application>().getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor =  prefs.edit()
        editor.putBoolean(PREFS_KEY_SHOW_SLP_ADDRESS, mShowSlpAddress.value ?: true)
        editor.apply()

        super.onCleared()
    }

}
