package com.bitcoin.tokenwallet.send

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController

class ScanViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    private lateinit var navController: NavController


    fun initialize(navController: NavController) {
        this.navController = navController
    }

    fun onBack() {
        navController.popBackStack()
    }
}
