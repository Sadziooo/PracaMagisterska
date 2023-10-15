package com.example.sadeklockapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.sadeklockapp.R
import com.example.sadeklockapp.customclass.MainViewPageAdapter
import com.example.sadeklockapp.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val PREFS_NAME = "NightModePrefs"
    private var isNightModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerForContextMenu(binding.btnSettingsMain)
        binding.btnSettingsMain.setOnClickListener {
                view -> view.showContextMenu()
        }

        setupNightMode()

        setupTabLayout()
    }

    private fun setupNightMode() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isNightModeEnabled = prefs.getBoolean("NightModeEnabled", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isNightModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    data class FragmentData(val title: String)

    private fun setupTabLayout() {
        val fragmentDataList = listOf<FragmentData>(
            FragmentData("Home"),
            FragmentData("History"),
            FragmentData("Users")
        )

        val tabLayoutMediator = TabLayoutMediator(binding.mainTabLayout, binding.viewPagerMain) { tab, position ->
            tab.text = fragmentDataList[position].title
        }

        binding.viewPagerMain.adapter = MainViewPageAdapter(this, fragmentDataList)
        tabLayoutMediator.attach()
    }

    override fun onPause() {
        super.onPause()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("NightModeEnabled", isNightModeEnabled)
        editor.apply()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.settings_menu, menu)

        val nightModeItem = menu?.findItem(R.id.settings_ColorMode)
        nightModeItem?.isChecked = isNightModeEnabled
    }

    @SuppressLint("SwitchIntDef")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val emailId = FirebaseAuth.getInstance().currentUser!!.email

        when(item.itemId){
            R.id.settings_UserInfo ->
                Toast.makeText(this, "Email :: $emailId" + "\n" + "User ID :: $userId" , Toast.LENGTH_LONG).show()
            R.id.settings_ColorMode ->  {
                isNightModeEnabled = !isNightModeEnabled
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putBoolean("NightModeEnabled", isNightModeEnabled).apply()
                recreate()
                return true
            }
            R.id.settings_LogOut -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        return super.onContextItemSelected(item)
    }

}

