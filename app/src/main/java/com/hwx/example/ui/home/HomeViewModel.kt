package com.hwx.example.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    val text = MutableLiveData("This is home Fragment")
}