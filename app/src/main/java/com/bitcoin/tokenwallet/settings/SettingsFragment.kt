package com.bitcoin.tokenwallet.settings

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.tokenwallet.MyNavSubGraphFragment

import com.bitcoin.tokenwallet.R
import kotlinx.android.synthetic.main.restore_fragment.*
import kotlinx.android.synthetic.main.settings_fragment.*
import timber.log.Timber

class SettingsFragment : Fragment(), MyNavSubGraphFragment, SettingFragment.OnListFragmentInteractionListener {

    private lateinit var viewAdapter: MySettingRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    companion object {
        fun newInstance() = SettingsFragment()
    }

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.settings_fragment, container, false)

        var items: List<SettingsContent.SettingsItem> = listOf()
        context?.let {
            items = SettingsContent(it).ITEMS
        }

        viewAdapter = MySettingRecyclerViewAdapter(items, this)
        viewManager = LinearLayoutManager(activity)

        setHasOptionsMenu(false)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        // TODO: Use the ViewModel

        settingsList?.apply {
            Timber.d("Configuring Settings list.")
            setHasFixedSize(true)

            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            layoutManager = viewManager
            adapter = viewAdapter

            viewAdapter.notifyDataSetChanged()

        }
    }


    override fun onListFragmentInteraction(item: SettingsContent.SettingsItem?) {
        Timber.d("Setting selected")
        if (item != null) {
            findNavController().navigate(item.destinationNavId)
        }
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
            it.setTitle(R.string.title_settings)
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }
        setMenuVisibility(false)
    }

}
