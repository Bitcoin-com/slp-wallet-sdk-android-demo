package com.bitcoin.tokenwallet.send

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.bitcoin.tokenwallet.R
import com.bitcoin.slpwallet.presentation.BalanceInfo
import com.bitcoin.slpwallet.presentation.blockieAddressFromTokenId
import com.bitcoin.slpwallet.presentation.getTokenNumberFormat
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import java.text.NumberFormat

class BlockieArrayAdapter(context: Context, private val resource: Int, private val objects: List<BalanceInfo>): ArrayAdapter<BalanceInfo>(context, resource, objects) {



    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }


    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val blockie: BlockiesIdenticon = view.findViewById(R.id.blockie)
        val nameView: TextView = view.findViewById(R.id.name)
        val balanceView: TextView = view.findViewById(R.id.balance)

        val item: BalanceInfo? = getItem(position)
        if (item != null) {

            nameView.text = item.name ?: item.tokenId
            if (item.tokenId.isNullOrEmpty()) { // Displaying a placeholder for no tokens
                balanceView.text = ""
                blockie.visibility = View.INVISIBLE
            } else {
                val nf: NumberFormat = getTokenNumberFormat(item.decimals, item.ticker)
                balanceView.text = "${nf.format(item.amount)}"

                blockie.setAddress(blockieAddressFromTokenId(item.tokenId))
                val radius: Float = view.resources.getDimension(R.dimen.token_list_blockie_single_line_radius)
                blockie.setCornerRadius(radius)
                blockie.visibility = View.VISIBLE
            }
        } else {
            nameView.text = ""
            balanceView.text = ""
            blockie.visibility = View.INVISIBLE
        }

        return view
    }

    public fun indexOfTokenId(tokenId: String): Int? {
        val token: BalanceInfo? = objects.firstOrNull({
            it.tokenId == tokenId
        })

        if (token != null) {
            return objects.indexOf(token)
        } else {
            return null
        }
    }

}