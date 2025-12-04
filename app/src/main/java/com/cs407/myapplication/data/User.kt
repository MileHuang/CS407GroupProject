package com.cs407.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
@Entity(
    indices = [Index(
        value = ["userUID"],
        unique = true
    )]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val userUID: String = ""
)