package com.hwx.example.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    val text = MutableLiveData("This is dashboard Fragment")
}