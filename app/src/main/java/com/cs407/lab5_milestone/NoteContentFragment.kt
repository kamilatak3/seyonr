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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

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
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // Use ViewModelProvider to init UserViewModel
            ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        }
        userId = userViewModel.userState.value.id
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_content, container, false)
        titleEditText = view.findViewById(R.id.titleEditText)
        contentEditText = view.findViewById(R.id.contentEditText)
        saveButton = view.findViewById(R.id.saveButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    val content: String? = if (note.content != null) {
                        // Content is stored in the database
                        note.content
                    } else {
                        // Content is stored in a file
                        val fileName = note.contentFileName
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
                        titleEditText.setText(note.title)
                        // Set the note content to the content EditText field
                        contentEditText.setText(content)
                    }
                } catch (e: Exception) {
                    // Optionally handle exceptions (e.g., file not found, database errors) if necessary
                    e.printStackTrace()
                }
            }
        }

        saveButton.setOnClickListener {
            saveContent()
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
        // Retrieve the title and content from EditText fields
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()

        // Launch a coroutine to save the note in the background (non-UI thread)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Check if the note content is too large for direct storage in the database
                    val contentBytes = content.toByteArray()
                    val contentTooLarge = contentBytes.size > 1024 // Assuming 1KB limit

                    val contentFileName: String?
                    val contentToStore: String?

                    if (contentTooLarge) {
                        // Save the content as a file
                        contentFileName = "note_content_${System.currentTimeMillis()}.txt"
                        val file = File(requireContext().filesDir, contentFileName)
                        file.writeText(content)
                        contentToStore = null
                    } else {
                        // Store the note content directly in the database
                        contentFileName = null
                        contentToStore = content
                    }

                    // Implement logic to create an abstract from the content
                    val noteAbstract = splitAbstractDetail(content)

                    // Create the Note object
                    val note = Note(
                        noteId = if (noteId != 0) noteId else 0, // Use existing noteId or 0 for new note
                        userId = userId,
                        title = title,
                        content = contentToStore,
                        contentFileName = contentFileName,
                        lastEdited = Calendar.getInstance().time,
                        noteAbstract = noteAbstract
                    )

                    // Insert or update the note in the Room database using the DAO method
                    if (noteId == 0) {
                        // New note, insert
                        noteId = noteDB.noteDao().insertNote(note).toInt()
                    } else {
                        // Existing note, update
                        noteDB.noteDao().updateNote(note)
                    }

                    // Switch back to the main thread to navigate the UI after saving
                    withContext(Dispatchers.Main) {
                        // Navigate back to the previous screen (e.g., after saving the note)
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
