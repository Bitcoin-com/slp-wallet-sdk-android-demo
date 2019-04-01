package com.bitcoin.tokenwallet.settings

import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController
import com.bitcoin.tokenwallet.R
import com.bitcoin.slpwallet.Network
import com.bitcoin.slpwallet.SLPWallet

class RestoreViewModel : ViewModel() {

    fun restore(activity: Activity, mnemonicPhrase: String, navController: NavController) {
        if (mnemonicPhrase.isBlank()) {
            showDialog(activity, activity.resources.getString(R.string.restore_error_phrase_blank))
            return
        }

        val words = mnemonicPhrase.split(" ")
        if (words.count() < 12) {
            showDialog(activity, activity.resources.getString(R.string.restore_error_insufficient_words))
            return
        }


        try {
            val wallet: SLPWallet = SLPWallet.fromMnemonic(activity, Network.MAIN, mnemonicPhrase, true)

        } catch (e: IllegalArgumentException) {
            val message: String = activity.resources.getString(R.string.restore_error_failed)
            showDialog(activity, "$message\n\n${e.message}")
            return
        }
        // TODO: Other errors to catch?

        navController.navigate(R.id.balancesFragment2)
    }


    fun showDialog(activity: Activity, message: String) {
        val builder = AlertDialog.Builder(activity)

        builder.setMessage(message)

        builder.apply {
            setPositiveButton(activity.resources.getString(R.string.dismss),
                DialogInterface.OnClickListener { dialog, id ->
                    // nop
                })

        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

}
