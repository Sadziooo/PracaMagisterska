package com.example.sadeklockapp.users

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.sadeklockapp.customclass.BaseFragment
import com.example.sadeklockapp.databinding.EditUserDialogBinding
import com.example.sadeklockapp.databinding.FragmentUsersBinding
import com.google.firebase.database.*

class UsersFragment : BaseFragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    data class User(
        val Name: String? = "",
        val UID: String? = ""
    ) {
        constructor() : this("", "")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = FirebaseDatabase.getInstance().reference.child("SadekLockData").child("Users")

        // Retrieve and listen for changes in the Users node
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        when (userSnapshot.key) {
                            "User1" -> displayUserData(binding.nameUser1, binding.uidUser1, user)
                            "User2" -> displayUserData(binding.nameUser2, binding.uidUser2, user)
                            "User3" -> displayUserData(binding.nameUser3, binding.uidUser3, user)
                            "User4" -> displayUserData(binding.nameUser4, binding.uidUser4, user)
                            "User5" -> displayUserData(binding.nameUser5, binding.uidUser5, user)
                        }
                    }
                }

                // Clear the user data if no data is available
                if (snapshot.childrenCount == 0L) {
                    clearUserData()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
                Log.e("UsersFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Failed to retrieve users.", Toast.LENGTH_SHORT).show()
            }
        })

        binding.user1Edit.setOnClickListener {
            editUserData(binding.nameUser1, binding.uidUser1, "User1")
        }

        binding.user2Edit.setOnClickListener {
            editUserData(binding.nameUser2, binding.uidUser2, "User2")
        }

        binding.user3Edit.setOnClickListener {
            editUserData(binding.nameUser3, binding.uidUser3, "User3")
        }

        binding.user4Edit.setOnClickListener {
            editUserData(binding.nameUser4, binding.uidUser4, "User4")
        }

        binding.user5Edit.setOnClickListener {
            editUserData(binding.nameUser5, binding.uidUser5, "User5")
        }

    }

    private fun editUserData(nameTextView: TextView, uidTextView: TextView, userKey: String) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val dialogBinding: EditUserDialogBinding = EditUserDialogBinding.inflate(layoutInflater)
        dialogBuilder.setView(dialogBinding.root)

        dialogBinding.editName.setText(nameTextView.text.toString())
        dialogBinding.editUid.setText(uidTextView.text.toString())

        dialogBuilder.setTitle("Edit User")
        dialogBuilder.setMessage("Modify the user's information.")
        dialogBuilder.setPositiveButton("Accept") { _: DialogInterface, _: Int ->
            val name = dialogBinding.editName.text.toString().trim()
            val uid = dialogBinding.editUid.text.toString().trim()
            updateUser(userKey, name, uid)
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    // Function to update user data in the database
    private fun updateUser(userKey: String, name: String, uid: String) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance().reference.child("SadekLockData").child("Users")
        val userUpdates = HashMap<String, Any>()
        userUpdates["$userKey/Name"] = name
        userUpdates["$userKey/UID"] = uid

        database.updateChildren(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(context, "User data updated successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to update user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to display user data in the UI
    private fun displayUserData(nameTextView: TextView, uidTextView: TextView, user: User) {
        nameTextView.text = user.Name
        uidTextView.text = user.UID
    }

    // Function to clear existing user data in the UI
    private fun clearUserData() {
        binding.nameUser1.text = ""
        binding.uidUser1.text = ""
        binding.nameUser2.text = ""
        binding.uidUser2.text = ""
        binding.nameUser3.text = ""
        binding.uidUser3.text = ""
        binding.nameUser4.text = ""
        binding.uidUser4.text = ""
        binding.nameUser5.text = ""
        binding.uidUser5.text = ""
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCapabilities = connectivityManager?.activeNetwork ?: return false
        val networkInfo = connectivityManager.getNetworkCapabilities(networkCapabilities)
        return networkInfo?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}