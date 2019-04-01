package com.bitcoin.tokenwallet.send

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.bitcoin.tokenwallet.R
import com.bitcoin.tokenwallet.ui.Event
import com.bitcoin.slpwallet.Network
import com.bitcoin.slpwallet.SLPWallet
import com.bitcoin.slpwallet.address.AddressFormatException
import com.bitcoin.slpwallet.address.AddressSLP
import com.bitcoin.slpwallet.presentation.BalanceInfo
import com.bitcoin.slpwallet.presentation.ProgressTask
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class SendViewModel(application: Application) : AndroidViewModel(application) {
    private val PREFS_FILE_NAME: String = "com.bitcoin.tokenwallet.send.SendViewModel"
    private val PREF_KEY_SELECTED_TOKEN_ID: String = "selectedToken"

    private var slp: SLPWallet = SLPWallet.getInstance(application)

    var balances: LiveData<List<BalanceInfo>> = slp.balance

    private val prefs: SharedPreferences = application.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    var _selectedTokenId: String? = prefs.getString(PREF_KEY_SELECTED_TOKEN_ID, null)
    var selectedTokenId: String?
        get() = _selectedTokenId
        set(value) {
            _selectedTokenId = value
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putString(PREF_KEY_SELECTED_TOKEN_ID, selectedTokenId)
            editor.apply()
        }

    private var mSendStartError: MutableLiveData<Event<String?>> = MutableLiveData<Event<String?>>().apply { Event<String?>(null) }
    val sendStartError: LiveData<Event<String?>>
        get() = mSendStartError

    var sendStatus: LiveData<ProgressTask<String?>> = slp.sendStatus
        private set

    // Sometimes it takes a while for the balance to catch up, so checking it immediately after sending is not
    // sufficient
    private val scheduledPool: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var scheduledFuture: ScheduledFuture<*>? = null

    private val compositeDisposable = CompositeDisposable()


    fun clearSendStatus() {
        slp.clearSendStatus()
    }

    fun refreshBalance() {
        slp.refreshBalance()
    }

    fun scan(navController: NavController) {
        navController.navigate(R.id.scanFragment2)
    }

    fun send(toAddress: String, tokenId: String, amountText: String, context: Context) {

        if (amountText.isBlank()) {
            Timber.d("No amount.")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_no_amount)))
            return
        }

        var amount: BigDecimal? = null
        try {
            amount = BigDecimal(amountText)
            Timber.d("Sending $amount")
        } catch (e: NumberFormatException) {
            Timber.e("Amount \"$amountText\" not a valid amount. ${e.message}")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_amount_invalid)))
            return
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            Timber.e("Amount \"$amountText\" not greater than zero.")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_amount_not_positive)))
            return
        }

        val balances: List<BalanceInfo>? = balances.value
        if (balances == null) {
            Timber.e("Current balance unavailable, balances list was null.")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_balance_null)))
            return
        }

        var tokenBalance: BalanceInfo? = null
        try {
            tokenBalance = balances.first {
                it.tokenId == tokenId
            }
        } catch (e: NoSuchElementException) {
            Timber.e("Current balance unavailable, balances list was null.")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_balance_null)))
            return
        }

        if (amount.compareTo(tokenBalance.amount) > 0) {
            Timber.e("Insufficient balance.")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_balance_insufficient)))
            return
        }

        val decimals: Int? = tokenBalance.decimals
        if (decimals != null) {
            try {
                amount.setScale(decimals)
            } catch (e: ArithmeticException) {
                Timber.e("Too many decimals in amount.")
                mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_too_many_decimals)))
                return
            }
        }

        val trimmedAddress = toAddress.trim()
        try {
            AddressSLP.parse(Network.MAIN, trimmedAddress)
        } catch (e: AddressFormatException) {
            Timber.e("Address \"$trimmedAddress\" was not a valid SLP address.")
            mSendStartError.postValue(Event<String?>(context.getString(R.string.send_error_address_invalid)))
            return
        }

        slp.sendToken(tokenId, amount, toAddress)
            .subscribeOn(Schedulers.io())
            .subscribe(
            { txid: String ->
                Timber.d("sendToken() was successful, with txid: $txid")
            },
            { e: Throwable ->
                Timber.e("Error when sending. $e")
            }
        ).addTo(compositeDisposable)

    }

    fun setWallet(slpWallet: SLPWallet) {
        slp = slpWallet
        balances = slp.balance
        sendStatus = slp.sendStatus
        slp.refreshBalance()
    }

    fun startScheduledBalanceRefresh() {
        val future: ScheduledFuture<*>? = scheduledFuture
        if (future == null || future.isCancelled) {
            scheduledFuture = scheduledPool.scheduleWithFixedDelay(Runnable {
                Timber.d("Starting scheduled balance refresh.")
                slp.refreshBalance()
            }, 0, 15, TimeUnit.SECONDS)
        } else {
            Timber.d("Balance refresh was already scheduled.")
        }

    }

    fun stopScheduledBalanceRefresh() {
        scheduledFuture?.let {
            it.cancel(false)
        }
    }

    override fun onCleared() {
        Timber.d("onCleared()")
        stopScheduledBalanceRefresh()
        scheduledPool.shutdown()
        compositeDisposable.dispose()
        super.onCleared()
    }
}
