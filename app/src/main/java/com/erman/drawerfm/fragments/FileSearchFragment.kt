package com.erman.drawerfm.fragments

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.erman.drawerfm.R
import com.erman.drawerfm.adapters.DirectoryRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_file_list.*
import java.io.File

class FileSearchFragment : Fragment() {
    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter
    private lateinit var onClickCallback: OnItemClickListener
    private var fileList: List<Parcelable>? = null

    interface OnItemClickListener {
        fun onClick(directory: File)
    }

    companion object {
        fun buildSearchFragment(fileList: List<File>): FileSearchFragment {
            val fragment = FileSearchFragment()
            val argumentBundle = Bundle()
            val fileArrayList = ArrayList<File>(fileList)
            argumentBundle.putParcelableArrayList(
                "files",
                fileArrayList as ArrayList<out Parcelable>
            )
            fragment.arguments = argumentBundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileList = arguments?.getParcelableArrayList<Parcelable>("files")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            onClickCallback = context as FileSearchFragment.OnItemClickListener
        } catch (e: Exception) {
            throw Exception("${context} fragments.FileSearchFragment.OnItemCLickListener is not implemented")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_file_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        fileListRecyclerView.layoutManager = LinearLayoutManager(context)
        directoryRecyclerViewAdapter = DirectoryRecyclerViewAdapter()
        fileListRecyclerView.adapter = directoryRecyclerViewAdapter

        directoryRecyclerViewAdapter.onClickListener = {
            onClickCallback.onClick(it)
        }
        updateData()
    }

    private fun updateData() {
        try {
            if (fileList!!.isEmpty()) {
                emptyFolderTextView.isVisible = true
                emptyFolderTextView.text = getString(R.string.unsuccessful_search)
            } else
                emptyFolderTextView.isVisible = false

            directoryRecyclerViewAdapter.updateData(fileList as List<File>)
        } catch (err: IllegalStateException) {
            Toast.makeText(
                context,
                getString(R.string.unable_to_execute_search_query),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}