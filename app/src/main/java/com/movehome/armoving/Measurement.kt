package com.movehome.armoving

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import com.google.ar.sceneform.rendering.Color as arColor
import kotlin.math.pow
import kotlin.math.sqrt


class Measurement : AppCompatActivity(), Scene.OnUpdateListener {
    private val MIN_OPENGL_VERSION = 3.0
    private val TAG: String = Measurement::class.java.getSimpleName()

    private var arFragment: ArFragment? = null

    private var distanceModeTextView: TextView? = null
    private lateinit var pointTextView: TextView

    private lateinit var arrow1UpLinearLayout: LinearLayout
    private lateinit var arrow1DownLinearLayout: LinearLayout
    private lateinit var arrow1UpView: ImageView
    private lateinit var arrow1DownView: ImageView
    private lateinit var arrow1UpRenderable: Renderable
    private lateinit var arrow1DownRenderable: Renderable

    private lateinit var arrow10UpLinearLayout: LinearLayout
    private lateinit var arrow10DownLinearLayout: LinearLayout
    private lateinit var arrow10UpView: ImageView
    private lateinit var arrow10DownView: ImageView
    private lateinit var arrow10UpRenderable: Renderable
    private lateinit var arrow10DownRenderable: Renderable

    private lateinit var multipleDistanceTableLayout: TableLayout

    private var cubeRenderable: ModelRenderable? = null
    private var distanceCardViewRenderable: ViewRenderable? = null

    private lateinit var distanceModeSpinner: Spinner
    private val distanceModeArrayList = ArrayList<String>()
    private var distanceMode: String = ""

    private val placedAnchors = ArrayList<Anchor>()
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val midAnchors: MutableMap<String, Anchor> = mutableMapOf()
    private val midAnchorNodes: MutableMap<String, AnchorNode> = mutableMapOf()
    private val fromGroundNodes = ArrayList<List<Node>>()

    private val multipleDistances = Array(Constants.maxNumMultiplePoints,
        {Array<TextView?>(Constants.maxNumMultiplePoints){null} })
    private lateinit var initCM: String

    private lateinit var clearButton: Button

    var imageView: ImageView? = null


    var data: Bundle? = null
    var items: ArrayList<CardListData>? = null
    var position: Int? = null
    var item: CardListData? = null
    var myDir: String? = null
    var imageFloderPath: String? = null
    var filepath: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        data = intent.extras
        items = data?.getParcelableArrayList<CardListData>("items")!!
        position = data?.getInt("position")
        item = items!![position!!]
        Log.d(TAG, "item("+position+").data("+ item!!.data.size+")")

        myDir = this.externalCacheDir?.path.toString()
        imageFloderPath = "/image/"+ item!!.title
        createDir()

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_measurement)
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment?


        val button = findViewById<Button>(R.id.doneButton)
        button.setOnClickListener {
            takePhoto()

            item!!.data.add(CardData(filepath, "new card name", "new card"))
            Log.d(TAG, "item.data("+ item!!.data.size+"): "+item!!.data[item!!.data.size-1])

            backToMain()
        }

        imageView = findViewById<ImageView>(R.id.imageView_test)
        imageView!!.setOnClickListener(View.OnClickListener { takePhoto() })


        val distanceModeArray = resources.getStringArray(R.array.distance_mode)
        distanceModeArray.map{it-> distanceModeArrayList.add(it) }
        distanceModeTextView = findViewById(R.id.distance_view)
        multipleDistanceTableLayout = findViewById(R.id.multiple_distance_table)

        initCM = resources.getString(R.string.initCM)

        configureSpinner()
        initArrowView()
        initRenderable()
        clearButton()


        arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (cubeRenderable == null || distanceCardViewRenderable == null) return@setOnTapArPlaneListener
            // Creating Anchor.
            when (distanceMode){
                distanceModeArrayList[0] -> {
                    clearAllAnchors()
                    placeAnchor(hitResult, distanceCardViewRenderable!!)
                }
                distanceModeArrayList[1] -> {
                    tapDistanceOf2Points(hitResult)
                }
                distanceModeArrayList[2] -> {
                    tapDistanceOfMultiplePoints(hitResult)
                }
                distanceModeArrayList[3] -> {
                    tapDistanceFromGround(hitResult)
                }
                else -> {
                    clearAllAnchors()
                    placeAnchor(hitResult, distanceCardViewRenderable!!)
                }
            }
        }
    }

    private fun initDistanceTable(){
        Log.d(TAG, "initDistanceTable")
        for (i in 0 until Constants.maxNumMultiplePoints+1){
            val tableRow = TableRow(this)
            multipleDistanceTableLayout.addView(tableRow,
                multipleDistanceTableLayout.width,
                Constants.multipleDistanceTableHeight / (Constants.maxNumMultiplePoints + 1))
            for (j in 0 until Constants.maxNumMultiplePoints+1){
                val textView = TextView(this)
                textView.setTextColor(Color.WHITE)
                if (i==0){
                    if (j==0){
                        textView.setText("cm")
                    }
                    else{
                        textView.setText((j-1).toString())
                    }
                }
                else{
                    if (j==0){
                        textView.setText((i-1).toString())
                    }
                    else if(i==j){
                        textView.setText("-")
                        multipleDistances[i-1][j-1] = textView
                    }
                    else{
                        textView.setText(initCM)
                        multipleDistances[i-1][j-1] = textView
                    }
                }
                tableRow.addView(textView,
                    tableRow.layoutParams.width / (Constants.maxNumMultiplePoints + 1),
                    tableRow.layoutParams.height)
            }
        }
    }

    private fun initArrowView(){
        Log.d(TAG, "initArrowView")
        arrow1UpLinearLayout = LinearLayout(this)
        arrow1UpLinearLayout.orientation = LinearLayout.VERTICAL
        arrow1UpLinearLayout.gravity = Gravity.CENTER
        arrow1UpView = ImageView(this)
        arrow1UpView.setImageResource(R.drawable.arrow_1up)
        arrow1UpLinearLayout.addView(arrow1UpView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)

        arrow1DownLinearLayout = LinearLayout(this)
        arrow1DownLinearLayout.orientation = LinearLayout.VERTICAL
        arrow1DownLinearLayout.gravity = Gravity.CENTER
        arrow1DownView = ImageView(this)
        arrow1DownView.setImageResource(R.drawable.arrow_1down)
        arrow1DownLinearLayout.addView(arrow1DownView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)

        arrow10UpLinearLayout = LinearLayout(this)
        arrow10UpLinearLayout.orientation = LinearLayout.VERTICAL
        arrow10UpLinearLayout.gravity = Gravity.CENTER
        arrow10UpView = ImageView(this)
        arrow10UpView.setImageResource(R.drawable.arrow_10up)
        arrow10UpLinearLayout.addView(arrow10UpView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)

        arrow10DownLinearLayout = LinearLayout(this)
        arrow10DownLinearLayout.orientation = LinearLayout.VERTICAL
        arrow10DownLinearLayout.gravity = Gravity.CENTER
        arrow10DownView = ImageView(this)
        arrow10DownView.setImageResource(R.drawable.arrow_10down)
        arrow10DownLinearLayout.addView(arrow10DownView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)
    }

    private fun initRenderable() {
        Log.d(TAG, "initRenderable")
        MaterialFactory.makeTransparentWithColor(
            this,
            arColor(Color.RED))
            .thenAccept { material: Material? ->
                cubeRenderable = ShapeFactory.makeSphere(
                    0.02f,
                    Vector3.zero(),
                    material)
                cubeRenderable!!.setShadowCaster(false)
                cubeRenderable!!.setShadowReceiver(false)
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("1. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, R.layout.distance_text_layout)
            .build()
            .thenAccept{
                distanceCardViewRenderable = it
                distanceCardViewRenderable!!.isShadowCaster = false
                distanceCardViewRenderable!!.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("2. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow1UpLinearLayout)
            .build()
            .thenAccept{
                arrow1UpRenderable = it
                arrow1UpRenderable.isShadowCaster = false
                arrow1UpRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("3. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow1DownLinearLayout)
            .build()
            .thenAccept{
                arrow1DownRenderable = it
                arrow1DownRenderable.isShadowCaster = false
                arrow1DownRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("4. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow10UpLinearLayout)
            .build()
            .thenAccept{
                arrow10UpRenderable = it
                arrow10UpRenderable.isShadowCaster = false
                arrow10UpRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("5. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow10DownLinearLayout)
            .build()
            .thenAccept{
                arrow10DownRenderable = it
                arrow10DownRenderable.isShadowCaster = false
                arrow10DownRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("6. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun configureSpinner(){
        Log.d(TAG, "configureSpinner")
        distanceMode = distanceModeArrayList[0]
        distanceModeSpinner = findViewById(R.id.distance_mode_spinner)
        val distanceModeAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            distanceModeArrayList
        )
        distanceModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distanceModeSpinner.adapter = distanceModeAdapter
        distanceModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                distanceMode = spinnerParent.selectedItem as String
                clearAllAnchors()
                setMode()
                toastMode()
                if (distanceMode == distanceModeArrayList[2]){
                    val layoutParams = multipleDistanceTableLayout.layoutParams
                    layoutParams.height = Constants.multipleDistanceTableHeight
                    multipleDistanceTableLayout.layoutParams = layoutParams
                    initDistanceTable()
                }
                else{
                    val layoutParams = multipleDistanceTableLayout.layoutParams
                    layoutParams.height = 0
                    multipleDistanceTableLayout.layoutParams = layoutParams
                }
                Log.i(TAG, "Selected arcore focus on ${distanceMode}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                clearAllAnchors()
                setMode()
                toastMode()
            }
        }
    }

    private fun setMode(){
        Log.d(TAG, "setMode")
        distanceModeTextView!!.text = distanceMode
    }

    private fun clearButton(){
        Log.d(TAG, "clearButton")
        clearButton = findViewById(R.id.clearButton)
        clearButton.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                clearAllAnchors()
            }
        })
    }

    private fun clearAllAnchors(){
        Log.d(TAG, "clearAllAnchors")
        placedAnchors.clear()
        for (anchorNode in placedAnchorNodes){
            arFragment!!.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor!!.detach()
            anchorNode.setParent(null)
        }
        placedAnchorNodes.clear()
        midAnchors.clear()
        for ((k,anchorNode) in midAnchorNodes){
            arFragment!!.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor!!.detach()
            anchorNode.setParent(null)
        }
        midAnchorNodes.clear()
        for (i in 0 until Constants.maxNumMultiplePoints){
            for (j in 0 until Constants.maxNumMultiplePoints){
                if (multipleDistances[i][j] != null){
                    multipleDistances[i][j]!!.setText(if(i==j) "-" else initCM)
                }
            }
        }
        fromGroundNodes.clear()
    }

    private fun tapDistanceFromGround(hitResult: HitResult){
        Log.d(TAG, "tapDistanceFromGround")
        clearAllAnchors()
        val anchor = hitResult.createAnchor()
        placedAnchors.add(anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val transformableNode = TransformableNode(arFragment!!.transformationSystem)
            .apply{
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true //If you want to move to the better position.
                this.renderable = renderable
                setParent(anchorNode)
            }

        val node = Node()
            .apply {
                setParent(transformableNode)
                this.worldPosition = Vector3(
                    anchorNode.worldPosition.x,
                    anchorNode.worldPosition.y,
                    anchorNode.worldPosition.z)
                this.renderable = distanceCardViewRenderable
            }

        val arrow1UpNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y+0.1f,
                    node.worldPosition.z
                )
                this.renderable = arrow1UpRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y+0.01f,
                        node.worldPosition.z
                    )
                }
            }

        val arrow1DownNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y-0.08f,
                    node.worldPosition.z
                )
                this.renderable = arrow1DownRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y-0.01f,
                        node.worldPosition.z
                    )
                }
            }

        val arrow10UpNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y+0.18f,
                    node.worldPosition.z
                )
                this.renderable = arrow10UpRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y+0.1f,
                        node.worldPosition.z
                    )
                }
            }

        val arrow10DownNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y-0.167f,
                    node.worldPosition.z
                )
                this.renderable = arrow10DownRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y-0.1f,
                        node.worldPosition.z
                    )
                }
            }

        fromGroundNodes.add(listOf(node, arrow1UpNode, arrow1DownNode, arrow10UpNode, arrow10DownNode))

        arFragment!!.arSceneView.scene.addOnUpdateListener(this)
        arFragment!!.arSceneView.scene.addChild(anchorNode)
        transformableNode.select()
    }

    private fun placeAnchor(hitResult: HitResult,
                            renderable: Renderable){
        Log.d(TAG, "placeAnchor")
        val anchor = hitResult.createAnchor()
        placedAnchors.add(anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val node = TransformableNode(arFragment!!.transformationSystem)
            .apply{
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true
                this.renderable = renderable
                setParent(anchorNode)
            }

        arFragment!!.arSceneView.scene.addOnUpdateListener(this)
        arFragment!!.arSceneView.scene.addChild(anchorNode)
        node.select()
    }


    private fun tapDistanceOf2Points(hitResult: HitResult){
        Log.d(TAG, "tapDistanceOf2Points")
        if (placedAnchorNodes.size == 0){
            placeAnchor(hitResult, cubeRenderable!!)
            //If you have 0 anchor, place one.
        }
        else if (placedAnchorNodes.size == 1){
            placeAnchor(hitResult, cubeRenderable!!)
            //If you already have 1, place another one with distance pop in the middle.

            val midPosition = floatArrayOf(
                (placedAnchorNodes[0].worldPosition.x + placedAnchorNodes[1].worldPosition.x) / 2,
                (placedAnchorNodes[0].worldPosition.y + placedAnchorNodes[1].worldPosition.y) / 2,
                (placedAnchorNodes[0].worldPosition.z + placedAnchorNodes[1].worldPosition.z) / 2)
            val quaternion = floatArrayOf(0.0f,0.0f,0.0f,0.0f)
            val pose = Pose(midPosition, quaternion)

            placeMidAnchor(pose, distanceCardViewRenderable!!)
        }
        else {
            clearAllAnchors()
            placeAnchor(hitResult, cubeRenderable!!)
        }
    }

    private fun placeMidAnchor(pose: Pose,
                               renderable: Renderable,
                               between: Array<Int> = arrayOf(0,1)){
        Log.d(TAG, "placeMidAnchor")
        val midKey = "${between[0]}_${between[1]}"
        val anchor = arFragment!!.arSceneView.session!!.createAnchor(pose)
        midAnchors.put(midKey, anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        midAnchorNodes.put(midKey, anchorNode)

        val node = TransformableNode(arFragment!!.transformationSystem)
            .apply{
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true
                this.renderable = renderable
                setParent(anchorNode)
            }
        arFragment!!.arSceneView.scene.addOnUpdateListener(this)
        arFragment!!.arSceneView.scene.addChild(anchorNode)
    }

    private fun tapDistanceOfMultiplePoints(hitResult: HitResult){
        Log.d(TAG, "tapDistanceOfMultiplePoints")
        if (placedAnchorNodes.size >= Constants.maxNumMultiplePoints){
            clearAllAnchors()
        }
        ViewRenderable
            .builder()
            .setView(this, R.layout.point_text_layout)
            .build()
            .thenAccept{
                it.isShadowReceiver = false
                it.isShadowCaster = false
                pointTextView = it.getView() as TextView
                pointTextView.setText(placedAnchors.size.toString())
                placeAnchor(hitResult, it)
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("7. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
        Log.i(TAG, "Number of anchors: ${placedAnchorNodes.size}")
    }

    @SuppressLint("SetTextI18n")
    override fun onUpdate(frameTime: FrameTime) {
        Log.d(TAG, "onUpdate")
        when(distanceMode) {
            distanceModeArrayList[0] -> {
                measureDistanceFromCamera()
            }
            distanceModeArrayList[1] -> {
                measureDistanceOf2Points()
            }
            distanceModeArrayList[2] -> {
                measureMultipleDistances()
            }
            distanceModeArrayList[3] -> {
                measureDistanceFromGround()
            }
            else -> {
                measureDistanceFromCamera()
            }
        }
    }

    private fun measureDistanceFromGround(){
        Log.d(TAG, "measureDistanceFromGround")
        if (fromGroundNodes.size == 0) return
        for (node in fromGroundNodes){
            val textView = (distanceCardViewRenderable!!.view as LinearLayout)
                .findViewById<TextView>(R.id.distanceCard)
            val distanceCM = changeUnit(node[0].worldPosition.y + 1.0f, "cm")
            textView.text = "%.0f".format(distanceCM) + " cm"
        }
    }

    private fun measureDistanceFromCamera(){
        Log.d(TAG, "measureDistanceFromCamera")
        val frame = arFragment!!.arSceneView.arFrame
        if (placedAnchorNodes.size >= 1) {
            val distanceMeter = calculateDistance(
                placedAnchorNodes[0].worldPosition,
                frame!!.camera.pose)
            measureDistanceOf2Points(distanceMeter)
        }
    }

    private fun measureDistanceOf2Points(){
        Log.d(TAG, "measureDistanceOf2Points")
        if (placedAnchorNodes.size == 2) {
            val distanceMeter = calculateDistance(
                placedAnchorNodes[0].worldPosition,
                placedAnchorNodes[1].worldPosition)
            measureDistanceOf2Points(distanceMeter)
        }
    }

    private fun measureDistanceOf2Points(distanceMeter: Float){
        Log.d(TAG, "measureDistanceOf2Points(distanceMeter)")
        val distanceTextCM = makeDistanceTextWithCM(distanceMeter)
        val textView = (distanceCardViewRenderable!!.view as LinearLayout)
            .findViewById<TextView>(R.id.distanceCard)
        textView.text = distanceTextCM
        Log.d(TAG, "distance: ${distanceTextCM}")
    }

    private fun measureMultipleDistances(){
        Log.d(TAG, "measureMultipleDistances")
        if (placedAnchorNodes.size > 1){
            for (i in 0 until placedAnchorNodes.size){
                for (j in i+1 until placedAnchorNodes.size){
                    val distanceMeter = calculateDistance(
                        placedAnchorNodes[i].worldPosition,
                        placedAnchorNodes[j].worldPosition)
                    val distanceCM = changeUnit(distanceMeter, "cm")
                    val distanceCMFloor = "%.2f".format(distanceCM)
                    multipleDistances[i][j]!!.setText(distanceCMFloor)
                    multipleDistances[j][i]!!.setText(distanceCMFloor)
                }
            }
        }
    }

    private fun makeDistanceTextWithCM(distanceMeter: Float): String{
        Log.d(TAG, "makeDistanceTextWithCM")
        val distanceCM = changeUnit(distanceMeter, "cm")
        val distanceCMFloor = "%.2f".format(distanceCM)
        return "${distanceCMFloor} cm"
    }

    private fun calculateDistance(x: Float, y: Float, z: Float): Float{
        Log.d(TAG, "calculateDistance")
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    private fun calculateDistance(objectPose0: Pose, objectPose1: Pose): Float{
        Log.d(TAG, "calculateDistance(objectPose: Pose)")
        return calculateDistance(
            objectPose0.tx() - objectPose1.tx(),
            objectPose0.ty() - objectPose1.ty(),
            objectPose0.tz() - objectPose1.tz())
    }


    private fun calculateDistance(objectPose0: Vector3, objectPose1: Pose): Float{
        Log.d(TAG, "calculateDistance(objectPose: Vector3, Pose)")
        return calculateDistance(
            objectPose0.x - objectPose1.tx(),
            objectPose0.y - objectPose1.ty(),
            objectPose0.z - objectPose1.tz()
        )
    }

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Vector3): Float{
        Log.d(TAG, "calculateDistance(objectPose: Vector3)")
        return calculateDistance(
            objectPose0.x - objectPose1.x,
            objectPose0.y - objectPose1.y,
            objectPose0.z - objectPose1.z
        )
    }

    private fun changeUnit(distanceMeter: Float, unit: String): Float{
        Log.d(TAG, "changeUnit")
        return when(unit){
            "cm" -> distanceMeter * 100
            "mm" -> distanceMeter * 1000
            else -> distanceMeter
        }
    }

    private fun toastMode(){
        Log.d(TAG, "toastMode")
        Toast.makeText(this@Measurement,
            when(distanceMode){
                distanceModeArrayList[0] -> "Find plane and tap somewhere"
                distanceModeArrayList[1] -> "Find plane and tap 2 points"
                distanceModeArrayList[2] -> "Find plane and tap multiple points"
                distanceModeArrayList[3] -> "Find plane and tap point"
                else -> "???"
            },
            Toast.LENGTH_LONG)
            .show()
    }


    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        Log.d(TAG, "checkIsSupportedDeviceOrFinish")
        val openGlVersionString =
            (Objects.requireNonNull(activity
                .getSystemService(Context.ACTIVITY_SERVICE)) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES ${MIN_OPENGL_VERSION} later")
            Toast.makeText(activity,
                "Sceneform requires OpenGL ES ${MIN_OPENGL_VERSION} or later",
                Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun takePhoto() {
        filepath = generateFilename()
        val view = arFragment!!.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread: HandlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request(view, bitmap, { copyResult: Int ->
                runOnUiThread{ imageView!!.setImageBitmap(bitmap) }
                saveBitmapToDisk(bitmap, filepath!!)
                handlerThread.quitSafely()
            }, Handler(handlerThread.looper))
        }
    }

    private fun createDir() {
        val imageFloder = File(myDir+imageFloderPath)
        if(!imageFloder.isDirectory) imageFloder.mkdirs()
    }

    private fun generateFilename(): String {
        val filename = "/"+item!!.title+"_"+item!!.data.size+".jpg"
        return myDir+imageFloderPath+filename
    }

    private fun saveBitmapToDisk(bitmap: Bitmap, filepath: String) {
        val file = File(filepath)
        try {
            Log.d(TAG, "file path: "+ filepath)

            if(file.isFile) file.delete()

            val bStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bStream)
            val byteArray = bStream.toByteArray()
            file.writeBytes(byteArray)
        } catch (e: Exception) {
            Log.e(TAG, "create new file", e)
        }
    }

    private fun backToMain() {
        Log.d(TAG, "backToMain")
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("items", items)
        intent.putExtra("position", position)
        startActivity(intent)
    }
}