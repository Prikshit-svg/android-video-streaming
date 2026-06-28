package com.example.videostreamingapp.viewModels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CamViewModel(): ViewModel() {
   private val _selectedCamera = mutableStateOf(false)
    val selectedCam: MutableState<Boolean> = _selectedCamera

    fun changeCam(){
        _selectedCamera.value=!_selectedCamera.value
    }
}