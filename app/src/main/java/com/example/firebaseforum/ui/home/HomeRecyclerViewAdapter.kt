package com.example.firebaseforum.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseforum.R
import com.example.firebaseforum.data.Room
import com.example.firebaseforum.databinding.HomeScreenItemBinding
import com.example.firebaseforum.firebase.FirebaseHandler
import com.example.firebaseforum.helpers.RVItemClickListener
import com.example.firebaseforum.helpers.myCapitalize
import com.example.firebaseforum.helpers.toDateString


class HomeRecyclerViewAdapter(
    private val clickListener: RVItemClickListener // Constructor that takes an RVItemClickListener object as a parameter
) : ListAdapter<Room, HomeRecyclerViewAdapter.ViewHolder>(Comparator) { // Extends ListAdapter, with Room as the data type and ViewHolder as the view holder class

    object Comparator :
        DiffUtil.ItemCallback<Room>() {
        // Defines a Comparator object that extends DiffUtil.ItemCallback and specifies the data type (Room)

        override fun areItemsTheSame(oldItem: Room, newItem: Room): Boolean {
            // Checks if the items are the same object (i.e., have the same reference)
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Room, newItem: Room): Boolean {
            // Compares the contents of the items to check if they're the same
            return oldItem.roomName == newItem.roomName && oldItem.lastMessageTimestamp == newItem.lastMessageTimestamp
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        // Overrides the onCreateViewHolder method to inflate the layout and create a ViewHolder
        return ViewHolder(
            HomeScreenItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        // Overrides the onBindViewHolder method to bind the data to the ViewHolder and set the click listener
        val item = getItem(position) // Gets the Room object at the specified position
        holder.bind(item) // Calls the ViewHolder's bind method to bind the data to the views
        holder.setOnClickListener(clickListener) // Sets the click listener on the ViewHolder
    }

    // Defines a ViewHolder that extends RecyclerView.ViewHolder and takes a binding object as a parameter
    inner class ViewHolder(binding: HomeScreenItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // Initializes the TextViews and Views with their respective views in the layout
        private val itemLabel: TextView = binding.homeItemLabel
        private val itemDate: TextView = binding.date
        private val itemPost: TextView = binding.post
        private val decoration: View = binding.decoration
        private val rootView = binding.root
        // Defines a function to set the click listener on the root view of the ViewHolder
        fun setOnClickListener(listener: RVItemClickListener) {
            rootView.setOnClickListener {
                // Calls the onItemClick method of the RVItemClickListener object with the adapter position as a parameter
                listener.onItemClick(adapterPosition) }
        }
        // Defines a function to bind the data to the views
        fun bind(room: Room) {
            // Sets the text of the TextViews to the corresponding values in the Room object
            itemLabel.text =
                "${room.roomName?.myCapitalize()} - ${room.lastMessageAuthor}"
            itemDate.text = room.lastMessageTimestamp?.toDateString()
            itemPost.text = room.lastMessage
            // Checks if the current user is the author of the last message
            val isOwner = room.lastMessageAuthor == FirebaseHandler.Authentication.getUserEmail()
            decoration.setBackgroundColor(
                // Sets the background color of the decoration View based on whether the user is the author of the last message
                decoration.context.getColor(
                    if (isOwner)
                        R.color.secondary
                    else
                        R.color.primary
                )
            )
        }

        override fun toString(): String {
            return super.toString() + " '" + (itemLabel.text) + "'"
        }
    }

}


