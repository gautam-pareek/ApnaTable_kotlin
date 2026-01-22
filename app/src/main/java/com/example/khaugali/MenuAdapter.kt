package com.example.khaugali

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MenuAdapter(
    private val items: List<MenuItem>,
    private val showDelete: Boolean = false,   // new flag
    private val onItemClick: ((MenuItem) -> Unit)? = null,
    private val onDeleteClick: ((MenuItem) -> Unit)? = null
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val imgFood: ImageView = view.findViewById(R.id.imgFood)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = item.price
        holder.tvRating.text = "⭐ ${item.rating}"

        // Show delete button only if flag is true
        holder.btnDelete.visibility = if (showDelete) View.VISIBLE else View.GONE

        // card click for edit
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }

        // delete button click
        holder.btnDelete.setOnClickListener { onDeleteClick?.invoke(item) }

        // load first image with Glide
        if (item.images.isNotEmpty()) {
            Glide.with(holder.imgFood.context)
                .load(Uri.parse(item.images[0]))
                .into(holder.imgFood)
        }
    }

    override fun getItemCount(): Int = items.size
}
