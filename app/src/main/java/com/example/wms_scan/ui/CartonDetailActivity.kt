package com.example.wms_scan.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetShelfResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.pallets
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.rack
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.shelf
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.warehouse
import com.example.scanmate.util.Utils
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.pallets.PalletsAdapter
import com.example.wms_scan.data.response.GetPalletResponse
import com.example.wms_scan.databinding.ActivityCartonDetailBinding
import com.example.wms_scan.utils.PermissionDialog
import java.lang.Exception


class CartonDetailActivity : AppCompatActivity() {
    private lateinit var binding:ActivityCartonDetailBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var permissionDialog: PermissionDialog
    private var analyticalNo = ""
    private var totCarton = ""
    private var pilotNo = ""
    private var cartonSNo = ""
    private var cartonCode = ""
    private var cartonNo = ""
    private var itemCode = ""
    private var stock = ""
    private var selectedPalletNo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartonDetailBinding.inflate(layoutInflater)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setContentView(binding.root)
        permissionDialog = PermissionDialog(this)
        initListener()
        initObserver()
        setupUi()

    }

    private fun initListener(){

        binding.backBtn.click {
            onBackPressed()
        }

        binding.saveBtn.click {
            with(viewModel) {

                this.addCarton(
                    CartonNo = Utils.getSimpleTextBody("0"),
                    CartonCode = Utils.getSimpleTextBody(cartonCode),
                    ItemCode = Utils.getSimpleTextBody("TEST"), //
                    PilotNo = Utils.getSimpleTextBody("2"),
                    AnalyticalNo = Utils.getSimpleTextBody(analyticalNo), //
                    Carton_SNo = Utils.getSimpleTextBody(cartonSNo), //
                    TotCarton = Utils.getSimpleTextBody(totCarton), //
                    LocationNo = Utils.getSimpleTextBody("1"),
                    DMLUserNo = Utils.getSimpleTextBody("2"),
                    DMLPCName = Utils.getSimpleTextBody("test")
                )
                Log.i("IntentSave","\n$cartonCode\n $cartonNo\n $cartonSNo\n $itemCode\n $pilotNo\n $analyticalNo\n $cartonSNo\n $totCarton")

            }
        }

        binding.updateBtn.click {

            viewModel.addCarton(
                Utils.getSimpleTextBody(cartonNo),
                CartonCode = Utils.getSimpleTextBody(cartonCode),
                ItemCode = Utils.getSimpleTextBody("TEST"), //
                PilotNo = Utils.getSimpleTextBody(selectedPalletNo),
                AnalyticalNo = Utils.getSimpleTextBody(analyticalNo), //
                Carton_SNo = Utils.getSimpleTextBody(cartonSNo), //
                TotCarton = Utils.getSimpleTextBody(totCarton), //
                LocationNo = Utils.getSimpleTextBody("1"),
                DMLUserNo = Utils.getSimpleTextBody("2"),
                DMLPCName = Utils.getSimpleTextBody("test"),
            )
            Log.i("IntentUpdate","$cartonCode $cartonNo $cartonSNo $itemCode $pilotNo $analyticalNo $cartonSNo $totCarton")
        }

        binding.logout.click {
            clearPreferences(this)
        }

        binding.closeBtn.click {
            binding.selectPalletCont.gone()
            binding.palletValuesCont.visible()
        }

        binding.changeTV.click {
            binding.palletValuesCont.gone()
            binding.selectPalletCont.visible()
        }

    }

    private fun initObserver(){

        viewModel.addCarton.observe(this, Observer {
            when(it.status){
                Status.LOADING->{

                }
                Status.SUCCESS->{
                    it.let {
                        try
                        {
                            if(it.data?.status == true)
                            {
                                Log.i("addCarton",it.data.status.toString())
                                toast(it.data.error.toString())
                            }
                        }
                        catch (e:Exception)
                        {

                        }
                    }
                }
                Status.ERROR->{

                }
            }
        })

        viewModel.getPallet(
            Utils.getSimpleTextBody(""),
            Utils.getSimpleTextBody("11"),
            Utils.getSimpleTextBody("1"),
        )
        viewModel.getPallet.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{
                    Log.i(Constants.LogMessages.loading,"Loading")
                }
                Status.SUCCESS ->{
                    try
                    {
                        LocalPreferences.put(this,
                            LocalPreferences.AppLoginPreferences.isRefreshRequired, true)
                        if(it.data?.get(0)?.status == true)
                        {
                            Log.i(Constants.LogMessages.success,"Success")
                            showPalletSpinner(it.data)
                        }
                        else { }
                    }
                    catch (e:Exception)
                    {
                        Log.i("exception","${e.message}")
                        Log.i("rackAdapter","${e.stackTrace}")
                    }

                }
                Status.ERROR ->{
                    Log.i(Constants.LogMessages.error,"Success")
                }
            }
        })

    }

    private fun setupUi(){

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)

        analyticalNo = intent.extras?.getString("Analytical_No").toString()
        val materialId = intent.extras?.getString("material_id")
        stock = intent.extras?.getString("matStock").toString()
        itemCode = intent.extras?.getString("itemCode").toString()
        totCarton = intent.extras?.getString("totCarton").toString()
        pilotNo = intent.extras?.getInt("pilotNo").toString()
        cartonSNo = intent.extras?.getString("cartonSNo").toString()
        cartonCode = intent.extras?.getString("cartonCode").toString()
        cartonNo = intent.extras?.getInt("cartonNo").toString()
        val pilotName = intent.extras?.getString("pilotName").toString()
        val pilotCode = intent.extras?.getString("pilotCode").toString()

        Log.i("pilotNo",pilotNo)

        binding.analyticalNumTV.text = analyticalNo
        binding.materialNumTV.text = materialId
        binding.stockTV.text = stock
        binding.cartonNumTV.text = cartonNo
        binding.palletCode.text = "Pallet Code : $pilotCode"
        binding.palletName.text = "Pallet Name : $pilotName"
        binding.palletNo.text = "Pallet no : $pilotNo"

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        binding.WHTV.text = LocalPreferences.getString(this, warehouse)
        binding.rackTV.text =  LocalPreferences.getString(this, rack)
        binding.shelfTV.text =  LocalPreferences.getString(this, shelf)
        binding.palletTV.text =  LocalPreferences.getString(this, pallets)

        when
        {
            intent.extras?.getInt("isExist") == 1 ->{
                binding.updateBtn.visible()
                binding.saveBtn.gone()
                binding.palletNo.visible()

            }
            intent.extras?.getInt("isExist") == 0 ->{
                binding.palletCode.gone()
                binding.palletName.gone()
                binding.palletNo.gone()
                binding.palletView.gone()
            }
        }
    }


    private fun showPalletSpinner(data:List<GetPalletResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val shelfResponse = binding.palletSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].pilotName
            val adapter: ArrayAdapter<String?> =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
            //setting adapter to spinner
            shelfResponse.adapter = adapter
            shelfResponse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

                override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {
                    if (Utils.isNetworkConnected(this@CartonDetailActivity))
                    {
                        Log.i("PalletNo","This is pallet pos ${adapter?.getItemAtPosition(position)}")
                        selectedPalletNo = data[position].pilotNo.toString()
                    }
                    else
                    {
                        toast(Constants.Toast.NoInternetFound)
                    }

                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }

    private fun clearPreferences(context: Context){
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        gotoActivity(LoginActivity::class.java)
    }

    override fun onBackPressed() {
        finish()
    }

}