package com.example.firebaseforum.ui.forums

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebaseforum.R
import com.example.firebaseforum.data.Post
import com.example.firebaseforum.databinding.FragmentRoomBinding
import com.example.firebaseforum.firebase.FirebaseHandler
import com.example.firebaseforum.helpers.KeyboardHelper
import com.example.firebaseforum.helpers.MyTextWatcher
import com.example.firebaseforum.helpers.TimerTaskListener
import com.example.firebaseforum.helpers.myCapitalize
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

class RoomFragment : Fragment(), ValueEventListener {

    private var _binding: FragmentRoomBinding? = null // declare binding property as nullable
    private val args: RoomFragmentArgs by navArgs() // get safe args using Navigation component

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!! // get the binding if it is not null or throw an exception

    private lateinit var listAdapter: RoomRecyclerViewAdapter // declare adapter variable
    private lateinit var roomName: String // declare room name variable
    private val posts: ArrayList<Post> = ArrayList() // create an array list to store posts
    private var isFirstGet: Boolean =
        true // initialize flag to true to indicate that it's the first time to get the posts


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomBinding.inflate(
            inflater,
            container,
            false
        ) // inflate the view using view binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFirstGet =
            true // set the flag to true to indicate that it's the first time to get the posts
        roomName = args.roomName // get the room name from the safe args
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            roomName.myCapitalize() // set the toolbar title
        FirebaseHandler.RealtimeDatabase.listenToMessageFromRoom(
            roomName,
            this
        ) // listen to messages from the Firebase database
        setupButtons() // set up the send button
        setupRecyclerView() // set up the recycler view
    }

    private fun setupButtons() {
        binding.postSendButton.isEnabled = false // disable the send button by default
        binding.postSendButton.setOnClickListener { // set the click listener for the send button
            messageTextWatcher.cancelTimer() // cancel the timer for text change events
            val message =
                binding.message.editText?.text.toString() // get the message from the text field
            FirebaseHandler.RealtimeDatabase.addMessage( // add the message to the Firebase database
                roomName, Post(
                    FirebaseHandler.Authentication.getUserEmail(),
                    message,
                    System.currentTimeMillis()
                )
            )
            binding.message.editText?.text?.clear() // clear the text field
            KeyboardHelper.hideSoftwareKeyboard(
                requireContext(),
                binding.root.windowToken
            ) // hide the software keyboard
        }
    }

    private fun setupEditText() {
        binding.message.editText?.addTextChangedListener(messageTextWatcher) // set up the text watcher for the message text field
    }

    private fun setupRecyclerView() {
        listAdapter = RoomRecyclerViewAdapter() // create the adapter instance
        with(binding.messageList) {
            layoutManager =
                LinearLayoutManager(requireContext()) // set the layout manager for the recycler view
            adapter = listAdapter // set the adapter for the recycler view
        }
    }

    private fun showList(posts: List<Post>) {
        binding.messageList.visibility = View.VISIBLE // make the recycler view visible
        listAdapter.submitList(posts) // submit the list of posts to the adapter
        binding.messageList.scrollToPosition(posts.size - 1) // scroll to the last position
        if (!isFirstGet)
            listAdapter.notifyItemInserted(posts.size) // notify the adapter that an item has been inserted if it's not the first time
    }

    override fun onResume() {
        super.onResume()
        // set up the text watcher for the message text field each time the fragment is resumed
        setupEditText()
    }
    override fun onPause() {
        super.onPause()
        // remove the text watcher for the message text field each time the fragment is paused
        binding.message.editText?.removeTextChangedListener(messageTextWatcher)
        messageTextWatcher.cancelTimer() // cancel the timer for text change events
    }

    // This function is called when the fragment view is about to be destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        posts.clear()
        FirebaseHandler.RealtimeDatabase.stopListeningToRoomMessages(roomName, this)
    }


    // This function is called when the data in the Realtime Database changes.
    override fun onDataChange(snapshot: DataSnapshot) {
        // If the snapshot is not empty
        if (snapshot.value != null) {
            // If it's the first time getting data, add all the posts in the snapshot
            if (isFirstGet) {
                for (child in snapshot.children) {
                    val post = child.getValue<Post>()
                    post?.let {
                        posts.add(post)
                    }
                }
            }
            // If it's not the first time getting data, add only the last post
            else {
                val lastPost = snapshot.children.lastOrNull()?.getValue<Post>()
                lastPost?.let {
                    posts.add(lastPost)
                }
            }
            // Show the posts in the RecyclerView and update isFirstGet to false
            showList(posts)
            isFirstGet = false
        }
    }

    // This function is called when there is a cancellation of the query.
    override fun onCancelled(error: DatabaseError) {
        // Do nothing
    }

    // An instance of MyTextWatcher is created which updates the state of the UI based on user input.
    private val messageTextWatcher: MyTextWatcher = MyTextWatcher(object : TimerTaskListener {
        override fun timerRun() {
            requireActivity().runOnUiThread {
                // Get the message text and check if it's empty
                val message = binding.message.editText?.text.toString()
                val notEmpty = message.isNotEmpty()
                // Enable or disable the post send button based on whether the message is empty
                binding.postSendButton.isEnabled = notEmpty
                // Set the tint color of the post send button based on whether the message is empty
                val tintColor =
                    binding.root.context.getColor(if (notEmpty) R.color.secondary else R.color.grey)
                binding.postSendButton.backgroundTintList = ColorStateList.valueOf(tintColor)
            }
        }
    })
}

