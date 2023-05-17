package com.example.firebaseforum.ui.forums

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebaseforum.R
import com.example.firebaseforum.data.Room
import com.example.firebaseforum.databinding.FragmentForumsBinding
import com.example.firebaseforum.firebase.FirebaseHandler
import com.example.firebaseforum.helpers.RVItemClickListener
import com.example.firebaseforum.ui.dialogs.AddRoomDialog
import com.example.firebaseforum.ui.dialogs.RoomPasswordDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.getValue


// The ForumsFragment is responsible for displaying the list of available chat rooms.
class ForumsFragment : Fragment(),
    ChildEventListener {
    // View binding for the fragment
    private var _binding: FragmentForumsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Adapter for the RecyclerView that displays the list of rooms
    private lateinit var listAdapter: ForumsRecyclerViewAdapter

    // List of invalid room names (used when adding a new room)
    private val invalidRoomNames: ArrayList<String> = ArrayList()

    // List of rooms currently displayed in the RecyclerView
    private var rooms: ArrayList<Room> = ArrayList()

    // Flag indicating whether this is the first time data is being loaded from the database
    private var isFirstGet: Boolean = true

    // Inflate the layout for the fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Initialize the RecyclerView and load the list of rooms from the database
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Wait 100ms before loading the list of rooms to allow the RecyclerView to initialize
        binding.root.postDelayed({
            isFirstGet = true

            // Setup event listeners for buttons
            setupButtons()
            // Initialize the RecyclerView
            setupRecyclerView()

            FirebaseHandler.RealtimeDatabase.getRooms().addOnSuccessListener {
                // Iterate over each child node in the "rooms" reference
                for (child in it.children) {
                    // Convert the child node to a Room object
                    val roomFromDB = child.getValue<Room>()
                    roomFromDB?.let {
                        // Add the room to the list of rooms
                        addRoom(roomFromDB)
                    }
                }
                // Display the list of rooms in the RecyclerView
                showList(rooms)
                // Register a ChildEventListener to listen for changes to the "rooms" reference
                FirebaseHandler.RealtimeDatabase.listenToRoomsReference(this@ForumsFragment)
            }
        }, 100)
    }

    /**
     * Adds a new room to the list of rooms and returns its index in the list.
     *
     * @param room The room object to add to the list.
     * @param isFirst Whether the new room is the first room in the list.
     * @return The index of the added room in the list.
     */
    private fun addRoom(room: Room, isFirst: Boolean = false): Int {

        // Initialize the index variable.
        var idx = 0

        // If the new room is not the first room in the list, find the appropriate index for it based on its timestamp.
        if (!isFirst) {
            for ((i, existingRoom) in rooms.withIndex()) {
                if (room.lastMessageTimestamp!! >= existingRoom.lastMessageTimestamp!!) {
                    idx = i
                    break
                } else
                    idx = i + 1
            }
        }

        // Add the new room to the list of invalid room names at the determined index.
        invalidRoomNames.add(idx, room.roomName!!)

        // Add the new room to the list of rooms at the determined index.
        rooms.add(idx, room)

        // Return the index of the added room.
        return idx
    }


    // Sets up the button listeners for the fragment
    private fun setupButtons() {
        // Sets the listener for the 'add forum' floating action button
        binding.addForumFab.setOnClickListener {
            showAddDialog()
        }
    }

    // Handles item clicks in the forum list
    private val listItemCLickListener: RVItemClickListener = object : RVItemClickListener {
        // Handles item click events
        override fun onItemClick(position: Int) {
            // Gets the room associated with the clicked item
            val room = rooms[position]
            room.roomName?.let {
                // Checks if the room is public or the user is the owner
                if (room.isPrivate == false ||
                    room.ownerEmail == FirebaseHandler.Authentication.getUserEmail()
                ) {
                    // Navigates to the selected room
                    val navigateToRoomFragmentAction =
                        ForumsFragmentDirections.actionNavigationForumsToRoomFragment(it)
                    findNavController().navigate(navigateToRoomFragmentAction)
                } else {
                    // Shows a password dialog for private rooms that the user is not the owner of
                    showPasswordDialog(position)
                }
            }
        }
    }

    // Sets up the recycler view for the forum list
    private fun setupRecyclerView() {
        // Creates the adapter with the item click listener
        listAdapter = ForumsRecyclerViewAdapter(listItemCLickListener)
        with(binding.forumList) {
            // Sets the layout manager to a grid with two columns
            layoutManager = LinearLayoutManager(requireContext())//GridLayoutManager(requireContext(), 2)
            // Sets the adapter for the recycler view
            adapter = listAdapter
        }
    }

    // Declares an instance of the DialogListener interface which listens for a positive click event on the AddRoomDialog.
    private val addDialogListener = object : AddRoomDialog.DialogListener {

        // Implements the onPositiveClick method which gets called when the user clicks the positive button on the dialog.
        override fun onPositiveClick(roomName: String, password: String) {

            // Creates a new Room object with the provided roomName,
            // current user's uid, email, empty message fields,
            // current timestamp, and password flag and value.
            val newRoom = Room(
                roomName,
                FirebaseHandler.Authentication.getUserUid(),
                FirebaseHandler.Authentication.getUserEmail(),
                "",
                "",
                System.currentTimeMillis(),
                password.isNotEmpty(),
                password
            )

            // Adds the newly created Room object to the database using the FirebaseHandler.RealtimeDatabase.addRoom method.
            FirebaseHandler.RealtimeDatabase.addRoom(newRoom)

            // Adds the roomName to the user's rooms list in the Realtime Database.
            FirebaseHandler.RealtimeDatabase.addUserRooms(roomName)

        }
    }

    // Shows the dialog for adding a new room to the forum
    private fun showAddDialog() {
        // Creates a new instance of the dialog
        val newInstance = AddRoomDialog().newInstance()
        // Sets the listener and invalid room names for the dialog
        newInstance.apply {
            setDialogListener(addDialogListener)
            setInvalidNames(invalidRoomNames)
        }
        // Shows the dialog
        newInstance.show(requireActivity().supportFragmentManager, "AddRoomDialog")
    }


    // Define a dialog listener for room password dialog
    private val passwordDialogListener = object : RoomPasswordDialog.DialogListener {
        // This method is called when the user clicks the positive button in the dialog
        override fun onPositiveClick(roomPosition: Int, password: String) {
            // Check if a valid room position was passed
            if (roomPosition != -1) {
                // Get the room at the specified position
                val room = rooms[roomPosition]
                // Check if the room has a password
                room.roomPassword?.let { roomPassword ->
                    // If the password matches, navigate to the room
                    if (roomPassword == password) {
                        val navigateToRoomFragmentAction =
                            ForumsFragmentDirections.actionNavigationForumsToRoomFragment(room.roomName!!)
                        findNavController().navigate(navigateToRoomFragmentAction)
                        // If the password is wrong, show an error message and allow the user to try again
                    } else {
                        Snackbar.make(
                            binding.forumList,
                            getString(R.string.wrong_pass_msg),
                            Snackbar.LENGTH_LONG
                        ).setAction(getString(R.string.try_again_msg)) {
                            showPasswordDialog(roomPosition)
                        }.show()
                    }
                }
            }
        }
    }


    /**
     * Shows a password dialog for a private chat room.
     *
     * @param roomPosition The index of the room in the list.
     */
    private fun showPasswordDialog(roomPosition: Int) {
        // Create a new instance of the password dialog.
        val newInstance = RoomPasswordDialog().newInstance()

        // Set the room position and dialog listener for the new instance.
        newInstance.apply {
            setRoomPosition(roomPosition)
            setDialogListener(passwordDialogListener)
        }

        // Show the password dialog using the activity's fragment manager.
        newInstance.show(requireActivity().supportFragmentManager, "PasswordDialog")
    }


    private fun showList(rooms: List<Room>, position: Int = -1) {
        // Make the forumList invisible
        binding.forumList.visibility = View.INVISIBLE
        // Schedule a delayed task to make the forumList visible
        binding.root.postDelayed({
            // Make the forumList visible
            binding.forumList.visibility = View.VISIBLE

            // Load an animation for the forumList
            val animation: LayoutAnimationController =
                AnimationUtils.loadLayoutAnimation(
                    requireContext(),
                    R.anim.layout_animation_fall_down
                )

            // Set the animation as the layout animation for the forumList
            binding.forumList.layoutAnimation = animation

            // Schedule the layout animation for the forumList
            binding.forumList.scheduleLayoutAnimation()

            // Submit the list of rooms to the listAdapter
            listAdapter.submitList(rooms)


            // If position is not -1, notify the adapter to update the list starting from the first item up to the last visible item at position.
            if (position != -1) {
                listAdapter.notifyDataSetChanged()
            }

            // Scroll the forumList to the top
            binding.forumList.smoothScrollToPosition(0)
        }, 50)
    }

    // This function is called when the view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the list of rooms and invalid room names to free up memory.
        rooms.clear()
        invalidRoomNames.clear()

        // Stop listening to changes in the Realtime Database reference associated with this fragment.
        FirebaseHandler.RealtimeDatabase.stopListeningToRoomsRef(this)

        // Set the binding object to null to avoid memory leaks.
        _binding = null
    }


    // Callback when a child node is added to the Firebase Realtime Database's "rooms" node
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        // Check if the child node has a non-null value and is not already on the list
        if (snapshot.value != null && !invalidRoomNames.contains(snapshot.key)) {
            // Retrieve the last room added to the Realtime Database from the snapshot
            val lastRoom = snapshot.getValue<Room>()
            lastRoom?.let {
                // Add the last room to the list of rooms and get its position in the list based on timestamp
                val roomPos = addRoom(lastRoom)
                // Display the updated list of rooms with an animation that falls down into view
                showList(rooms, roomPos)
            }
        }
    }

    // Callback when a child node is changed in the Firebase Realtime Database's "rooms" node
    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

        // Check if the child node has a non-null value
        if (snapshot.value != null) {
            // Retrieve the changed room from the snapshot
            val changedRoom = snapshot.getValue<Room>()
            changedRoom?.let {
                Log.i("changedRoom", "$changedRoom $previousChildName\n${snapshot}" )
                // Find the position of the changed room in the list of invalid room names
                val roomPos = invalidRoomNames.indexOf(changedRoom.roomName)
                // Remove the changed room's name from the list of invalid room names
                invalidRoomNames.removeAt(roomPos)
                // Remove the changed room from the list of rooms at its previous position
                rooms.removeAt(roomPos)
                // Add the changed room at the top of the list of rooms
                addRoom(changedRoom, true)
                val newRoomPos = rooms.indexOf(changedRoom)
                // Display the updated list of rooms with an animation that falls down into view
                showList(rooms, newRoomPos)
            }
        }
    }


    override fun onChildRemoved(snapshot: DataSnapshot) {
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
    }

    override fun onCancelled(error: DatabaseError) {
    }

}

