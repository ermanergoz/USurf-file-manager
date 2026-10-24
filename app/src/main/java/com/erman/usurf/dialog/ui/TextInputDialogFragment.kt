package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.erman.usurf.R
import com.erman.usurf.directory.model.NameRules
import com.google.android.material.textfield.TextInputLayout

private const val ARG_REQUEST: String = "arg_request"
private const val RESULT_TEXT: String = "result_text"
private const val CANCEL_KEY_SUFFIX: String = "_cancelled"

/**
 * The one dialog that asks for a name or a query. OK is enabled only while the text is acceptable: a name passes
 * [NameRules], the rule the model applies too, and a query needs some text. Back, a tap outside and Cancel send no
 * text; they send the request's payload under [cancelKeyOf] its key, for a screen that waits on the answer.
 */
class TextInputDialogFragment : DialogFragment() {
    private val request: TextInputRequest
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), ARG_REQUEST, TextInputRequest::class.java))

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val request = request
        val view = layoutInflater.inflate(R.layout.dialog_text_input, null)
        val inputLayout: TextInputLayout = view.findViewById(R.id.textInputLayout)
        val field: EditText = view.findViewById(R.id.textInputEditText)
        inputLayout.hint = getString(request.hint)
        if (savedInstanceState == null) {
            field.setText(request.defaultText)
            field.setSelection(0, request.selectedLength?.coerceIn(0, request.defaultText.length) ?: request.defaultText.length)
        }
        val builder =
            AlertDialog.Builder(requireActivity())
                .setTitle(request.title)
                .setView(view)
                .setPositiveButton(request.positiveButton) { _, _ -> sendText(field.text.toString()) }
                .setNegativeButton(R.string.cancel) { _, _ -> sendCancelled() }
        if (request.message != 0) builder.setMessage(request.message)
        val dialog = builder.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener { showVerdict(dialog, inputLayout, field.text.toString()) }
        field.doAfterTextChanged { text -> showVerdict(dialog, inputLayout, text?.toString().orEmpty()) }
        field.setOnEditorActionListener { _, actionId, _ ->
            val isSubmitted = actionId == EditorInfo.IME_ACTION_DONE && acceptsText(field.text.toString())
            if (isSubmitted) {
                sendText(field.text.toString())
                dismiss()
            }
            isSubmitted
        }
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        sendCancelled()
    }

    // An empty field only disables OK; a name the rule refuses also says why, under the field.
    private fun showVerdict(dialog: AlertDialog, inputLayout: TextInputLayout, text: String) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = acceptsText(text)
        val isRefusedName = text.isNotEmpty() && request.validateAsName && NameRules.validate(text) != null
        inputLayout.error = if (isRefusedName) getString(R.string.invalid_item_name) else null
    }

    private fun acceptsText(text: String): Boolean = text.isNotEmpty() && (!request.validateAsName || NameRules.validate(text) == null)

    private fun sendText(text: String) {
        setFragmentResult(request.requestKey, Bundle(request.payload).apply { putString(RESULT_TEXT, text) })
    }

    private fun sendCancelled() {
        setFragmentResult(cancelKeyOf(request.requestKey), Bundle(request.payload))
    }

    companion object {
        fun newInstance(request: TextInputRequest): TextInputDialogFragment = TextInputDialogFragment().apply {
            arguments = argumentsFor(request)
        }

        /** The arguments a dialog for [request] is created with. */
        fun argumentsFor(request: TextInputRequest): Bundle = bundleOf(ARG_REQUEST to request)

        /** The text a result of this dialog carries. */
        fun textIn(result: Bundle): String = result.getString(RESULT_TEXT).orEmpty()

        /** The key a dialog sent under [requestKey] uses to tell that it was cancelled. */
        fun cancelKeyOf(requestKey: String): String = requestKey + CANCEL_KEY_SUFFIX
    }
}
