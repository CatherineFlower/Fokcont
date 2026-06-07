package ru.fokcont.app.data.repository

import ru.fokcont.app.data.db.dao.UserDao
import ru.fokcont.app.data.db.entity.UserEntity

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun getUser(): UserEntity? {
        return userDao.getUser()
    }

    suspend fun hasUser(): Boolean {
        return userDao.getUserCount() > 0
    }
}