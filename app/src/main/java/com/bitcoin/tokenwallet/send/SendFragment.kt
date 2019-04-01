package com.bitcoin.tokenwallet.send


import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.bitcoin.tokenwallet.MyNavSubGraphFragment
import com.bitcoin.tokenwallet.R
import com.bitcoin.tokenwallet.ui.Event
import com.bitcoin.slpwallet.SLPWallet
import com.bitcoin.slpwallet.presentation.BalanceInfo
import com.bitcoin.slpwallet.presentation.ProgressTask
import com.bitcoin.slpwallet.presentation.TaskStatus
import com.bitcoin.slpwallet.presentation.getTokenNumberFormat
import timber.log.Timber
import java.math.BigDecimal
import kotlin.math.max


private data class BalanceInfoPlaceholder(
    override var tokenId: String,
    override var amount: BigDecimal,
    override var ticker: String?,
    override var name: String?,
    override var decimals: Int?) : BalanceInfo

class SendFragment : Fragment(), MyNavSubGraphFragment {


    private lateinit var addressInput: EditText
    private lateinit var mAmountInput: EditText
    private lateinit var mMessage: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var scanButton: AppCompatButton
    private lateinit var sendButton: Button
    private var tokenSpinner: Spinner? = null

    private val args: SendFragmentArgs by navArgs<SendFragmentArgs>()

    companion object {
        fun newInstance() = SendFragment()
    }

    private lateinit var viewModel: SendViewModel

    private val balanceObserver = Observer<List<BalanceInfo>> { balances: List<BalanceInfo>? ->
        Timber.d("Balance change detected, balances: ${balances?.count()}.")

        balances?.let {

            //val selectedItem: BalanceInfo? = getSelectedToken()
            //var selectedTokenId: String? = selectedItem?.tokenId
            val selectedTokenId = viewModel.selectedTokenId
            Timber.d("Balance change detected, selectedTokenId: $selectedTokenId")

            setUpSpinner(it, selectedTokenId)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_send, container, false)

        tokenSpinner = view.findViewById(R.id.tokenSpinner)
        tokenSpinner?.setOnItemSelectedListener(object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                viewModel.selectedTokenId = getSelectedToken()?.tokenId
                Timber.d("viewModel.selectedTokenId is now: ${viewModel.selectedTokenId}")
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //viewModel.selectedTokenId = getSelectedToken()?.tokenId
            }
        })

        mAmountInput = view.findViewById(R.id.amount)
        addressInput = view.findViewById(R.id.address)

        mMessage = view.findViewById(R.id.message)

        scanButton = view.findViewById<AppCompatButton>(R.id.scanButton)
        scanButton.setOnClickListener { scanView: View ->
            Timber.d("Scan clicked")

            view?.let {v: View ->
                val navController = Navigation.findNavController(v)
                viewModel.scan(navController)
            }
        }

        sendButton = view.findViewById(R.id.sendButton)
        sendButton.setOnClickListener( View.OnClickListener { view ->

            val selectedItem: BalanceInfo? = getSelectedToken()

            if (selectedItem == null) {
                Timber.d("No tokens to send")
                showDialog(resources.getString(R.string.send_error_no_tokens), false)
                return@OnClickListener
            }

            val tokenId: String = selectedItem.tokenId
            val amountText: String = mAmountInput.text.toString()
            val toAddress: String = addressInput.text.toString()

            context?.let {
                viewModel.send(
                    toAddress,
                    tokenId,
                    amountText,
                    it
                )
            }
        })

        progressBar = view.findViewById(R.id.sendProgress)
        return view
    }

    private fun getSelectedToken(): BalanceInfo? {
        val spinner: Spinner? = tokenSpinner
        if (spinner == null) {
            return null
        }

        var selectedItem: BalanceInfo? = spinner.selectedItem as? BalanceInfo
        val adapter: SpinnerAdapter? = spinner.adapter
        if (selectedItem == null && adapter != null && adapter.count > 0) {
            selectedItem = spinner.getItemAtPosition(0) as? BalanceInfo
        }

        if (selectedItem == null || selectedItem.tokenId.isBlank()) {
            return null
        } else {
            return selectedItem
        }
    }

    fun setSelectedToken(tokenId: String) {
        Timber.d("setSelectedToken() $tokenId")
        viewModel.selectedTokenId = tokenId

        val spinner: Spinner? = tokenSpinner
        if (spinner != null) {
            (spinner.adapter as? BlockieArrayAdapter)?.let { adapter: BlockieArrayAdapter ->
                adapter.indexOfTokenId(tokenId)?.let {
                    spinner.setSelection(it)

                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toAddress: String? = args.toAddress
        val addressForDisplay: String = if (toAddress.isNullOrBlank()) { "" } else toAddress
        addressInput.setText(addressForDisplay)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("onActivityCreated()")
        viewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)

        Timber.d("InitializedSendViewModel")
        viewModel.sendStartError.observe(this, Observer {event: Event<String?>? ->
            event?.let {ev: Event<String?> ->
                ev.getContentIfNotHandled()?.let { message: String ->
                    showDialog(message, false)
                }
            }
        })


        viewModel.sendStatus.observe(this, Observer { progress: ProgressTask<String?> ->
            Timber.d("Send status change detected: ${progress.status.name}.")

            setUIForSendingStatus(progress)
            when(progress.status) {

                TaskStatus.SUCCESS -> {
                    val txid: String? = progress.result
                    Timber.d("Send status had txid: $txid")
                    if (txid != null) {
                        showSuccessfullySentDialog(txid)
                    } else {
                        showDialog(resources.getString(R.string.sent), true)
                    }

                }
                TaskStatus.ERROR -> {
                    // TODO: Do this within the SDK
                    viewModel.refreshBalance()

                    showDialog(
                        progress.message ?: resources.getString(R.string.unknown_error),
                        false,
                        resources.getString(R.string.send_failed)
                    )
                }
                else -> {
                }
            }
        })
        Timber.d("Observing sendStatus.")



    }

    fun setUIForSendingStatus(progress: ProgressTask<String?>) {

        val isSending: Boolean = progress.status == TaskStatus.UNDERWAY

        tokenSpinner?.isEnabled = !isSending
        mAmountInput.isEnabled = !isSending
        addressInput.isEnabled = !isSending
        scanButton.isEnabled = !isSending
        sendButton.isEnabled = !isSending
        sendButton.text = if (isSending) "" else resources.getString(R.string.send)
        progressBar.visibility = if (isSending) View.VISIBLE else View.GONE
    }

    fun setUpSpinner(balances: List<BalanceInfo>, tokenIdToSelect: String?) {
        Timber.d("setUpSpinner")

        var selectedIndex: Int = 0

        val tokensOnly: MutableList<BalanceInfo> = balances.toMutableList()

        val bchIndex: Int = tokensOnly.indexOfFirst {balance: BalanceInfo ->
            balance.tokenId.isEmpty()
        }
        if (bchIndex >= 0) {
            tokensOnly.removeAt(bchIndex)
        }

        if (tokenIdToSelect != null) {
            selectedIndex = tokensOnly.indexOfFirst { balance: BalanceInfo ->
                balance.tokenId == tokenIdToSelect
            }
            selectedIndex = max(selectedIndex, 0)
        }

        Timber.d("Selectable tokens: ${tokensOnly.count()}")


        tokensOnly.forEach {
            val nf = getTokenNumberFormat(it.decimals, it.ticker)
            Timber.d("Balance: ${nf.format(it.amount)}")
        }

        if (tokensOnly.count() == 0) {

            tokensOnly.add(BalanceInfoPlaceholder("", BigDecimal(0), "", resources.getString(R.string.no_tokens), 0))
        }

        context?.let {
            val adapter: BlockieArrayAdapter = BlockieArrayAdapter(it, R.layout.fragment_choose_token_item_blockie, tokensOnly)

            tokenSpinner?.let { spinner: Spinner ->
                spinner.adapter = adapter
                spinner.setSelection(selectedIndex)
            }
        }

    }


    fun showDialog(message: String, isPositive: Boolean, title: String? = null) {
        val alertDialog: AlertDialog? = activity?.let { fActivity: FragmentActivity ->
            val builder = AlertDialog.Builder(fActivity)

            if (title != null) {
                builder.setTitle(title)
            }

            builder.setMessage(message)


            builder.apply {
                val buttonText: String = if (isPositive) resources.getString(R.string.ok) else resources.getString(R.string.dismss)
                setPositiveButton(buttonText,
                    DialogInterface.OnClickListener { dialog, id ->
                        // User clicked OK button
                        viewModel.clearSendStatus()
                    })

            }

            builder.create()
        }

       alertDialog?.show()
    }

    fun showSuccessfullySentDialog(txid: String) {
        val alertDialog: AlertDialog? = activity?.let { fActivity: FragmentActivity ->
            val builder = AlertDialog.Builder(fActivity)

            val message: String = resources.getString(R.string.sent)
            builder.setMessage(message)

            builder.apply {
                val explorerText: String = resources.getString(R.string.show_in_explorer)
                setNegativeButton(explorerText,
                    DialogInterface.OnClickListener { dialog, id ->
                        val url = "https://explorer.bitcoin.com/bch/tx/$txid"
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                    })

                val okText: String = resources.getString(R.string.ok)
                setPositiveButton(okText,
                    DialogInterface.OnClickListener { dialog, id ->
                        viewModel.clearSendStatus()
                    })

            }

            builder.create()
        }

        alertDialog?.show()
    }

    override fun onPause() {
        Timber.d("onPause()")
        onPauseOrNavigationHide()
        super.onPause()
    }

    private fun onPauseOrNavigationHide() {
        viewModel.stopScheduledBalanceRefresh()
        val selectedTokenId: String? = getSelectedToken()?.tokenId
        Timber.d("onPauseOrNavigationHide() selectedTokenId: $selectedTokenId")
        viewModel.selectedTokenId = selectedTokenId
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume(), isVisible: $isVisible")
        if (isVisible) {
            onResumeOrNavigationShow()
        }
    }

    private fun onResumeOrNavigationShow() {
        Timber.d("onResumeOrNavigationShow(), isVisible: $isVisible")

        activity?.let {
            viewModel.setWallet(SLPWallet.getInstance(it))
        }

        viewModel.refreshBalance()
        // Don't observe at app startup
        if (isVisible) {
            setUpActionBar()

            // Calls with the same tuple are ignored
            // https://developer.android.com/reference/android/arch/lifecycle/LiveData
            viewModel.balances.observe(this, balanceObserver)
            viewModel.startScheduledBalanceRefresh()
        }

        Timber.d("onResumeOrNavigationShow() setting viewModel.selectedTokenId")
        viewModel.selectedTokenId?.let {
            Timber.d("onResumeOrNavigationShow() setting viewModel.selectedTokenId to $it")
            setSelectedToken(it)
        }
    }

    override fun onNavigationHide() {
        Timber.d("onNavigationHide()")
        onPauseOrNavigationHide()
        viewModel.balances.removeObserver(balanceObserver)
    }

    override fun onNavigationShow() {
        Timber.d("onNavigationShow()")
        onResumeOrNavigationShow()
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")

        (activity as? AppCompatActivity)?.supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")
            it.setTitle(R.string.title_send)
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }

        setMenuVisibility(false)
    }

}
