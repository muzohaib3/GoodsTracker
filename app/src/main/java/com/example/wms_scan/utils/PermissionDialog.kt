package com.example.wms_scan.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.example.scanmate.extensions.click
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityPermissionDialogBinding

class PermissionDialog(context: Context) : Dialog(context, R.style.customDialog)
{
    private lateinit var viewModel: MainViewModel

    init {
        val binding: ActivityPermissionDialogBinding = ActivityPermissionDialogBinding.inflate(LayoutInflater.from(context))

        setTitle(null)
        setCancelable(false)
        setOnCancelListener(null)
        binding.yesTV.click {
            dismiss()
            Toast.makeText(this.context, "Data saved", Toast.LENGTH_SHORT).show()
        }
        binding.noTV.click {
            dismiss()
        }
        binding.closeIV.click {
            dismiss()
        }
        //window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }
}