package com.example.firebaseforum.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebaseforum.R
import com.example.firebaseforum.data.Room
import com.example.firebaseforum.databinding.FragmentHomeBinding
import com.example.firebaseforum.firebase.FirebaseHandler
import com.example.firebaseforum.helpers.RVItemClickListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

// TODO: W instrukcji dodać zadanie sortowania listy wiadomości
class HomeFragment : Fragment(), ValueEventListener {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
// Get the binding object from the nullable _binding property. It will throw an exception
// if accessed outside the lifecycle between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var listAdapter: HomeRecyclerViewAdapter
    private val rooms: ArrayList<Room> = ArrayList()
    private val roomNames: ArrayList<String> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the view using the generated binding class and set the _binding property.
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.postDelayed({
            // Set up the RecyclerView
            setupRecyclerView()

            // Get the user's rooms from the Firebase Realtime Database
            FirebaseHandler.RealtimeDatabase.getUserRooms().addOnSuccessListener {
                // Clear the list of room names and repopulate it with the user's current rooms
                roomNames.clear()
                for (child in it.children) {
                    val key = child.key
                    // Skip over the "email" child, which is not a room
                    if (child.key != "email") {
                        key?.let { roomName ->
                            // Add the room name to the list of room names and start listening for updates
                            roomNames.add(roomName)
                            FirebaseHandler.RealtimeDatabase.listenToRoomReference(roomName, this)
                        }
                    }
                }
            }
        }, 100)
    }

    override fun onStop() {
        super.onStop()
        //stop listening for updates to each room
        for (roomName in roomNames) {
            FirebaseHandler.RealtimeDatabase.stopListeningToRoomRef(roomName, this)
        }
    }
    // This method is called when the view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the list of rooms
        rooms.clear()
        // Clear the list of room names and set the binding object to null
        roomNames.clear()
        _binding = null
    }

    // Interface for item click events in a RecyclerView
    private val listItemCLickListener: RVItemClickListener = object : RVItemClickListener {
        // Callback for the item click event on the recycler view adapter
        override fun onItemClick(position: Int) {
            val room = rooms[position] // Get the Room object from the clicked position
            room.roomName?.let { // If the roomName property is not null
                // Create an action to navigate to the RoomFragment with the room name as argument
                val navigateToRoomFragmentAction =
                    HomeFragmentDirections.actionNavigationHomeToRoomFragment(it)
                findNavController().navigate(navigateToRoomFragmentAction) // Navigate to the RoomFragment
            }
        }
    }

    // Set up the recycler view with a LinearLayoutManager and a HomeRecyclerViewAdapter with the item click listener
    private fun setupRecyclerView() {
        // Create a HomeRecyclerViewAdapter with the item click listener
        listAdapter = HomeRecyclerViewAdapter(listItemCLickListener)
        with(binding.homeList) {
            // Set the layout manager for the recycler view
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter // Set the adapter for the recycler view
        }
    }


    // This function shows the list of rooms on the home screen
    private fun showList(rooms: List<Room>) {
        binding.homeList.visibility = View.VISIBLE
        // Load layout animation from resources
        val animation: LayoutAnimationController =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)
        // Set the layout animation to the RecyclerView
        binding.homeList.layoutAnimation = animation
        // Schedule the layout animation to be played
        binding.homeList.scheduleLayoutAnimation()
        // Submit the list of rooms to the adapter to be displayed
        listAdapter.submitList(rooms)
    }

    // This function updates the list item at the given position
    private fun updateList(position: Int) {
        // Notify the adapter that the item at the given position has changed
        listAdapter.notifyItemChanged(position)
    }

    // This function is called whenever data changes in the Firebase Realtime Database
    override fun onDataChange(snapshot: DataSnapshot) {
        // Get the Room object from the snapshot
        val room = snapshot.getValue<Room>()
        room?.let {
            // Find the index of the room in the list of rooms
            val indexOfRoom = roomNames.indexOf(room.roomName)
            // If the room is already in the list, update it
            if (indexOfRoom != -1 && rooms.size == roomNames.size) {
                rooms[indexOfRoom] = room
                updateList(indexOfRoom)
            } else {
                // Otherwise, add the room to the list
                rooms.add(room)
                // If all rooms have been added to the list, show the list
                if (rooms.size == roomNames.size)
                    showList(rooms)
            }
        }
    }


    override fun onCancelled(error: DatabaseError) {
        // shown Snackbar with error message
        Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
    }


}
