package com.bitcoin.tokenwallet.balances


import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.tokenwallet.MainActivity
import com.bitcoin.tokenwallet.MyNavSubGraphFragment
import com.bitcoin.tokenwallet.R
import com.bitcoin.slpwallet.SLPWallet
import com.bitcoin.slpwallet.presentation.BalanceInfo
import timber.log.Timber
import java.math.BigDecimal


class BalancesFragment : Fragment(), MyNavSubGraphFragment, TokenBalanceItemFragment.OnListFragmentInteractionListener {

    private lateinit var balancesList: RecyclerView
    private lateinit var viewAdapter: MyTokenBalanceItemRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var message: TextView

    companion object {
        fun newInstance() = BalancesFragment()
    }

    private lateinit var viewModel: BalancesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_balances, container, false)

        // Balances List
        viewManager = LinearLayoutManager(activity)
        viewAdapter = MyTokenBalanceItemRecyclerViewAdapter(ArrayList<BalanceInfo>(), this)

        balancesList = view.findViewById<RecyclerView>(R.id.balancesList).apply {
            setHasFixedSize(true)

            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            layoutManager = viewManager
            adapter = viewAdapter
        }

        message = view.findViewById<TextView>(R.id.message)
        setHasOptionsMenu(true)

        return view;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BalancesViewModel::class.java)
        context?.let {
            val slp: SLPWallet = SLPWallet.getInstance(it)
            viewModel.initialize(slp)
            Timber.d("Initialized BalancesViewModel")


            viewModel.balances.observe(this, Observer { data ->
                Timber.d("Balance change detected, token count: ${data.count()}.")

                var bchBalance: BalanceInfo? = null
                // Put BCH at the top
                try {
                    bchBalance = data.first() {
                        it.tokenId == ""
                    }
                } catch (e: NoSuchElementException) {
                    // nop
                }

                var dataAdded: List<BalanceInfo> = data;

                if (bchBalance != null) {
                    val mutableData: MutableList<BalanceInfo> = data.toMutableList()
                    mutableData.remove(bchBalance)
                    if (bchBalance.amount > BigDecimal.ZERO) {
                        mutableData.add(0, bchBalance)
                    }
                    //Timber
                    viewAdapter.setItems(mutableData)
                    dataAdded = mutableData
                } else {
                    viewAdapter.setItems(data)
                }


                viewAdapter.notifyDataSetChanged()

                if (dataAdded.count() > 0) {
                    message.visibility = View.GONE
                } else {
                    message.visibility = View.VISIBLE
                }

            })
        }
    }

    override fun onPause() {
        Timber.d("onPause()")
        onPauseOrNavigationHide()
        super.onPause()
    }

    private fun onPauseOrNavigationHide() {
        viewModel.stopScheduledBalanceRefresh()
        setMenuVisibility(false)
    }

    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()
        if (isVisible) {
            onResumeOrNavigationShow()
        }
    }

    private fun onResumeOrNavigationShow() {
        activity?.let {
            viewModel.setWallet(SLPWallet.getInstance(it))
            
        }
        viewModel.startScheduledBalanceRefresh()
        setUpActionBar()
        setMenuVisibility(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.wallet, menu)
    }



    override fun onListFragmentInteraction(balance: BalanceInfo) {
        Timber.d("List fragment interaction with token: ${balance.name} ${balance.tokenId}")

        val tokenId: String? = if (balance.tokenId.isNullOrBlank()) { null } else balance.tokenId
        if (tokenId != null) {
            val mainActivity: MainActivity? = activity as? MainActivity
            if (mainActivity != null) {
                Timber.d("MainActivity found")
                mainActivity.navigateToSendTab(tokenId)
            } else {
                Timber.d("MainActivity missing")
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.settingsFragment -> {

                val localView: View? = this.view
                if (localView != null) {
                    var nav: NavController? = null
                    try {
                        nav = Navigation.findNavController(localView)
                    } catch (e: IllegalStateException) {
                        Timber.d("Navigation controller not found on Balances.")
                    }

                    if (nav != null) {
                        nav.navigate(R.id.settingsFragment2)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationHide() {
        Timber.d("onNavigationHide()")
        onPauseOrNavigationHide()
    }

    override fun onNavigationShow() {
        Timber.d("onNavigationShow()")
        onResumeOrNavigationShow()
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")
            it.setTitle(R.string.title_wallet)

            // To see logo, need to display both home and logo
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }

    }


}
