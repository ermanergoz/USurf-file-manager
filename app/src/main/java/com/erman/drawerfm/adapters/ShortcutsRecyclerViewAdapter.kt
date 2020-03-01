package com.erman.drawerfm.adapters

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erman.drawerfm.common.MARQUEE_CHOICE_KEY
import com.erman.drawerfm.common.MARQUEE_REPEAT_LIM
import com.erman.drawerfm.R
import com.erman.drawerfm.common.SHARED_PREF_FILE
import com.erman.drawerfm.database.Shortcut
import com.erman.drawerfm.interfaces.OnShortcutClickListener
import kotlinx.android.synthetic.main.shortcut_recycler_layout.view.*

class ShortcutRecyclerViewAdapter(var context: Context) :
    RecyclerView.Adapter<ShortcutRecyclerViewAdapter.ShortcutHolder>() {
    //var shortcutNames: Set<String> = mutableSetOf()
    //var shortcutPaths: Set<String> = mutableSetOf()
    var shortcuts = listOf<Shortcut>()
    private lateinit var onClickCallback: OnShortcutClickListener

    override fun getItemCount(): Int {
        return shortcuts.count()
    }

    override fun onBindViewHolder(holder: ShortcutHolder, position: Int) {
        holder.bindButtons(shortcuts.elementAt(position))

        try {
            onClickCallback = context as OnShortcutClickListener
        } catch (e: Exception) {
            throw Exception("${context} OnShortcutClickListener is not implemented")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutHolder {
        return ShortcutHolder(LayoutInflater.from(parent.context).inflate(R.layout.shortcut_recycler_layout,
                                                                          parent,
                                                                          false))
    }

    inner class ShortcutHolder(var view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {
        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        fun bindButtons(shortcut: Shortcut) {
            itemView.shortcut.text = shortcut.name
            itemView.shortcut.tag = shortcut.path
            itemView.shortcut.setBackgroundResource(R.drawable.storage_button_style)
            itemView.shortcut.isSingleLine = true

            if (itemView.context.getSharedPreferences(SHARED_PREF_FILE,
                                                      Context.MODE_PRIVATE).getBoolean(MARQUEE_CHOICE_KEY, true)) {
                itemView.shortcut.ellipsize =
                    TextUtils.TruncateAt.MARQUEE  //for sliding names if the length is longer than 1 line
                itemView.shortcut.isSelected = true
                itemView.shortcut.marqueeRepeatLimit = MARQUEE_REPEAT_LIM   //-1 is for forever
            }
        }

        override fun onClick(p0: View?) {
            onClickCallback.onClick(itemView.shortcut)
        }

        override fun onLongClick(view: View): Boolean {
            onClickCallback.onLongClick(itemView.shortcut)
            return true
        }
    }

    fun updateData(shortcuts: List<Shortcut>) {
        this.shortcuts = shortcuts
        notifyDataSetChanged()
    }
}