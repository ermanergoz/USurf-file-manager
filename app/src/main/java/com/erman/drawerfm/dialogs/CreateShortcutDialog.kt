import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.drawerfm.R

class CreateShortcutDialog : DialogFragment() {
    lateinit var shortcutPath: String
    lateinit var shortcutName: String
    private lateinit var listener: DialogCreateShortcutListener

    private lateinit var pathEditText: EditText
    private lateinit var nameEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.create_shortcut_dialog, null)

            this.pathEditText = dialogView.findViewById(R.id.pathEditText)
            this.nameEditText = dialogView.findViewById(R.id.nameEditText)

            // Create the AlertDialog object and return it
            builder.setMessage(R.string.create_shortcut)

                .setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { dialog, id ->
                        // Send the positive button event back to the host activity
                        shortcutPath = this.pathEditText.text.toString()
                        shortcutName = this.nameEditText.text.toString()

                        listener.dialogCreateShortcutListener(shortcutPath, shortcutName)
                    })
                .setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as DialogCreateShortcutListener
        } catch (err: ClassCastException) {
            throw ClassCastException(
                (context.toString() + " must implement CreateShortcutDialogListener")
            )
        }
    }

    interface DialogCreateShortcutListener {
        fun dialogCreateShortcutListener(shortcutPath: String, shortcutName: String)
    }
}