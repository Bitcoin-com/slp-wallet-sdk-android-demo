package com.bitcoin.tokenwallet.balances

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.slpwallet.SLPWallet
import com.bitcoin.slpwallet.presentation.BalanceInfo
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BalancesViewModel : ViewModel() {

    private lateinit var slp: SLPWallet

    lateinit var balances: LiveData<List<BalanceInfo>>
    private set

    private val scheduledPool: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private var scheduledFuture: ScheduledFuture<*>? = null


    fun initialize(slpWallet: SLPWallet) {
        setWallet(slpWallet)
    }

    fun setWallet(slpWallet: SLPWallet) {
        slp = slpWallet
        balances = slp.balance // How to trigger getting the latest data here?
        startScheduledBalanceRefresh()
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
        stopScheduledBalanceRefresh()
        scheduledPool.shutdown()
        super.onCleared()
    }

}
