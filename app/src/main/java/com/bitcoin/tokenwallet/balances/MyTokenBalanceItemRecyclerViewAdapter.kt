package com.bitcoin.tokenwallet.balances

import android.app.Activity
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.tokenwallet.R
import com.bitcoin.tokenwallet.balances.TokenBalanceItemFragment.OnListFragmentInteractionListener
import com.bitcoin.slpwallet.presentation.BalanceInfo
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import kotlinx.android.synthetic.main.fragment_tokenbalanceitem.view.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class MyTokenBalanceItemRecyclerViewAdapter(
    private var mBalances: List<BalanceInfo>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<MyTokenBalanceItemRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as BalanceInfo
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_tokenbalanceitem, parent, false)
        return ViewHolder(view)
    }

    /**
     * Because of bug in Badger Wallet expecting the token id to be an address prefixed with "bitcoincash:"
     */
    fun blockieAddressFromTokenId(tokenId: String): String {
        return tokenId.slice(IntRange(12, tokenId.count() - 1))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tokenBalance = mBalances[position]
        //holder.mAmountView.text = tokenBalance.amount.toString()
        holder.mNameView.text = tokenBalance.name


        if (tokenBalance.tokenId == "" && tokenBalance.ticker == "BCH") {
            holder.mIcon.visibility = View.VISIBLE
            holder.mIdenticon.visibility = View.INVISIBLE

            holder.mIcon.setImageResource(R.drawable.logo_bch)

        } else {
            holder.mIcon.visibility = View.GONE
            holder.mIdenticon.visibility = View.VISIBLE

            holder.mIdenticon.setAddress(blockieAddressFromTokenId(tokenBalance.tokenId))
            val radius: Float = holder.itemView.resources.getDimension(R.dimen.token_list_blockie_radius)
            holder.mIdenticon.setCornerRadius(radius)
        }

        val nf: NumberFormat = getTokenNumberFormat(tokenBalance.decimals, tokenBalance.ticker)
        holder.mAmountView.text = nf.format(tokenBalance.amount)

        holder.mTickerView.text = tokenBalance.ticker
        holder.mFiatValueView.text = ""

        /*
        if (tokenBalance.decimals != null) {
            val nf: NumberFormat = NumberFormat.getCurrencyInstance()
            val decimalFormatSymbols = (nf as DecimalFormat).getDecimalFormatSymbols()
            decimalFormatSymbols.currencySymbol = tokenBalance.ticker ?: ""
            nf.maximumFractionDigits = tokenBalance.decimals

            nf.decimalFormatSymbols = decimalFormatSymbols
            holder.mAmountView.text = nf.format(tokenBalance.amount)
        }
        */


        with(holder.mView) {
            tag = tokenBalance
            setOnClickListener(mOnClickListener)
        }
    }

    fun getTokenNumberFormat(decimals: Int?, ticker: String?): NumberFormat {

        val nf: NumberFormat = NumberFormat.getCurrencyInstance()
        nf.isGroupingUsed = true

        val decimalFormat: DecimalFormat? = nf as? DecimalFormat
        if (decimalFormat != null) {
            var decimalFormatSymbols: DecimalFormatSymbols = decimalFormat.decimalFormatSymbols
            decimalFormatSymbols.currencySymbol = if (ticker != null) { " ${ticker} " } else { "" }
            nf.decimalFormatSymbols = decimalFormatSymbols
        }

        nf.maximumFractionDigits = decimals ?: 0
        nf.minimumFractionDigits = decimals ?: 0


        return nf
    }

    override fun getItemCount(): Int = mBalances.size

    fun setItems(balances: List<BalanceInfo>) {
        mBalances = balances
    }


    fun generateIdenticon(view: View, hash: ByteArray, image_width: Int, image_height: Int): Bitmap {
        val width = 5
        val height = 5

        //val hash = text.toByteArray()

        val metrics: DisplayMetrics = DisplayMetrics()
        (view.context as Activity).windowManager.defaultDisplay.getMetrics(metrics)


        val identicon: Bitmap = Bitmap.createBitmap(metrics, width, height, Bitmap.Config.ARGB_8888)
        //val raster = identicon.getRaster()

        //val background = intArrayOf(255, 255, 255, 0)
        val background = 0
        //val foreground = intArrayOf(hash[0].toInt() and 255, hash[1].toInt() and 255, hash[2].toInt() and 255, 255)
        val foreground = ((hash[0].toInt() and 0xff) shl 24) + ((hash[1].toInt() and 0xff) shl 16) + ((hash[2].toInt() and 0xff) shl 8) + 255

        for (x in 0 until width) {
            //Enforce horizontal symmetry
            val i = if (x < 3) x else 4 - x
            for (y in 0 until height) {
                val pixelColor: Int
                //toggle pixels based on bit being on/off
                if (hash[i].toInt() shr y and 1 == 1)
                    pixelColor = foreground
                else
                    pixelColor = background
                identicon.setPixel(x, y, pixelColor)
            }
        }

        /*
        var finalImage = BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_ARGB)

        //Scale image to the size you want
        val at = AffineTransform()
        at.scale((image_width / width).toDouble(), (image_height / height).toDouble())
        val op = AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        finalImage = op.filter(identicon, finalImage)
        */

        return identicon
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIcon: ImageView = mView.icon
        val mIdenticon: BlockiesIdenticon = mView.identicon
        val mNameView: TextView = mView.name
        val mAmountView: TextView = mView.amount
        val mFiatValueView: TextView = mView.fiatValue
        val mTickerView: TextView = mView.ticker



        override fun toString(): String {
            return super.toString() + " '" + mNameView.text + "'"
        }


    }
}
