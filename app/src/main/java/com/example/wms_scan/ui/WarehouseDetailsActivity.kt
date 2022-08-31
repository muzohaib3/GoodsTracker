package com.example.wms_scan.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.TypedArrayUtils.getText
import androidx.lifecycle.Observer
import com.example.scanmate.data.callback.Status
import com.example.scanmate.extensions.*
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isRefreshRequired
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityWarehouseDetailsBinding
import java.util.*
import java.util.regex.Pattern

class WarehouseDetailsActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityWarehouseDetailsBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private var selectedBusLocNo:String? = ""
    private var selectedWHNo:String? = ""
    private var updatedWarehouseName = ""
    private var selectedBusLocName:String? = ""
    private var selectedWhName:String? = ""
    private var updatedBusLocNo:String? = ""
    private var updatedBusLocName:String? = ""
    private var addWhCode:String? = ""
    private var dataStatus:String = ""
    private var deviceId = ""
    private var updatedwhCode = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarehouseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initListener()
        initObserver()

    }

    private fun setupUi(){

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)
        dialog = CustomProgressDialog(this)
        deviceId = UUID.randomUUID().toString()

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        when {
            intent.extras?.getBoolean("AddWHKey") == true -> {

                selectedBusLocName = intent.extras?.getString("addBusName")
                selectedBusLocNo = intent.extras?.getString("addBusLocNo")
                selectedWHNo = intent.extras?.getString("addWhNo")
                addWhCode = intent.extras?.getString("addWhCode")
                binding.busLocTV.text = selectedBusLocName
                binding.updateWarehouseET.text = selectedWhName?.toEditable()
                Log.i("warehouseValues","THE KEYS ARE VALUES WITH selectedBusLocNo :$selectedBusLocNo\n $selectedBusLocName\n $selectedWHNo")
            }

            intent.extras?.getBoolean("UpdateWHKey") == true -> {
                updatedBusLocName = intent.extras?.getString("updateBusName")
                updatedBusLocNo = intent.extras?.getString("updateBusLocNo")
                selectedWHNo = intent.extras?.getString("updateWhNo")
                selectedWhName = intent.extras?.getString("updateWhName")
                updatedwhCode = intent.extras?.getString("updateWhCode").toString()
                binding.editDetailTV.text = "Update to"
                binding.busLocTV.text = updatedBusLocName
                binding.updateWarehouseET.text = selectedWhName?.toEditable()
                binding.addWarehouseBTN.gone()
                binding.updateWarehouseBtn.visible()
                binding.updateWarehouseET.hint = "Update warehouse"
                Log.i("updateWHValues","THE KEYS ARE VALUES ARE :$selectedWHNo \n$selectedWhName")
            }
        }
    }

    private fun initListener(){

        binding.addWarehouseBTN.click {

            updatedWarehouseName = binding.updateWarehouseET.text.toString()
            val special = Pattern.compile("[!@#\$%&*()_+=|<>?{}\\[\\]:;^`.~£-]")
            val hasSpecial = special.matcher(updatedWarehouseName)
            val constainsSymbols: Boolean = hasSpecial.find()

            if (binding.updateWarehouseET.text.isNullOrEmpty())
            {
                toast("Field must not be empty")
                binding.updateWarehouseET.error = "Field must not be empty"
            }
            else if(selectedBusLocNo.isNullOrEmpty())
            {
                toast("warehouse cannot be added")
            }
            else if(updatedWarehouseName.startsWith(" "))
            {
                toast("Please enter any value")
            }
            else if (constainsSymbols)
            {
                toast("Please enter any value")
            }
            else if(updatedWarehouseName.startsWith("0"))
            {
                toast("Please enter any value")
            }
            else
            {
                if(isNetworkConnected(this))
                {
                    viewModel.addUpdateWarehouse(
                        "0",
                        updatedWarehouseName,
                        "$addWhCode",
                        "$selectedBusLocNo",
                        LocalPreferences.getInt(this, userNo).toString(),
                        "$deviceId"
                    )
                    LocalPreferences.put(this, isRefreshRequired, true)
                    Log.i("addWH","1.0\n 2.$updatedWarehouseName\n 3.$addWhCode\n4.$selectedBusLocNo\n5.1\n6.1")
                }
                else
                {
                    toast("No connection found")
                }


            }
        }

        binding.updateWarehouseBtn.click {

            updatedWarehouseName = binding.updateWarehouseET.text.toString()
            val special = Pattern.compile("[!@#\$%&*()_+=|<>?{}\\[\\]:;^`.~£-]")
            val hasSpecial = special.matcher(updatedWarehouseName)
            val constainsSymbols: Boolean = hasSpecial.find()

            if (binding.updateWarehouseET.text.isNullOrEmpty())
            {
                toast("Field must not be empty")
                binding.updateWarehouseET.error = "Field must not be empty"
            }
            else if(updatedBusLocNo.isNullOrEmpty())
            {
                toast("warehouse cannot be added")
            }
            else if(updatedWarehouseName.startsWith(" "))
            {
                toast("Please enter any value")
            }
            else if (constainsSymbols)
            {
                toast("Please enter any value")
            }
            else if(updatedWarehouseName.startsWith("0"))
            {
                toast("Please enter any value")
            }
            else if (binding.busLocTV.text == null)
            {
                toast("Error found")
            }
            else
            {
                if(isNetworkConnected(this))
                {
                    viewModel.addUpdateWarehouse(
                        "$selectedWHNo",
                        "$updatedWarehouseName",
                        "$updatedwhCode",
                        "$updatedBusLocNo",
                        LocalPreferences.getInt(this, userNo).toString(),
                        "$deviceId"
                    )
                    Log.i("updateWH","1.0\n 2.$updatedWarehouseName\n 3.$addWhCode\n4.$selectedBusLocNo\n5.1\n6.1")
                }
                else
                {
                    toast("No connection found")
                }

            }
        }

        binding.backBtn.click {
            onBackPressed()
        }
    }

    private fun initObserver(){

        /**
         *       ADD WAREHOUSE OBSERVER
         */

        viewModel.addUpdateWarehouse.observe(this, Observer {
            it.let {
                when(it.status){
                    Status.LOADING ->{
                        dialog.show()
                        dialog.setCanceledOnTouchOutside(true)
                    }
                    Status.SUCCESS ->{
                        dataStatus = it.data?.status.toString()

                        if (it.data?.status == true){
                            toast(it.data.error!!)
                            dialog.dismiss()
                            Log.i("warehouseAdded",it.data.error.toString())
                            LocalPreferences.put(this, isRefreshRequired, true)
                        }
                        else
                        {
                            toast(it.data?.error!!)
                        }
                        finish()

                    }
                    Status.ERROR ->{
                        dialog.dismiss()
                        toast(it.data?.error!!)
                    }
                }
            }
        })

    }

    private fun clearPreferences(context: Context)
    {
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        onBackPressed()
    }


}