package com.erman.usurf.home.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.erman.usurf.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private const val ARG_PATH: String = "arg_path"
private const val ARG_NAME: String = "arg_name"
private const val RESULT_OPTION: String = "result_option"
private const val RESULT_PATH: String = "result_path"
private const val RESULT_NAME: String = "result_name"

/** What can be done with a favourite. */
enum class FavoriteOption { OPEN, RENAME, REMOVE }

/**
 * The options of one favourite: open it, rename it, or remove it from the favourites, which deletes nothing on disk.
 * The sheet is the same every time it opens; a chosen option is sent with the favourite it was opened for.
 */
class FavoriteOptionsSheet : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.sheet_favorite_options, container, false)
        view.findViewById<TextView>(R.id.favoriteNameTextView).text = requireArguments().getString(ARG_NAME)
        mapOf(
            R.id.openFavoriteRow to FavoriteOption.OPEN,
            R.id.renameFavoriteRow to FavoriteOption.RENAME,
            R.id.removeFavoriteRow to FavoriteOption.REMOVE
        ).forEach { (rowId, option) -> view.findViewById<View>(rowId).setOnClickListener { choose(option) } }
        return view
    }

    private fun choose(option: FavoriteOption) {
        val arguments = requireArguments()
        setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                RESULT_OPTION to option.name,
                RESULT_PATH to arguments.getString(ARG_PATH),
                RESULT_NAME to arguments.getString(ARG_NAME)
            )
        )
        dismiss()
    }

    companion object {
        const val REQUEST_KEY = "favorite_options"

        fun newInstance(path: String, name: String): FavoriteOptionsSheet = FavoriteOptionsSheet().apply {
            arguments = bundleOf(ARG_PATH to path, ARG_NAME to name)
        }

        /** The option a result of this sheet names; null when it names none. */
        fun optionIn(result: Bundle): FavoriteOption? = FavoriteOption.entries.find { it.name == result.getString(RESULT_OPTION) }

        fun pathIn(result: Bundle): String = result.getString(RESULT_PATH).orEmpty()

        fun nameIn(result: Bundle): String = result.getString(RESULT_NAME).orEmpty()
    }
}
