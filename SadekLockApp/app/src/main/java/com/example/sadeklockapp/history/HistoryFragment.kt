package com.example.sadeklockapp.history

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sadeklockapp.customclass.BaseFragment
import com.example.sadeklockapp.databinding.FragmentHistoryBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class HistoryFragment : BaseFragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val historyList = ArrayList<HistoryEntry>()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    data class HistoryEntry(
        val Name: String = "",
        val UID: String = "",
        val timestamp: String = "",
        val Permission: Boolean = false
    ) {
        // Add a no-argument constructor
        constructor() : this("", "", "", false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = binding.swipeRefreshLayout
        historyAdapter = HistoryAdapter(requireContext(), historyList)
        binding.listviewHistory.adapter = historyAdapter

        if (historyList.isEmpty()) {
            if (isInternetAvailable()) {
                databaseUpdateHistory()
            } else {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            }
        } else {
            historyAdapter.notifyDataSetChanged()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                databaseUpdateHistory()
            } else {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }

    }

    private fun databaseUpdateHistory() {
        historyList.clear() // Clear the previous entries
        val database = FirebaseDatabase.getInstance().reference
        val historyQuery = database.child("/SadekLockData/History").orderByKey().limitToLast(10)

        historyQuery.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val historyEntry = dataSnapshot.getValue(HistoryEntry::class.java)
                if (historyEntry != null) {
                    historyList.add(0, historyEntry)
                    historyAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val updatedHistoryEntry = dataSnapshot.getValue(HistoryEntry::class.java)
                if (updatedHistoryEntry != null) {
                    val index = historyList.indexOfFirst { it.UID == updatedHistoryEntry.UID }
                    if (index != -1) {
                        historyList.removeAt(index)
                        // Add the updated entry back at the same position
                        historyList.add(index, updatedHistoryEntry)
                        // Notify the adapter of the change at the specific position
                        historyAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val removedHistoryEntry = dataSnapshot.getValue(HistoryEntry::class.java)
                if (removedHistoryEntry != null) {
                    historyList.remove(removedHistoryEntry)
                    historyAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // Handle child moved
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
