package com.cs407.lab5_milestone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.UserNote
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class NoteListFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private lateinit var userViewModel: UserViewModel

    private lateinit var noteDB: NoteDatabase
    private lateinit var userPasswdKV: SharedPreferences

    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteDB = NoteDatabase.getDatabase(requireContext()) // Initialize noteDB here

        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE
        )
        userViewModel = injectedUserViewModel ?: ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)
        greetingTextView = view.findViewById(R.id.greetingTextView)
        noteRecyclerView = view.findViewById(R.id.noteRecyclerView)
        fab = view.findViewById(R.id.fab)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.note_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        userViewModel.setUser(UserState()) // Reset user state
                        findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
                        true
                    }
                    R.id.action_delete_account -> {
                        deleteAccountAndLogout()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        val userState = userViewModel.userState.value ?: UserState()
        greetingTextView.text = getString(R.string.greeting_text, userState.name)

        // Initialize the adapter with click and long-click listeners
        adapter = NoteAdapter(
            onClick = { note ->
                val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(note.noteId)
                findNavController().navigate(action)
            },
            onLongClick = { note ->
                showDeleteBottomSheet(note)
            }
        )

        noteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        noteRecyclerView.adapter = adapter

        fab.setOnClickListener {
            // Navigate to NoteContentFragment with noteId = 0 (new note)
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(0)
            findNavController().navigate(action)
        }

        // Fetch notes for the current user
        fetchNotes()
    }

    private fun fetchNotes() {
        val userId = userViewModel.userState.value?.id ?: return

        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) {
                noteDB.noteDao().getAllNotes(userId)
            }
            adapter.submitList(notes)
        }
    }

    private fun showDeleteBottomSheet(note: Note) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val deleteButton = bottomSheetView.findViewById<Button>(R.id.deleteButton)
        val cancelButton = bottomSheetView.findViewById<Button>(R.id.cancelButton)
        val deletePrompt = bottomSheetView.findViewById<TextView>(R.id.deletePrompt)


        deleteButton.setOnClickListener {
            // Perform deletion
            deleteNote(note)
            bottomSheetDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun deleteNote(note: Note) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Delete from UserNote table
                    val userNote = UserNote(userId = note.userId, noteId = note.noteId)
                    noteDB.userNoteDao().deleteUserNote(userNote)

                    // Delete the note itself
                    noteDB.noteDao().deleteNoteById(note.noteId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Refresh the notes list after deletion
            fetchNotes()
        }
    }

    private fun deleteAccountAndLogout() {
        // Retrieve the current user state from the ViewModel (contains user details)
        val userState = userViewModel.userState.value ?: UserState()

        // Remove the user's credentials from SharedPreferences
        val editor = userPasswdKV.edit()
        editor.remove(userState.name)
        editor.apply()

        // Delete user from the database
        lifecycleScope.launch(Dispatchers.IO) {
            val user = noteDB.userDao().getById(userState.id)
            if (user != null) {
                noteDB.userDao().deleteUser(user)
            }
        }

        // Reset the user state in the ViewModel to represent a logged-out state
        userViewModel.setUser(UserState())

        // Navigate back to the login screen after the account is deleted and user is logged out
        findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
    }
}
