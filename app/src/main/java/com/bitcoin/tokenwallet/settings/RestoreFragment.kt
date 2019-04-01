package com.bitcoin.tokenwallet.settings

import android.content.DialogInterface
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.bitcoin.tokenwallet.MyNavSubGraphFragment

import com.bitcoin.tokenwallet.R
import kotlinx.android.synthetic.main.backup_fragment.*
import kotlinx.android.synthetic.main.restore_fragment.*
import timber.log.Timber

class RestoreFragment : Fragment(), MyNavSubGraphFragment {

    companion object {
        fun newInstance() = RestoreFragment()
    }

    private lateinit var viewModel: RestoreViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view:View = inflater.inflate(R.layout.restore_fragment, container, false)

        val restoreButton: Button = view.findViewById<Button>(R.id.restoreButton)
        restoreButton.setOnClickListener {
            showConfirmation()
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RestoreViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
        if (isVisible) {
            onResumeOrNavigationShow()
        }
    }

    override fun onNavigationHide() {
        // nop
    }

    override fun onNavigationShow() {
        onResumeOrNavigationShow()
    }

    private fun onResumeOrNavigationShow() {
        setUpActionBar()
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")
            it.setTitle(R.string.title_restore)
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }
        setMenuVisibility(false)
    }

    fun showConfirmation() {
        val confirmationDialog: AlertDialog? = activity?.let { fActivity: FragmentActivity ->
            val builder = AlertDialog.Builder(fActivity)

            builder.setTitle(R.string.restore_wallet_confirmation_title)
            builder.setMessage(R.string.restore_wallet_confirmation_message)


            builder.apply {
                setNegativeButton(resources.getString(R.string.restore_wallet_confirmation_cancel),
                    DialogInterface.OnClickListener { dialog, id ->
                        Timber.d("Keep Old Wallet")
                        // nop
                    })

                setPositiveButton(resources.getString(R.string.restore_wallet_confirmation_ok),
                    DialogInterface.OnClickListener { dialog, id ->
                        Timber.d("New Wallet")
                        viewModel.restore(fActivity, phrase.text.toString(), findNavController())

                    })
            }

            builder.create()
        }

        confirmationDialog?.show()
    }

}
