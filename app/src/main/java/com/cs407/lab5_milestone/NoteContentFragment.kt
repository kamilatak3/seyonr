package com.cs407.lab5_milestone

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

class NoteContentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_content, container, false)
        return view
    }
}