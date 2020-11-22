package com.hwx.flowing.navigation

import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI

object NavigationUtil {

    fun setupWithNavController(
        bottomNavigationView: FlowingBottomNavigationView,
        navController: NavController
    ) {
        bottomNavigationView.listener =
            object : FlowingBottomNavigationView.OnItemSelectedListener {
                override fun onItemSelected(item: MenuItem) {
                    NavigationUI.onNavDestinationSelected(
                        item,
                        navController
                    )
                }
            }
    }
}