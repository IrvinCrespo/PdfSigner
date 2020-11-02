package com.matrix.lumston.pdfsigner

import android.graphics.pdf.PdfRenderer
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.synthetic.main.item_page.view.*

class PagesAdapter(private val interaction: Interaction? = null) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PagePdf>() {


        override fun areItemsTheSame(oldItem: PagePdf, newItem: PagePdf): Boolean {
            return oldItem.index == newItem.index
        }

        override fun areContentsTheSame(oldItem: PagePdf, newItem: PagePdf): Boolean {
            return oldItem == newItem
        }

    }

    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return PageVideHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_page,
                parent,
                false
            ),
            interaction
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PageVideHolder -> {
                holder.bind(differ.currentList.get(position))
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<PagePdf>) {
        differ.submitList(list)
    }

    class PageVideHolder
    constructor(
        itemView: View,
        private val interaction: Interaction?
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: PagePdf) = with(itemView) {

            itemView.setOnClickListener {
                interaction?.onItemSelected(adapterPosition, item)
            }

            page_image.setImageBitmap(item.bitmap)

        }
    }

    interface Interaction {
        fun onItemSelected(position: Int, item: PagePdf)
    }
}