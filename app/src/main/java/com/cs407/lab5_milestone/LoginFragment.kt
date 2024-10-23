package com.cs407.lab5_milestone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cs407.lab5_milestone.data.NoteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginFragment(
    private val injectedUserViewModel: UserViewModel? = null // For testing only
) : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorTextView: TextView

    private lateinit var userViewModel: UserViewModel

    private lateinit var userPasswdKV: SharedPreferences
    // private lateinit var noteDB: NoteDatabase // Not used in this milestone

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        errorTextView = view.findViewById(R.id.errorTextView)

        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // Use ViewModelProvider to init UserViewModel
            ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        }

        // Get shared preferences using R.string.userPasswdKV as the name
        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        // Set the login button click action
        loginButton.setOnClickListener {
            // Get the entered username and password from EditText fields
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                // Show an error message if either username or password is empty
                errorTextView.text = "Username and password cannot be empty."
                errorTextView.visibility = View.VISIBLE
            } else {
                // Attempt to login or sign up in a coroutine
                lifecycleScope.launch {
                    val loginSuccess = withContext(Dispatchers.IO) {
                        getUserPasswd(username, password)
                    }

                    if (loginSuccess) {
                        // Set the logged-in user in the ViewModel (store user info)
                        userViewModel.setUser(UserState(name = username))

                        // Navigate to another fragment after successful login
                        findNavController().navigate(R.id.action_loginFragment_to_noteListFragment)
                    } else {
                        // Show an error message if login failed
                        errorTextView.text = getString(R.string.fail_login)
                        errorTextView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private suspend fun getUserPasswd(
        name: String,
        passwdPlain: String
    ): Boolean {
        // Hash the plain password using a secure hashing function
        val hashedPassword = hash(passwdPlain)

        // Check if the user exists in SharedPreferences (using the username as the key)
        if (userPasswdKV.contains(name)) {
            // Retrieve the stored password from SharedPreferences
            val storedPassword = userPasswdKV.getString(name, null)
            // Compare the hashed password with the stored one and return false if they don't match
            if (storedPassword != null && storedPassword == hashedPassword) {
                return true
            } else {
                return false
            }
        } else {
            // If the user doesn't exist in SharedPreferences, create a new user
            // Store the hashed password in SharedPreferences for future logins
            val editor = userPasswdKV.edit()
            editor.putString(name, hashedPassword)
            editor.apply()

            // Return true as the user was newly created
            return true
        }
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}
