package com.cs407.lab5_milestone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.NoteSummary
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val notes: List<NoteSummary>,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (NoteSummary) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList: List<Note> = listOf()

    fun submitList(noteList: List<Note>) {
        this.noteList = noteList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(noteList[position])
    }

    override fun getItemCount(): Int = noteList.size

    class NoteViewHolder(
        itemView: View,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (NoteSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val noteTitle: TextView = itemView.findViewById(R.id.titleTextView)
        private val noteAbstract: TextView = itemView.findViewById(R.id.abstractTextView)
        private val noteDate: TextView = itemView.findViewById(R.id.dateTextView)

        fun bind(note: Note) {
            noteTitle.text = note.noteTitle
            noteAbstract.text = note.noteAbstract
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            noteDate.text = dateFormatter.format(note.lastEdited)

            itemView.setOnClickListener {
                onClick(note.noteId)
            }
        }
    }
}
