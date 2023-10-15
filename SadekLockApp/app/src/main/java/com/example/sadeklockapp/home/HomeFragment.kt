package com.example.sadeklockapp.home

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sadeklockapp.R
import com.example.sadeklockapp.customclass.BaseFragment
import com.example.sadeklockapp.databinding.FragmentHomeBinding
import com.google.firebase.database.*

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val padlockRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("/SadekLockData/PadlockState")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPadlockState()
    }

    private fun checkPadlockState() {
        padlockRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val tempPadlock = dataSnapshot.getValue(Boolean::class.java)
                if(tempPadlock == true) {
                    _binding?.imageViewPadlock?.setImageResource(R.drawable.ic_padlock_opened)

                } else {
                    _binding?.imageViewPadlock?.setImageResource(R.drawable.ic_padlock_closed)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadImage:onCancelled", databaseError.toException())
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}