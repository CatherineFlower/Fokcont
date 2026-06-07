package ru.fokcont.app.data.repository

import ru.fokcont.app.data.db.dao.NoteDao
import ru.fokcont.app.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    suspend fun insertNote(note: NoteEntity): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    fun getNotesBySession(sessionId: Int): Flow<List<NoteEntity>> {
        return noteDao.getNotesBySession(sessionId)
    }

    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    suspend fun deleteNote(noteId: Int) {
        noteDao.deleteNote(noteId)
    }

    suspend fun deleteNotesBySession(sessionId: Int) {
        noteDao.deleteNotesBySession(sessionId)
    }
}