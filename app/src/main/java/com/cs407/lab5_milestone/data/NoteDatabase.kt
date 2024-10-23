package com.cs407.lab5_milestone.data

import android.content.Context
import androidx.room.*
import java.util.Date

// TypeConverters for Date
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

// 1. Define the User entity
@Entity(tableName = "User")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val userName: String
    // Password is stored in SharedPreferences; omit from database for security reasons
)

// 2. Define the Note entity
@Entity(
    tableName = "Note"
)
data class Note(
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val userId: Int,
    val noteTitle: String,
    val noteAbstract: String,
    val noteDetail: String?,
    val notePath: String?,
    val lastEdited: Date
)

// 3. Define the UserNote entity (Join Table)
@Entity(
    tableName = "UserNote",
    primaryKeys = ["userId", "noteId"],
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"]),
        ForeignKey(entity = Note::class, parentColumns = ["noteId"], childColumns = ["noteId"])
    ]
)
data class UserNote(
    val userId: Int,
    val noteId: Int
)

// 4. Create the UserDao interface
@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM User WHERE userName = :userName LIMIT 1")
    suspend fun getByName(userName: String): User?

    @Query("SELECT * FROM User WHERE userId = :userId")
    suspend fun getById(userId: Int): User?
}

// 5. Create the NoteDao interface
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM Note WHERE noteId = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    @Query("SELECT * FROM Note WHERE noteId = :noteId")
    suspend fun getNoteById(noteId: Int): Note?

    @Query("""
        SELECT Note.* FROM Note 
        INNER JOIN UserNote ON Note.noteId = UserNote.noteId 
        WHERE UserNote.userId = :userId 
        ORDER BY Note.lastEdited DESC
    """)
    suspend fun getAllNotes(userId: Int): List<Note>
}

// 6. Create the UserNoteDao interface
@Dao
interface UserNoteDao {
    @Insert
    suspend fun insertUserNote(userNote: UserNote)

    @Delete
    suspend fun deleteUserNote(userNote: UserNote)

    @Transaction
    @Query("SELECT * FROM Note INNER JOIN UserNote ON Note.noteId = UserNote.noteId WHERE UserNote.userId = :userId ORDER BY Note.lastEdited DESC")
    suspend fun getNotesForUser(userId: Int): List<Note>
}

// 7. Set up the NoteDatabase class
@Database(entities = [User::class, Note::class, UserNote::class], version = 2)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun userNoteDao(): UserNoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .fallbackToDestructiveMigration() // Use this to drop and recreate the database
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
