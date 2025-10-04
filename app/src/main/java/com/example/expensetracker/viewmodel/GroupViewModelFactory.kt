package com.example.expensetracker.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.repository.GroupRepository

class GroupViewModelFactory(private val repository: GroupRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
