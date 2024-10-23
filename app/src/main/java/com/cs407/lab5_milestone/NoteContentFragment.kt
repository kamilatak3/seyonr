package com.cs407.lab5_milestone

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.UserNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import android.util.Log

class NoteContentFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button

    private var noteId: Int = 0
    private lateinit var noteDB: NoteDatabase
    private lateinit var userViewModel: UserViewModel
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = arguments?.getInt("noteId") ?: 0
        noteDB = NoteDatabase.getDatabase(requireContext())
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        userId = userViewModel.userState.value?.id ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_note_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleEditText = view.findViewById(R.id.titleEditText)
        contentEditText = view.findViewById(R.id.contentEditText)
        saveButton = view.findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            Log.d("NoteContentFragment", "Save button clicked")
            saveContent()
        }

        setupMenu()
        setupBackNavigation()

        if (noteId != 0) {
            // Launch a coroutine to fetch the note from the database in the background
            lifecycleScope.launch {
                try {
                    // Retrieve the note from the Room database using the noteId
                    val note = withContext(Dispatchers.IO) {
                        noteDB.noteDao().getNoteById(noteId)
                    }

                    // Check if the note content is stored in the database or in a file
                    val content: String? = if (note?.noteDetail != null) {
                        // Content is stored in the database
                        note.noteDetail
                    } else {
                        // Content is stored in a file
                        val fileName = note?.notePath
                        if (fileName != null) {
                            // Read the file content
                            val file = File(requireContext().filesDir, fileName)
                            if (file.exists()) {
                                file.readText()
                            } else {
                                ""
                            }
                        } else {
                            ""
                        }
                    }

                    // Switch back to the main thread to update the UI with the note content
                    withContext(Dispatchers.Main) {
                        // Set the retrieved note title to the title EditText field
                        titleEditText.setText(note?.noteTitle)
                        // Set the note content to the content EditText field
                        contentEditText.setText(content)
                    }
                } catch (e: Exception) {
                    // Optionally handle exceptions (e.g., file not found, database errors) if necessary
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        findNavController().popBackStack()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (activity is AppCompatActivity) {
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBackNavigation() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity is AppCompatActivity) {
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun saveContent() {
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()

        if (title.isBlank() || content.isBlank()) {
            Log.e("NoteContentFragment", "Title or content is empty")
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    Log.d("NoteContentFragment", "Saving note...")
                    val noteAbstract = content.take(20)
                    val note = Note(
                        noteId = if (noteId != 0) noteId else 0,
                        userId = userId,
                        noteTitle = title,
                        noteAbstract = noteAbstract,
                        noteDetail = content,
                        notePath = null,
                        lastEdited = Calendar.getInstance().time
                    )

                    val newNoteId = noteDB.noteDao().upsertNote(note).toInt()

                    // Check if the user exists
                    val user = noteDB.userDao().getById(userId)
                    if (user != null) {
                        if (noteId == 0) {
                            val userNote = UserNote(
                                userId = userId,
                                noteId = newNoteId
                            )
                            noteDB.userNoteDao().insertUserNote(userNote)
                        }
                    } else {
                        Log.e("NoteContentFragment", "User does not exist")
                    }

                    withContext(Dispatchers.Main) {
                        Log.d("NoteContentFragment", "Note saved successfully")
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    Log.e("NoteContentFragment", "Error saving note", e)
                }
            }
        }
    }

    private fun splitAbstractDetail(content: String?): String {
        val stringList = content?.split('\n', limit = 2) ?: listOf("")
        var stringAbstract = stringList[0]
        if (stringAbstract.length > 20) {
            stringAbstract = stringAbstract.substring(0, 20) + "..."
        }
        return stringAbstract
    }
}
