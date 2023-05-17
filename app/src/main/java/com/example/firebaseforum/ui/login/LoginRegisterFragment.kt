package com.example.firebaseforum.ui.login

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.firebaseforum.R
import com.example.firebaseforum.data.User
import com.example.firebaseforum.databinding.FragmentLoginBinding
import com.example.firebaseforum.firebase.FirebaseHandler
import com.example.firebaseforum.helpers.KeyboardHelper
import com.example.firebaseforum.helpers.MyTextWatcher
import com.example.firebaseforum.helpers.TimerTaskListener
import com.google.android.material.snackbar.Snackbar


class LoginRegisterFragment : Fragment() {
    // Validity check flags
    private var passwordConfirmValid: Boolean = false
    private var passwordValid: Boolean = false
    private var usernameValid: Boolean = false

    // Flag indicating the state of the form. True means that the form is used for Login,
    // false means that the form is used to register user
    private var isLogin = true


    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set isLogin to true
        isLogin = true

        // The LoginRegisterFragment handles both the login and register process
        // The Login form is the default one so the password confirm field is hidden by default
        binding.loginPasswordConfirm.visibility = View.GONE

        // The Login/Register confirm button is disable until valid data is provided in the form
        binding.loginRegisterButton.isEnabled = false

        setupButtons()
    }

    private fun setupEditTexts() {
        binding.loginUsername.editText?.addTextChangedListener(usernameTextWatcher)
        binding.loginPassword.editText?.addTextChangedListener(passwordTextWatcher)
        binding.loginPasswordConfirm.editText?.addTextChangedListener(passwordConfirmTextWatcher)

    }

    // This function sets up the buttons in the login/register screen
    private fun setupButtons() {
        // Set a click listener to the login/register toggle button
        binding.loginRegisterToggle.setOnClickListener {
            // Toggle the screen to login or register mode
            toggleLoginRegister()
        }
        binding.loginRegisterButton.setOnClickListener {
            cancelTimers() // Cancel any existing timers
            KeyboardHelper.hideSoftwareKeyboard(requireContext(), binding.root.windowToken)
            if (validate()) {
                val email = binding.loginUsername.editText?.text.toString()
                val password = binding.loginPassword.editText?.text.toString()
                if (isLogin)
                    login(email, password)
                else register(email, password)
            }
        }
    }


    private fun toggleLoginRegister() {
        // Toggle the isLogin flag
        isLogin = !isLogin
        if (isLogin) {
            // When the isLogin is true:
            // Change the text on the login/register button to login
            binding.loginRegisterButton.setText(R.string.login_str)
            // Change the text on the login/register toggle button to register
            binding.loginRegisterToggle.setText(R.string.register_str)
            // Hide the password confirm field
            binding.loginPasswordConfirm.visibility = View.GONE
        } else {
            // When the isLogin is false:
            // Change the text on the login/register button to register
            binding.loginRegisterButton.setText(R.string.register_str)
            // Change the text on the login/register toggle button to login
            binding.loginRegisterToggle.setText(R.string.login_str)
            // Show the password confirm field
            binding.loginPasswordConfirm.visibility = View.VISIBLE
        }
        // Change the state of the login/register button depending on the state of the fields flags
        binding.loginRegisterButton.isEnabled = validate()
    }

    private fun checkUserName(username: String) {
        // Check the username data
        Log.i("checkUserName", username)
        if (!isUserNameValid(username)) {
            // If the username fails the validity check set an error message in the loginUsername
            // TextInputField. This will change the end icon of the View and also display the
            // message in the Helper text region of the view
            binding.loginUsername.error = getString(R.string.invalid_username)
            // Set the usernameValid flag to false
            usernameValid = false
        } else {
            // If the username passes the test clear the error message and set the flag to true
            binding.loginUsername.error = ""
            usernameValid = true
        }
    }

    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            // A valid username contains an "@" symbol and matches an email pattern
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            false
        }
    }

    private fun checkPassword(password: String) {
        // Check the password data
        Log.i("checkPassword", password)
        if (!isPasswordValid(password)) {
            // If the password fails the validity check set an error message in the loginPassword
            // TextInputField. This will change the end icon of the View and also display the
            // message in the Helper text region of the view
            binding.loginPassword.error = getString(R.string.invalid_password)
            // Set the passwordValid flag to false
            passwordValid = false
        } else {
            // If the password passes the test clear the error message and set the flag to true
            binding.loginPassword.error = ""
            passwordValid = true
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        // A valid password has at least 6 characters
        return password.length >= 6
    }

    private fun checkRegisterPassword(passwordConfirm: String, password: String) {
        // Check the password confirm data
        Log.i("checkRegisterPassword", "$passwordConfirm $password")
        if (!isPasswordConfirmValid(passwordConfirm, password)) {
            // If the password confirm fails the validity check set an error message in the
            // loginPasswordConfirm TextInputField. This will change the end icon of the View and
            // also display the message in the Helper text region of the view
            binding.loginPasswordConfirm.error = getString(R.string.password_mismatch)
            // Set the passwordConfirmValid flag to false
            passwordConfirmValid = false
        } else {
            // If the password confirm passes the test clear the error message and set the flag to true
            binding.loginPasswordConfirm.error = ""
            passwordConfirmValid = true
        }
    }

    private fun isPasswordConfirmValid(passwordConfirm: String, password: String): Boolean {
        // Password and password confirm have to be the same
        return passwordConfirm == password
    }

    private fun validate(): Boolean {
        // Check the state of the fields flags. Depending on the isLogin flag we check either 2
        // or 3 flags (the passwordConfirmValid flag is not used in the Login form)
        return if (isLogin)
            passwordValid && usernameValid
        else
            passwordValid && usernameValid && passwordConfirmValid
    }

    private fun login(email: String, password: String) {
        // Call the Firebase Authentication login method with the email and password
        FirebaseHandler.Authentication.login(email, password).apply {
            // If login is successful, navigate to the home screen
            addOnSuccessListener {
                Log.d("Login", "signInWithEmail:success")
                findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
            }
            // If login fails, display a message to the user using a Snackbar
            addOnFailureListener {
                Log.w("Login", "signInWithEmail:failure", it)
                Snackbar.make(
                    binding.root, "Authentication failed.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun register(email: String, password: String) {
        // Call the Firebase Authentication register method with the email and password
        FirebaseHandler.Authentication.register(email, password).apply {
            // If registration is successful, add the user to the Realtime Database and navigate to the home screen
            addOnSuccessListener {
                Log.d("Register", "createUserWithEmail:success")
                FirebaseHandler.RealtimeDatabase.addUser(User(email))
                findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
            }
            // If registration fails, display a message to the user using a Snackbar
            addOnFailureListener {
                Log.w("Register", "createUserWithEmail:failure", it)
                Snackbar.make(
                    binding.root, "Registering failed.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        // Check whether the user is already logged in
        if (FirebaseHandler.Authentication.isLoggedIn())
        // navigate to the home screen if the user is already logged in
            findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
    }

    override fun onResume() {
        super.onResume()
        // register the text watchers for input views
        setupEditTexts()
    }
    override fun onPause() {
        super.onPause()
        Log.i("login", "onPause")
        cancelTimers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i("login", "onDestroyView")
        // clear edit texts
        binding.loginUsername.editText?.text?.clear()
        binding.loginPassword.editText?.text?.clear()
        binding.loginPasswordConfirm.editText?.text?.clear()
        // clear the binding object
        _binding = null
    }

    //********************************************************************************************//
    // Watchers for input fields - trigger validation after some delay (500 ms is default)
    //This code defines text watchers for input fields (password, password confirmation, and username)
    // to trigger validation after a delay of 500 ms.
    // The MyTextWatcher class takes a TimerTaskListener object that implements the timerRun() function
    // to execute the validation logic after the specified delay.
    //********************************************************************************************//

    // Watcher for password field
    private val passwordTextWatcher: MyTextWatcher = MyTextWatcher(object : TimerTaskListener {
        override fun timerRun() {
            binding.loginPassword.post {
                // Get the current input from the password field
                val passwordInput = binding.loginPassword.editText?.text.toString()
                if (passwordInput.isNotEmpty()) {
                    // Call checkPassword function to validate the password
                    checkPassword(passwordInput)
                    // Enable or disable the register button based on validation result
                    binding.loginRegisterButton.isEnabled = validate()
                } else
                // If the password field is empty, disable the register button
                {
                    passwordValid = false
                    binding.loginRegisterButton.isEnabled = false
                }
            }
        }
    })

    // Watcher for password confirmation field
    private val passwordConfirmTextWatcher: MyTextWatcher =
        MyTextWatcher(object : TimerTaskListener {
            override fun timerRun() {
                binding.loginPasswordConfirm.post {
                    // Get the current input from the password confirmation field
                    val passwordConfirm = binding.loginPasswordConfirm.editText?.text.toString()
                    // Get the current input from the password field
                    val password = binding.loginPassword.editText?.text.toString()
                    if (passwordConfirm.isNotEmpty() && password.isNotEmpty()) {
                        // Call checkRegisterPassword function to validate the password confirmation
                        checkRegisterPassword(passwordConfirm, password)
                        // Enable or disable the register button based on validation result
                        binding.loginRegisterButton.isEnabled = validate()
                    } else {
                        // If either the password or the password confirmation field is empty, disable the register button
                        passwordConfirmValid = false
                        binding.loginRegisterButton.isEnabled = false
                    }
                }
            }
        })

    // Watcher for username field
    private val usernameTextWatcher: MyTextWatcher = MyTextWatcher(object : TimerTaskListener {
        override fun timerRun() {
            binding.loginUsername.post {
                // Get the current input from the username field
                val username = binding.loginUsername.editText?.text.toString()
                if (username.isNotEmpty()) {
                    // Call checkUserName function to validate the username
                    checkUserName(username)
                    // Enable or disable the register button based on validation result
                    binding.loginRegisterButton.isEnabled = validate()
                } else {
                    // If the username field is empty, disable the register button
                    usernameValid = false
                    binding.loginRegisterButton.isEnabled = false
                }
            }
        }
    })

    // Cancel all timers used by the text watchers
    private fun cancelTimers() {
        Log.i("login", "cancelTimers")
        usernameTextWatcher.cancelTimer()
        passwordTextWatcher.cancelTimer()
        passwordConfirmTextWatcher.cancelTimer()
        binding.loginUsername.editText?.removeTextChangedListener(usernameTextWatcher)
        binding.loginPassword.editText?.removeTextChangedListener(passwordTextWatcher)
        binding.loginPasswordConfirm.editText?.removeTextChangedListener(passwordConfirmTextWatcher)
    }


}
