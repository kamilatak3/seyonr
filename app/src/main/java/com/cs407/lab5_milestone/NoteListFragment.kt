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
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.NoteSummary
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

    private var deleteIt: Boolean = false
    private lateinit var noteToDelete: NoteSummary

    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteDB = NoteDatabase.getDatabase(requireContext()) // Initialize noteDB here

        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE
        )
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        }

        // Since we are hard-coding notes in Milestone 1, we can omit any database operations here
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

        // Create a hard-coded list of notes for Milestone 1
        val sampleNotes = listOf(
            NoteSummary(
                noteId = 1,
                noteTitle = "Sample Note 1",
                noteAbstract = "This is the first sample note.",
                lastEdited = Calendar.getInstance().time
            ),
            NoteSummary(
                noteId = 2,
                noteTitle = "Sample Note 2",
                noteAbstract = "This is the second sample note.",
                lastEdited = Calendar.getInstance().time
            )
            // Add more notes as needed
        )

        adapter = NoteAdapter(
            notes = sampleNotes,  // Ensure this matches the constructor parameter name
            onClick = { noteId ->
                val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(noteId)
                findNavController().navigate(action)
            },
            onLongClick = { noteSummary ->
                deleteIt = true
                noteToDelete = noteSummary
                showDeleteBottomSheet()
            }
        )

        noteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        noteRecyclerView.adapter = adapter

        fab.setOnClickListener {
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(0)
            findNavController().navigate(action)
        }

        // Fetch notes for the current user
        lifecycleScope.launch {
            val userId = userViewModel.userState.value?.id ?: return@launch
            val notes = withContext(Dispatchers.IO) {
                noteDB.noteDao().getAllNotes(userId) // Use getAllNotes to fetch a list
            }
            adapter.submitList(notes)
        }
    }

    private fun showDeleteBottomSheet() {
        if (deleteIt) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val deleteButton = bottomSheetView.findViewById<Button>(R.id.deleteButton)
            val cancelButton = bottomSheetView.findViewById<Button>(R.id.cancelButton)
            val deletePrompt = bottomSheetView.findViewById<TextView>(R.id.deletePrompt)

            deletePrompt.text = "Delete Note: ${noteToDelete.noteTitle}"

            deleteButton.setOnClickListener {
                // Since note deletion is not required for Milestone 1, we can simply dismiss the dialog
                deleteIt = false
                bottomSheetDialog.dismiss()
            }

            cancelButton.setOnClickListener {
                deleteIt = false
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setOnDismissListener {
                deleteIt = false
            }

            bottomSheetDialog.show()
        }
    }

    private fun deleteAccountAndLogout() {
        // Retrieve the current user state from the ViewModel (contains user details)
        val userState = userViewModel.userState.value ?: UserState()

        // Remove the user's credentials from SharedPreferences
        val editor = userPasswdKV.edit()
        editor.remove(userState.name)
        editor.apply()

        // Reset the user state in the ViewModel to represent a logged-out state
        userViewModel.setUser(UserState())

        // Navigate back to the login screen after the account is deleted and user is logged out
        findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
    }
}
