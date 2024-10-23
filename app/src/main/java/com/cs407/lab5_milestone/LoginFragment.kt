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
import com.cs407.lab5_milestone.data.User
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

    private lateinit var noteDB: NoteDatabase

    private lateinit var userViewModel: UserViewModel

    private lateinit var userPasswdKV: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize noteDB here
        noteDB = NoteDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        errorTextView = view.findViewById(R.id.errorTextView)

        userViewModel = injectedUserViewModel ?: ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        // Get shared preferences using R.string.user_passwd_kv as the name
        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE
        )

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
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                errorTextView.text = getString(R.string.empty_field)
                errorTextView.visibility = View.VISIBLE
            } else {
                lifecycleScope.launch {
                    val loginSuccess = withContext(Dispatchers.IO) {
                        getUserPasswd(username, password)
                    }

                    if (loginSuccess) {
                        val user = withContext(Dispatchers.IO) {
                            noteDB.userDao().getByName(username)
                        }

                        if (user != null) {
                            userViewModel.setUser(UserState(id = user.userId, name = username))
                            findNavController().navigate(R.id.action_loginFragment_to_noteListFragment)
                        } else {
                            errorTextView.text = getString(R.string.fail_login)
                            errorTextView.visibility = View.VISIBLE
                        }
                    } else {
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
        val hashedPassword = hash(passwdPlain)

        if (userPasswdKV.contains(name)) {
            val storedPassword = userPasswdKV.getString(name, null)
            return storedPassword != null && storedPassword == hashedPassword
        } else {
            // Insert into SharedPreferences
            val editor = userPasswdKV.edit()
            editor.putString(name, hashedPassword)
            editor.apply()

            // Insert user into the database
            withContext(Dispatchers.IO) {
                val user = User(userName = name)
                noteDB.userDao().insertUser(user)
            }

            return true
        }
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    // Example of logging user creation
    private fun logUserCreation(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = User(userName = name)
            val userId = noteDB.userDao().insertUser(user)
            // Log or handle the created user
        }
    }

    private fun createUserIfNotExists(userName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val existingUser = noteDB.userDao().getByName(userName)
            if (existingUser == null) {
                val newUser = User(userName = userName)
                val userId = noteDB.userDao().insertUser(newUser)
                withContext(Dispatchers.Main) {
                    userViewModel.setUser(UserState(id = userId.toInt(), name = userName))
                }
            } else {
                withContext(Dispatchers.Main) {
                    userViewModel.setUser(UserState(id = existingUser.userId, name = userName))
                }
            }
        }
    }
}
