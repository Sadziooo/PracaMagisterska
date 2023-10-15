package com.example.sadeklockapp.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.sadeklockapp.R
import com.example.sadeklockapp.databinding.HistoryListElementBinding

class HistoryAdapter(private val context: Context, private val historyList: List<HistoryFragment.HistoryEntry>) :
    BaseAdapter() {

    private val openedPadlockResource = R.drawable.ic_padlock_opened
    private val closedPadlockResource = R.drawable.ic_padlock_closed

    override fun getCount(): Int {
        return historyList.size
    }

    override fun getItem(position: Int): HistoryFragment.HistoryEntry {
        return historyList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: HistoryListElementBinding
        val historyEntry = historyList[position]

        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            binding = HistoryListElementBinding.inflate(inflater, parent, false)
            binding.root.tag = binding
        } else {
            binding = convertView.tag as HistoryListElementBinding
        }

        with(binding){
            Name.text = historyEntry.Name
            tagID.text = historyEntry.UID
            Timestamp.text = historyEntry.timestamp

            openedOrClosedPadlock.setImageResource(if (historyEntry.Permission) openedPadlockResource else closedPadlockResource)
        }

        return binding.root
    }
}
