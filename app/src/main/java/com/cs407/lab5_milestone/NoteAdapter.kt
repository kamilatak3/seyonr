package com.cs407.lab5_milestone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(itemView, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class NoteViewHolder(
        itemView: View,
        private val onClick: (Note) -> Unit,
        private val onLongClick: (Note) -> Unit
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
                onClick(note)
            }

            itemView.setOnLongClickListener {
                onLongClick(note)
                true
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.noteId == newItem.noteId
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
