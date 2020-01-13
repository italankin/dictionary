package com.italankin.dictionary.ui.main

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.italankin.dictionary.R
import com.italankin.dictionary.api.dto.TranslationEx
import java.util.*

internal class TranslationAdapter(context: Context) : RecyclerView.Adapter<TranslationAdapter.ItemViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val dataset: MutableList<TranslationEx> = ArrayList(0)
    private var listener: OnAdapterItemClickListener? = null

    fun setData(data: List<TranslationEx>) {
        dataset.clear()
        dataset.addAll(data)
        notifyDataSetChanged()
    }

    fun setListener(listener: OnAdapterItemClickListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val v = inflater.inflate(R.layout.item_attribute, parent, false)
        return ItemViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val holder = viewHolder
        val item = dataset[position]
        holder.text.text = item.text
        if (TextUtils.isEmpty(item.pos)) {
            holder.pos.visibility = View.GONE
        } else {
            holder.pos.text = "(%s)".format(item.pos)
            holder.pos.visibility = View.VISIBLE
        }
        holder.means.text = item.means
        holder.syns.text = item.synonyms
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun getItemId(position: Int): Long {
        return dataset[position].hashCode().toLong()
    }

    interface OnAdapterItemClickListener {
        fun onItemClick(position: Int)

        fun onItemMenuClick(position: Int, menuItemId: Int)
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView
        val means: TextView
        val syns: TextView
        val pos: TextView
        private val menu: ImageView
        private val popupMenu: PopupMenu

        init {
            view.setOnClickListener { listener?.onItemClick(adapterPosition) }
            text = view.findViewById(R.id.text)
            means = view.findViewById(R.id.means)
            syns = view.findViewById(R.id.synonyms)
            pos = view.findViewById(R.id.pos)
            menu = view.findViewById(R.id.overflow)
            popupMenu = PopupMenu(view.context, menu)
            popupMenu.inflate(R.menu.attribute)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                listener?.onItemMenuClick(adapterPosition, item.itemId)
                true
            }
            menu.setOnClickListener { popupMenu.show() }
        }
    }

}
