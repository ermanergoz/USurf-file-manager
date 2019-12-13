package com.erman.drawerfm.fragmentsimport DirectoryDataimport android.content.Contextimport android.os.Bundleimport android.view.LayoutInflaterimport android.view.Viewimport android.view.ViewGroupimport androidx.fragment.app.Fragmentimport androidx.recyclerview.widget.LinearLayoutManagerimport com.erman.drawerfm.Rimport com.erman.drawerfm.adapters.DirectoryRecyclerViewAdapterimport getDirectoryDataimport getFilesimport kotlinx.android.synthetic.main.fragment_file_list.*class ListDirFragment : Fragment() {    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter    private lateinit var path: String    private lateinit var onClickCallback: OnItemClickListener    interface OnItemClickListener {        fun onClick(directoryData: DirectoryData)        fun onLongClick(directoryData: DirectoryData)    }    companion object {        fun buildFragment(path: String, isMarqueeEnabled: Boolean): ListDirFragment {            val fragment = ListDirFragment()            val argumentBundle = Bundle()            argumentBundle.putString("path", path)            argumentBundle.putBoolean("isMarqueeEnabled", isMarqueeEnabled)            fragment.arguments = argumentBundle            return fragment        }    }    override fun onAttach(context: Context) {        super.onAttach(context)        try {            onClickCallback = context as OnItemClickListener        } catch (e: Exception) {            throw Exception("${context} should implement com.erman.drawerfm.fragments.ListDirFragment.OnItemCLickListener")        }    }    override fun onCreateView(        inflater: LayoutInflater,        container: ViewGroup?,        savedInstanceState: Bundle?    ): View? {        return inflater.inflate(R.layout.fragment_file_list, container, false)    }    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {        super.onViewCreated(view, savedInstanceState)        val filePath = arguments?.getString("path")        path = filePath.toString()        //pathTextView.text = path        initViews()    }    private fun initViews() {        fileListRecyclerView.layoutManager = LinearLayoutManager(context)        directoryRecyclerViewAdapter = DirectoryRecyclerViewAdapter()        fileListRecyclerView.adapter = directoryRecyclerViewAdapter        directoryRecyclerViewAdapter.onClickListener = {            onClickCallback.onClick(it)        }        directoryRecyclerViewAdapter.onLongClickListener = {            onClickCallback.onLongClick(it)        }        updateData()    }    private fun updateData() {        val files = getDirectoryData(getFiles(path))        directoryRecyclerViewAdapter.updateData(files)    }}