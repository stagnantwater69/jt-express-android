package com.jtexpress.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BannerAdapter(private val items: List<BannerAdapter.BannerItem>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    data class BannerItem(
        val title: String,
        val subtitle: String,
        val body: String,
        val tag: String
    )

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_banner_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_banner_subtitle)
        val tvBody: TextView = view.findViewById(R.id.tv_banner_body)
        val tvTag: TextView = view.findViewById(R.id.tv_banner_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)

        // ← ADD THESE TWO LINES
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvBody.text = item.body
        holder.tvTag.text = item.tag

        holder.tvTag.visibility =
            if (item.tag.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = items.size
}