package com.tech7fox.myoassistant

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.MenuHost
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        (requireActivity() as MainActivity).setMenuVisibility(false)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        (requireActivity() as MainActivity).setMenuVisibility(true)
        super.onDestroy()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}