package com.bitcoin.tokenwallet.receive

import android.content.Context.CLIPBOARD_SERVICE
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.text.ClipboardManager
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.tokenwallet.MyNavSubGraphFragment
import com.bitcoin.tokenwallet.R
import com.bitcoin.slpwallet.SLPWallet
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_receive.*
import timber.log.Timber


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_BCH_ADDRESS = "bchAddress"
private const val ARG_SLP_ADDRESS = "slpAddress"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ReceiveFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ReceiveFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ReceiveFragment : Fragment(), MyNavSubGraphFragment {
    // TODO: Rename and change types of parameters
    private lateinit var mAddressText: TextView
    //private var bchAddress: String? = null
    //private var slpAddress: String? = null
    private lateinit var mQrCard: View
    private lateinit var mQrImage: ImageView
    private lateinit var mQrLogo: ImageView

    private lateinit var viewModel: ReceiveViewModel



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView()")
        // Inflate the layout for this fragment
       val view: View = inflater.inflate(R.layout.fragment_receive, container, false)

        mAddressText = view.findViewById<TextView>(R.id.address)
        //mAddressText.text = slpAddress
        mAddressText.setOnClickListener { v ->
            Timber.d("Address clicked")
            copyAddress(v)
        }

        val copyButton: Button = view.findViewById<Button>(R.id.copyButton)
        copyButton.setOnClickListener { v ->
            copyAddress(v)
        }

        mQrCard = view.findViewById<ImageView>(R.id.qrCard)

        mQrImage = view.findViewById<ImageView>(R.id.qrImage)
        mQrImage.setOnClickListener { v ->
            copyAddress(v)
        }

        mQrLogo = view.findViewById<ImageView>(R.id.qrLogo)
        mQrLogo.setOnClickListener { v ->
            copyAddress(v)
        }

        val swapButton: ImageButton = view.findViewById<ImageButton>(R.id.swapButton)
        swapButton.setOnClickListener { _ ->
            Timber.d("Swap button clicked")
            viewModel.swapAddressDisplay()
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Timber.d("onActivityCreated()")
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)


        context?.let {
            val slp = SLPWallet.getInstance(it)
            val bchAddress: String = slp.bchAddress
            val slpAddress: String = slp.slpAddress

            viewModel.initialize(bchAddress, slpAddress)
            Timber.d("Initialized ReceiveViewModel")

            viewModel.qrCodeBch.observe(this, Observer { qrCodeBch: Bitmap? ->
                val isSlp: Boolean = viewModel.showSlpAddress.value ?: true
                if (qrCodeBch != null && !isSlp) {
                    setQrImage(qrImage, isSlp, qrCodeBch, false)
                }
            })

            viewModel.qrCodeSlp.observe(this, Observer { qrCodeBch: Bitmap? ->
                val isSlp: Boolean = viewModel.showSlpAddress.value ?: true
                if (qrCodeBch != null && isSlp) {
                    setQrImage(qrImage, isSlp, qrCodeBch, false)
                }
            })

            viewModel.showSlpAddress.observe(this, Observer { isSlp ->
                Timber.d("Show SLP address changed to $isSlp.")

                setAddressWithoutQrCode(isSlp)
                setQrImage(isSlp, true)
            })

            val showSlp: Boolean = viewModel.showSlpAddress.value ?: true
            setAddressWithoutQrCode(showSlp)

        }
    }

    override fun onPause() {
        Timber.d("onPause()")
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
        if (isVisible) {
            onResumeOrNavigationShow()
        }
    }

    fun copyAddress(view: View) {
        val clipboardManager: ClipboardManager? = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboardManager != null) {
            //val clipData: ClipData = ClipData.newPlainText("Source Text", slpAddress)
            clipboardManager.text = if (viewModel.showSlpAddress.value ?: true) viewModel.slpAddress else viewModel.bchAddress
            Timber.d("Copied")

            val message: String? = context?.getString(R.string.address_copied_to_clipboard)
            if (message != null) {
                val mySnackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                mySnackbar.show()
            }
        }
    }

    fun displayTextFromAddress(addressWithPrefix: String?): SpannableString? {
        if (addressWithPrefix != null) {
            val parts: List<String> = addressWithPrefix.split(":")
            if (parts.count() > 1) {
                val addr: String = parts[1]
                if (addr.length > 8) {
                    val formatted: SpannableString = SpannableString(addr)
                    val localActivity: FragmentActivity? = activity
                    if (localActivity != null) {
                        val endColor: Int = ContextCompat.getColor(localActivity.applicationContext, R.color.main)
                        val len: Int = addr.count()
                        formatted.setSpan(ForegroundColorSpan(endColor), 0, 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        formatted.setSpan(ForegroundColorSpan(endColor), len - 4, len, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }

                    return formatted
                }
                return SpannableString(addressWithPrefix);
            } else {
                return SpannableString(addressWithPrefix)
            }
        } else {
            return null
        }
    }

    private fun setAddressWithoutQrCode(isSlp: Boolean) {
        var title: String = getString(R.string.title_receive_slp)
        var logoResourceId: Int = R.drawable.logo_slp_white
        val slpAddress: String = viewModel.slpAddress
        var addressText: SpannableString? = displayTextFromAddress(slpAddress)

        if (!isSlp) {
            title = getString(R.string.title_receive_bch)
            logoResourceId = R.drawable.logo_bch
            val bchAddress = viewModel.bchAddress
            addressText = displayTextFromAddress(bchAddress)
        }

        (activity as? AppCompatActivity)?.supportActionBar?.title = title
        mQrLogo.setImageResource(logoResourceId)
        mAddressText.text = addressText
    }

    fun setQrImage(qrView: ImageView, isSlp: Boolean, source: Bitmap, animate: Boolean) {
        val displayWidth = Resources.getSystem().getDisplayMetrics().widthPixels

        val distance: Float = displayWidth.toFloat()
        val duration: Long = 100

        val offScreenDestination: Float = if (isSlp) -distance else distance

        if (animate) {
            if (animate) {
                // Animate Out
                mQrCard.translationX = 0f
                mQrCard.animate().withLayer()
                    .translationX(offScreenDestination)
                    .setDuration(duration)
                    .withEndAction(
                        object: Runnable {
                            override fun run() {

                                qrView.setImageBitmap(source)

                                // Animate In
                                mQrCard.setTranslationX(-offScreenDestination);
                                mQrCard.animate().withLayer()
                                    .translationX(0f)
                                    .setDuration(duration)
                                    .start();
                            }
                        }
                    ).start();
            }
        } else {
            qrView.setImageBitmap(source)
        }
    }

    fun setQrImage(isSlp: Boolean, animate: Boolean) {
        Timber.d( "setQrImage(animate: $animate)")
        val localBchQrCode: Bitmap? = viewModel.qrCodeBch.value
        val localQrImage: ImageView? = mQrImage
        val localSlpQrCode: Bitmap? = viewModel.qrCodeSlp.value

        if (localQrImage != null) {
            if (isSlp && localSlpQrCode !=  null) {
                setQrImage(localQrImage, isSlp, localSlpQrCode, animate)
            } else if (!isSlp && localBchQrCode != null) {
                setQrImage(localQrImage, isSlp, localBchQrCode, animate)
            }

        } else {
            Timber.d("Not ready to show QR code.")
        }
    }

    override fun onNavigationHide() {
        // nop
    }

    override fun onNavigationShow() {
        Timber.d("onNavigationShow()")
        onResumeOrNavigationShow()
    }

    private fun onResumeOrNavigationShow() {
        val displayIsSlp: Boolean = viewModel.showSlpAddress.value ?: true
        val titleId: Int = if (displayIsSlp)  R.string.title_receive_slp  else  R.string.title_receive_bch
        activity?.setTitle(titleId)
        setUpActionBar()

        context?.let {
            val slp: SLPWallet = SLPWallet.getInstance(it)
            val bchAddress: String = slp.bchAddress
            val slpAddress: String = slp.slpAddress

            viewModel.setAddresses(bchAddress, slpAddress)
            setAddressWithoutQrCode(viewModel.showSlpAddress.value ?: true)
        }
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")
        val displayIsSlp: Boolean = viewModel.showSlpAddress.value ?: true
        val titleId: Int = if (displayIsSlp)  R.string.title_receive_slp  else  R.string.title_receive_bch

        (activity as? AppCompatActivity)?.supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")

            it.setTitle(titleId)
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }
        setMenuVisibility(false)
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ReceiveFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            ReceiveFragment().apply {
                context?.let {
                    val slp: SLPWallet = SLPWallet.getInstance(it)
                    arguments = Bundle().apply {
                        putString(ARG_BCH_ADDRESS, slp.bchAddress)
                        putString(ARG_SLP_ADDRESS, slp.slpAddress)
                    }
                }

            }
    }
}
