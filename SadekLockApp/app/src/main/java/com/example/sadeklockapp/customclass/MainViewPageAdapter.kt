package com.example.sadeklockapp.customclass

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sadeklockapp.activities.MainActivity
import com.example.sadeklockapp.history.HistoryFragment
import com.example.sadeklockapp.home.HomeFragment
import com.example.sadeklockapp.users.UsersFragment
import java.lang.RuntimeException

class MainViewPageAdapter(
    activity: AppCompatActivity,
    private val fragmentDataList: List<MainActivity.FragmentData>
): FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return fragmentDataList.size
    }

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> HomeFragment()
            1 -> HistoryFragment()
            2 -> UsersFragment()
            else -> throw RuntimeException("Invalid position : $position")
        }
    }
}