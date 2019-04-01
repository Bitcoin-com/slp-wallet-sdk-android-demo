package com.bitcoin.tokenwallet.settings

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.bitcoin.tokenwallet.MyNavSubGraphFragment

import com.bitcoin.tokenwallet.R
import com.bitcoin.slpwallet.SLPWallet
import kotlinx.android.synthetic.main.backup_fragment.*
import timber.log.Timber

class BackupFragment : Fragment(), MyNavSubGraphFragment {

    companion object {
        fun newInstance() = BackupFragment()
    }

    private lateinit var viewModel: BackupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.backup_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BackupViewModel::class.java)
        // TODO: Use the ViewModel

        context?.let {
            val slpWallet: SLPWallet = SLPWallet.getInstance(it)
            val words: List<String> = slpWallet.mnemonic
            val phrase: String = words.joinToString(" ")
            mnemonic.text = phrase
        }

        setUpActionBar()
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")
            it.setTitle(R.string.title_backup)
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }
        setMenuVisibility(false)
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
}
