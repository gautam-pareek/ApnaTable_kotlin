package com.example.khaugali

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FoodSearchAdapter(
    private val items: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<FoodSearchAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val imgFood: ImageView = view.findViewById(R.id.imgFood)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = item.price
        holder.tvRating.text = "⭐ ${item.rating}"

        // Load first image if exists
        if (item.images.isNotEmpty()) {
            val imageUrl = item.images[0]
            try {
                // Use Glide to load images (handles both local URIs and server URLs)
                Glide.with(holder.imgFood.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.imgFood)
            } catch (e: Exception) {
                // Fallback to default image if loading fails
                holder.imgFood.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            holder.imgFood.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Hide delete button for customer
        holder.btnDelete.visibility = View.GONE

        holder.itemView.setOnClickListener { onItemClick(item) }
    }
}
