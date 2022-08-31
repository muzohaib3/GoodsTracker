package com.example.wms_scan.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetRackResponse
import com.example.scanmate.data.response.GetWarehouseResponse
import com.example.scanmate.data.response.UserLocationResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.Toast.NoInternetFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isRefreshRequired
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isSpinnerSelected
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.racks.RackAdapter
import com.example.wms_scan.databinding.ActivityRacksBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RacksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRacksBinding
    private lateinit var racksAdapter: RackAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private var selectedBusLocNo = ""
    private var selectedWareHouseNo = ""
    private var selectedRackNo = ""
    private var selectedRackName = ""
    private var selectedRackCode = ""
    private var businessLocName = ""
    private var warehouseName = ""
    private var capacity = ""
    private var rackCode = ""
    private lateinit var bmp:Bitmap
    private val bmpList = mutableListOf<Bitmap>()
    private var STORAGE_CODE = 1001
    private var whCode = ""
    private val textList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRacksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initListeners()
        initObserver()


    }

    private fun setupUi(){

        dialog = CustomProgressDialog(this)
        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

    }

    private fun initListeners(){

        binding.rackAddBTN.click {

            if (isNetworkConnected(this))
            {

                val intent = Intent(this, AddUpdateRackDetails::class.java)
                intent.putExtra("addBusNo",selectedBusLocNo)
                intent.putExtra("addBusName",businessLocName)
                intent.putExtra("addWHNo",selectedWareHouseNo)
                intent.putExtra("addWHName",warehouseName)
                intent.putExtra("addRackNo",selectedRackNo)
                intent.putExtra("addRackName",selectedRackName)
                intent.putExtra("addRackCode",rackCode)
                intent.putExtra("shelfCapacity",capacity)
                intent.putExtra("AddRackKey",true)
                startActivity(intent)

                Log.i("addRack","$selectedBusLocNo $businessLocName $selectedWareHouseNo $warehouseName $selectedRackNo $selectedRackName $rackCode $capacity" )

            }
            else
            {
                toast(NoInternetFound)
            }
        }

        binding.swipeRefresh.setOnRefreshListener {

            if (isNetworkConnected(this))
            {
                binding.rackCont.visible()
                binding.connectionTimeout.gone()
                viewModel.userLocation(Utils.getSimpleTextBody(selectedBusLocNo))
                /*viewModel.getWarehouse("", LocalPreferences.getString(this, isSpinnerSelected).toString())
                viewModel.getRack(
                    Utils.getSimpleTextBody(""),
                    Utils.getSimpleTextBody(LocalPreferences.getString(this, isSpinnerSelected).toString()),
                    Utils.getSimpleTextBody(selectedBusLocNo)
                )*/

            }
            else
            {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.backBtn.click {
            onBackPressed()
        }

    }

    fun openActivity(rackName: String?, rackNo: String, rackCode: String,capacity:String){

        if (isNetworkConnected(this))
        {
            Log.i("Warehouse","$rackName $rackNo")
            val intent = Intent(this, AddUpdateRackDetails::class.java)
            intent.putExtra("updateBusNo",selectedBusLocNo)
            intent.putExtra("updateBusName",businessLocName)
            intent.putExtra("updateWHNo",selectedWareHouseNo)
            intent.putExtra("updateWhName",warehouseName)
            intent.putExtra("updateRackName",rackName)
            intent.putExtra("updateRackNo",rackNo)
            intent.putExtra("updateRackCode",rackCode)
            intent.putExtra("updateShelfCapacity",capacity)
            intent.putExtra("updateRackKey",true)
            startActivity(intent)

        }
        else
        {
            toast(NoInternetFound)
        }
    }

    private fun initObserver(){

        /**
         *      USER LOCATION OBSERVER
         */

        viewModel.userLocation(
            Utils.getSimpleTextBody(
                LocalPreferences.getInt(this,
                    LocalPreferences.AppLoginPreferences.userNo
                ).toString()
            ))
        viewModel.userLoc.observe(this, Observer {
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status){
                        Status.LOADING->{
                            dialog.setCanceledOnTouchOutside(true);
                        }
                        Status.SUCCESS ->{
                            if(it.data?.get(0)?.status == true)
                            {
                                dialog.dismiss()
                                binding.businessLocationSpinner.visible()
                                binding.businessSpinnerCont.visible()
                                showBusLocSpinner(it.data)
                                binding.rackAddBTN.visible()
                            }
                            else
                            {
                                binding.racksRV.adapter = null
                                binding.rackAddBTN.gone()
                                binding.businessLocationSpinner.gone()
                            }
                        }
                        Status.ERROR ->{
                            dialog.dismiss()
                            binding.swipeRefresh.isRefreshing = false
                            binding.rackCont.gone()
                            binding.connectionTimeout.visible()
                        }
                    }
                }
                else { }

            }
        })

        /**
         *      GET WAREHOUSE OBSERVER
         */

        viewModel.getWarehouse.observe(this, Observer{
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status)
                    {

                        Status.LOADING->{}

                        Status.SUCCESS ->
                        {
                            if (isNetworkConnected(this))
                            {
                                binding.swipeRefresh.isRefreshing = false
                                binding.warehouseSpinner.visible()
                                binding.warehouseSpinnerCont.visible()
                                binding.availableRacks.visible()
                                try
                                {
                                    LocalPreferences.put(this, isRefreshRequired, false)

                                    if(it.data?.get(0)?.status == true)
                                    {
                                        it.data[0].wHName?.let { it1 -> Log.i("warehouseResponse", it1) }
                                        showWarehouseSpinner(it.data)
                                        binding.rackAddBTN.visible()
                                        binding.availableRacks.visible()
                                        binding.warehouseSpinnerCont.visible()
                                    }
                                    else
                                    {
                                        binding.racksRV.adapter = null
                                        binding.availableRacks.gone()
                                        binding.rackAddBTN.gone()
                                        binding.warehouseSpinnerCont.gone()
                                        binding.printIV.click {
                                            toast("Nothing to print!")
                                        }
                                    }
                                }
                                catch(e:Exception)
                                {
                                    Log.i("rackAdapter","${e.message}")
                                    Log.i("rackAdapter","${e.stackTrace}")
                                }
                                //warehouseAdapter.addItems(list)

                            }
                            else
                            {
                                binding.rackAddBTN.isEnabled = false
                                binding.warehouseSpinnerCont.gone()

                            }

                        }
                        Status.ERROR ->
                        {
                            dialog.dismiss()
                            binding.swipeRefresh.isRefreshing = false
                            binding.rackCont.gone()
                            binding.connectionTimeout.visible()
                        }
                    }
                }

            }
        })

        /**
         *      GET RACK OBSERVER
         */

        viewModel.getRack.observe(this, Observer{
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status){
                        Status.LOADING ->
                        {

                        }
                        Status.SUCCESS ->{

                            binding.swipeRefresh.isRefreshing = false

                            if (isNetworkConnected(this))
                            {
                                try
                                {
                                    if(it.data?.get(0)?.status == true)
                                    {
                                        racksAdapter = RackAdapter(this,it.data as ArrayList<GetRackResponse>)

                                        bmpList.clear()
                                        textList.clear()
                                        binding.printIV.click { btn->

                                            if (isNetworkConnected(this))
                                            {
                                                for (i in it.data)
                                                {
                                                    generateQRCode("${i.rackCode}")
                                                    textList.add("${i.rackName}")
                                                    Log.i("rackList","${selectedBusLocNo}L-${whCode}-${i.rackCode}")
                                                    Log.i("rackList","${i.rackCode}")
                                                }
                                                generatePDF()
                                            }
                                            else
                                            {
                                                toast(NoInternetFound)
                                            }

                                        }
                                        binding.racksRV.apply {
                                            layoutManager = LinearLayoutManager(this@RacksActivity)
                                            adapter = racksAdapter
                                        }
                                        rackCode = it.data[0].rackCode.toString()
                                        capacity = it.data[0].capacity.toString()
                                        binding.availableRacks.visible()
                                        binding.warehouseSpinnerCont.visible()
                                    }

                                    else
                                    {
                                        binding.racksRV.adapter = null
                                        binding.printIV.click { btn ->
                                            toast("Nothing to print!")
                                        }
                                    }
                                }
                                catch (e: Exception)
                                {
                                    Log.i("RACK_OBSERVER","${e.message}")
                                    Log.i("RACK_OBSERVER","${e.stackTrace}")
                                }
                            }
                            else
                            {
                                binding.rackAddBTN.isEnabled = false
                            }
                        }

                        Status.ERROR ->{
                            dialog.dismiss()
                            binding.swipeRefresh.isRefreshing = false
                            binding.rackCont.gone()
                            binding.connectionTimeout.visible()
                        }
                    }
                }
                else  { }
            }
        })
    }

    private fun showBusLocSpinner(data:List<UserLocationResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val businessLocSpinner = binding.businessLocationSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].busLocationName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        businessLocSpinner.adapter = adapter

        businessLocSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {

                businessLocName = data[position].busLocationName.toString()
                selectedBusLocNo = data[position].orgBusLocNo.toString()
//                toast("onItem: $selectedBusLocNo")
                binding.racksRV.adapter = null
                //setting adapter to spinner
                binding.warehouseSpinner.adapter = returnNoAdapter()

                if (isNetworkConnected(this@RacksActivity))
                {
                    Log.i("LocBus","business Location no ${data[position].orgBusLocNo}")
                    // binding.rackSpinnerCont.visible()

                    viewModel.getWarehouse("", selectedBusLocNo)
                    binding.rackAddBTN.visible()
                    binding.rackAddBTN.isEnabled = true
                }
                else
                {
                    binding.racksRV.adapter = null
                    binding.rackAddBTN.gone()
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?)
            {
                toast("Please select any value")
                binding.rackAddBTN.isEnabled = false
            }
        }
    }

    private fun showWarehouseSpinner(data:List<GetWarehouseResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val warehouseSpinner = binding.warehouseSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].wHName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        warehouseSpinner.adapter = adapter
        warehouseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {
                selectedWareHouseNo = data[position].wHNo.toString()
                binding.racksRV.adapter = null

//                toast("warehouse value: ${warehouseSpinner.selectedItem}")
                if (warehouseSpinner.selectedItem != "No Record")
                {
                    if (isNetworkConnected(this@RacksActivity)) {

                        viewModel.getRack(
                            Utils.getSimpleTextBody(""),
                            Utils.getSimpleTextBody(selectedWareHouseNo),
                            Utils.getSimpleTextBody(selectedBusLocNo)
                        )
                        warehouseName = data[position].wHName.toString()
                        whCode = data[position].wHCode.toString()
                        binding.rackAddBTN.isEnabled = true
                        binding.printIV.isEnabled = true

                        Log.i("LocBus","This is warehouse name is ${adapter?.getItemAtPosition(position)}")
                        Log.i("LocBus","This is warehouse pos is ${data[position].wHNo}")
                    }
                    else
                    {
                        binding.racksRV.adapter = null
                        var selectedWH = data[position].wHNo.toString()
                        LocalPreferences.put(this@RacksActivity,isSpinnerSelected,"$selectedWH")
                        binding.rackAddBTN.gone()
                    }
                }
                else
                {
                    binding.rackAddBTN.isEnabled = false
                    binding.printIV.isEnabled = false
                }

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toast("Please select any value")
                binding.rackAddBTN.isEnabled = false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (LocalPreferences.getBoolean(this, isRefreshRequired))
        {
            viewModel.getRack(
                Utils.getSimpleTextBody(""),
                Utils.getSimpleTextBody(selectedWareHouseNo),
                Utils.getSimpleTextBody(selectedBusLocNo)
            )
        }
    }

    fun showQrCode(rackCode:String, rackName:String, rackNo:String){
        val intent = Intent(this, QrCodeDetailActivity::class.java)
        intent.putExtra("rackKey",true)
        intent.putExtra("rackQrCode",rackCode)
        intent.putExtra("rackQrName",rackName)
        intent.putExtra("rackQrNo",rackNo)
        startActivity(intent)
    }

    private fun clearPreferences(context: Context){
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        gotoActivity(ScannerActivity::class.java)
    }

    private fun generateQRCode(text:String) {
        val qrWriter = QRCodeWriter()
        try
        {
            val bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, 512,512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bmpList.add(bmp)

            for (x in 0 until width)
            {
                for(y in 0 until height)
                {
                    bmp.setPixel(x,y, if (bitMatrix[x,y]) Color.BLACK else Color.WHITE)
                }
            }
        }
        catch (e:Exception) { }
    }

    private fun generatePDF(){
        //handle button click
        //we need to handle runtime permission for devices with marshmallow and above
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            //system OS >= Marshmallow(6.0), check permission is enabled or not
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
                //permission was not granted, request it
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permissions, STORAGE_CODE)
            }
            else{
                //permission already granted, call savePdf() method
                savePdf()
            }
        }
        else{
            //system OS < marshmallow, call savePdf() method
            savePdf()
        }
    }

    private fun savePdf() {
        //create object of Document class

        //pdf file name
        val mFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        //pdf file path
        val mFilePath = Environment.getExternalStorageDirectory().toString() + "/" + "QrGeneratedFile" +".pdf"
        try {

            val file = File(mFilePath)
            val mDoc = Document()
            PdfWriter.getInstance(mDoc, FileOutputStream(mFilePath))
            mDoc.open()

            val pdfTable = PdfPTable(2)

            for (i in bmpList.indices)
            {
                val stream = ByteArrayOutputStream()
                bmpList[i].compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val myImg: Image = Image.getInstance(stream.toByteArray())
                myImg.scaleAbsolute(100f,100f)
                myImg.setAbsolutePosition(100f,100f)
                myImg.alignment = Element.ALIGN_CENTER

                val headingPara = Paragraph(Chunk("Racks"))
                headingPara.alignment = Element.ALIGN_CENTER

                val rackCode = Paragraph(Chunk(textList[i]))
                rackCode.alignment = Element.ALIGN_CENTER

                val pdfcell = PdfPCell()

                with(pdfcell)
                {
                    rowspan = 2
                    addElement(headingPara)
                    addElement(myImg)
                    addElement(rackCode)
                    paddingBottom = 10f
                }

                pdfTable.addCell(pdfcell)
            }
            pdfTable.completeRow()

            mDoc.add(pdfTable)

            mDoc.close()
            openPDF(file, "QrGeneratedFile.pdf\nis saved to\n$mFilePath")

            //show file saved message with file name and path
            Toast.makeText(this, "$mFileName.pdf\nis saved to\n$mFilePath", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception)
        {
            Log.i("pdfException","${e.message}")
            //if anything goes wrong causing exception, get and show exception message
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode)
        {
            STORAGE_CODE ->
            {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //permission from popup was granted, call savePdf() method
                    savePdf()

                }
                else
                {
                    //permission from popup was denied, show error message
                    Toast.makeText(this, "Permission denied...!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPDF(file: File, text: String)
    {
        val path = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName.toString() + ".provider",
            file
        )
        val pdfOpenIntent = Intent(Intent.ACTION_VIEW)
        pdfOpenIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        pdfOpenIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        pdfOpenIntent.setDataAndType(path, "application/pdf")
        try
        {
            startActivity(pdfOpenIntent)
        }
        catch (e: ActivityNotFoundException)
        {
            Log.i("openPDFException","${e.message}")
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed(){
        finish()
    }
}