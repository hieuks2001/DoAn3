package com.vdhieu.doan.room

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.model.User
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vdhieu.doan.DialogStroke
import com.vdhieu.doan.R
import com.vdhieu.doan.model.GuessText
import com.vdhieu.doan.play.DrawSegment
import com.vdhieu.doan.play.LeaderboardItem
import com.vdhieu.doan.play.PaintView
import com.vdhieu.doan.play.Player
import com.vdhieu.doan.room.RoomActivity.Companion.playerPlaying

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.custom_toast.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import java.io.*
import java.util.*
import kotlin.math.min


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var _doubleBackToExitPressedOnce = false
    private var currentTurnInGame = 0

    var otherPlayerTimer: CountDownTimer? = null
    lateinit var timerForChoosingWord: CountDownTimer
    private var startTimer = false
    lateinit var currentDrawerTimer: CountDownTimer
    var timeRemaining = 0

    var nearlyEqual = false
    var players = mutableSetOf<Player>()


    var maxPlayersNum = 0
    var roundNo = 1

    var roomCode =""
    private val db = Firebase.firestore
    private val myUid = FirebaseAuth.getInstance().uid
    private val name = FirebaseAuth.getInstance().currentUser?.displayName
    private val fbdb = FirebaseDatabase.getInstance()
    var currentPlayer: Player? = null
    var count = 0
    var chosenWordByDrawer = ""
    var text = ""
    var currentDrawerUsername = ""
    var finishTimer = false
    var timerFinished = false
    var flagOfWritingFinishTimer = false
    var timerRunning = true
    private var rankListener: ListenerRegistration? = null

    val transparent = Color.TRANSPARENT
    private val leaderboardAdapter = GroupAdapter<GroupieViewHolder>()

    lateinit var fade_in: Animation
    lateinit var fade_out: Animation
    lateinit var top_anim: Animation
    lateinit var bottom_anim: Animation
    lateinit var ltr: Animation
    lateinit var rtl: Animation
    lateinit var rtl_remove: Animation
    val MAX_POINTS = 400


    private val currentUser: User? = null
    private val guessAdapter = GroupAdapter<GroupieViewHolder>()

    var colorArray = listOf<Int>(
        Color.parseColor("#fe548b"),
        Color.parseColor("#feaa46"),
        Color.parseColor("#43bcfe"),
        Color.parseColor("#3fdec3"),
        Color.parseColor("#a97cf4"),
        Color.parseColor("#ffeb3b")
    )

    companion object {
        val TAG = "Game Activity"
        var myPaintView: PaintView? = null
        val playersList = mutableListOf<Player>()

        //StrokeWidth
        var seekProgress = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

//        startGame()
        fade_in = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow)
        fade_out = AnimationUtils.loadAnimation(this, R.anim.fade_out_slow)
        top_anim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottom_anim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        ltr = AnimationUtils.loadAnimation(this, R.anim.left_animation)
        rtl = AnimationUtils.loadAnimation(this, R.anim.right_animation)
        rtl_remove = AnimationUtils.loadAnimation(this, R.anim.right_to_left_invisible)

        drawing_board.animation = top_anim
        clock_imageview.animation = ltr
        leaderboard_recyclerview.animation = bottom_anim
        chat_log_recyclerview.animation = bottom_anim

        roomCode = intent.getStringExtra(StartJoinActivity.JOIN_USER_KEY) ?: return
//        roomCode = "19406890"


        leaderboard_recyclerview.adapter = leaderboardAdapter
        chat_log_recyclerview.adapter = guessAdapter
        val linearLayoutManager = LinearLayoutManager(this)
        chat_log_recyclerview.layoutManager = linearLayoutManager

        currentPlayer = playerPlaying

        myPaintView = drawing_board
        myPaintView?.roomCodePaintView = roomCode
        myPaintView?.ref =
            fbdb.getReference("games/${roomCode}/drawing")
        myPaintView?.addDatabaseListeners()

        val task = myPaintView?.TouchEvent()
        task?.execute("abhay")

        myPaintView?.setOnTouchListener(task)
        myPaintView?.invalidate()


        chat_log_recyclerview.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (bottom < oldBottom) {
                    if (guessAdapter.itemCount - 1 > 0)
                        linearLayoutManager.smoothScrollToPosition(
                            chat_log_recyclerview,
                            null,
                            guessAdapter.itemCount - 1
                        )
                }
            }
        })
//        btn_sign_out.setOnClickListener {
//            AuthUI.getInstance().signOut(this@MainActivity).addOnCompleteListener {
//                startActivity(intentFor<LoginActivity>().newTask().clearTask())
//            }
//        }

        detectCurrentDrawerUsername()
        listenChosenWord()
        findMaxPlayers()
        obtainCurrentTurn()
        detectTimerStart()
        listenGuesses()
        alreadyGuessListener()
        msgListener()
        checkRank()
        roundListener()
        leaderBoardListener()
        listenCorrectGuessNum()

        color_pallette.setOnClickListener(this)
        eraser.setOnClickListener(this)
        clear_button.setOnClickListener(this)
        stroke_imageButton.setOnClickListener(this)
        pencil.setOnClickListener(this)

        guess_button.setOnClickListener {
            if (guess_editText.text.toString() != "") {
                performGuess()
            } else {
                return@setOnClickListener
            }



        }
    }
    private fun checkRank() {
        val timeStampRef = db.collection("rooms").document(roomCode)
            .collection("correctGuessTimeStamp")

            rankListener =
            timeStampRef
                .whereEqualTo("uid", myUid)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    for (doc in value!!) {
                        val uid = doc.getString("uid").toString()
                        val username = doc.getString("username").toString()
                        val timeLeft = (doc.get("timeLeft") as Long).toInt()
                        ++count
                        val reward =
                            MAX_POINTS - (count - 1) * (200 / maxPlayersNum) - (90 - timeLeft) * 2

                        val drawerReward = (reward / maxPlayersNum)

                        if (myUid == uid) {
                            awardPoints(uid, reward, username, drawerReward)
                            writeDrawerReward(drawerReward)

                            val ref = db.collection("rooms").document(roomCode)
                                .collection("correctGuess")

                            val correctMap = hashMapOf("correctGuess" to true)
                            ref
                                .add(correctMap)
                        }

                    }
                }

    }
    private fun writeDrawerReward(drawerReward: Int) {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("drawerReward")

        val hashmap = hashMapOf("drawReward" to drawerReward)

        drawerRef
            .add(hashmap)
    }
    private fun findMaxPlayers() {
        val maxPlayersRef = db.collection("rooms").document(roomCode)

        maxPlayersRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    maxPlayersNum = (document.get("maxPlayers") as Long).toInt()
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }
    private fun listenCorrectGuessNum() {
        val ref = db.collection("rooms").document(roomCode)
            .collection("correctGuess")

        ref
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(TAG, "New city: ${dc.document.data}")
                            ++count
                            if (count == maxPlayersNum - 1) {
                                if (currentPlayer?.currentDrawer!!) {
                                    currentDrawerTimer.cancel()
                                    currentDrawerTimer.onFinish()
                                } else {
                                    otherPlayerTimer?.cancel()
                                    otherPlayerTimer?.onFinish()
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> Log.d(
                            TAG,
                            "Modified city: ${dc.document.data}"
                        )
                        DocumentChange.Type.REMOVED -> Log.d(
                            TAG,
                            "Removed city: ${dc.document.data}"
                        )
                    }
                }
            }
    }

    private fun leaderBoardListener() {
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers")
        var player: Player
        playerRef
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(TAG, "New city leadboard: ${dc.document.data}")
                            val username = dc.document.getString("username").toString()
                            player = dc.document.toObject(Player::class.java)
                            playersList.removeAll { it.username == username }
                            playersList.add(player)
                            Log.d(TAG, "New city1: ${playersList}")

                            updateRecyclerView()
                        }
                        DocumentChange.Type.MODIFIED -> {
                            Log.d(TAG, "Modified city: ${dc.document.data}")
                            val username = dc.document.getString("username").toString()
                            player = dc.document.toObject(Player::class.java)
                            playersList.removeAll { it.username == username }
                            playersList.add(player)
                            updateRecyclerView()

                        }
                        DocumentChange.Type.REMOVED -> Log.d(
                            TAG,
                            "Removed city: ${dc.document.data}"
                        )
                    }
                }

            }
    }

    private fun updateRecyclerView() {
        leaderboard_recyclerview.scheduleLayoutAnimation()
        var colorCount = -1
        playersList.sort()
        if (leaderboardAdapter.itemCount > 0) {
            leaderboardAdapter.clear()
        }
        for (player in playersList) {
            ++colorCount
            val bColor = colorArray[colorCount]
            val item = LeaderboardItem(player, bColor)
            leaderboardAdapter.add(item)
        }

    }

    private fun roundListener() {
        val roundRef = db.collection("rooms").document(roomCode)
            .collection("game").document("round")
        roundRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    roundNo = (snapshot.get("currentRound") as Long).toInt()
                    if (roundNo <= 3)
                        round_textview.text = "Round $roundNo of 3"
                    if (roundNo == 4) {
                        round_textview.text = ""
                        Handler().postDelayed({
                            val intent = Intent(this, GameResultsActivity::class.java)
                            intent.putExtra("ROOM_CODE", roomCode)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra("ROOM_CODE", roomCode)
                            startActivity(intent)
//                        this.overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
                        }, 2000)
                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun msgListener() {
        val msgRef = db.collection("rooms").document(roomCode)
            .collection("message")
        msgRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (dc in snapshot.documentChanges) {
                        Log.d(TAG, "Current data: $snapshot")
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                val username = dc.document.getString("username").toString()
                                val award = (dc.document.get("reward") as Long).toInt()
                                val layout: View = layoutInflater.inflate(
                                    R.layout.custom_toast,
                                    custom_toast_layout
                                )
                                val tv =
                                    layout.findViewById(R.id.points_awarded_textview) as TextView
//
                                Log.d("Toast", "Executing")
                                if (award > 0) {
                                    tv.text = "$username  + $award points"
                                    val toast = Toast(applicationContext)
                                    toast.duration = Toast.LENGTH_SHORT
                                    toast.view = layout
                                    toast.setGravity(Gravity.TOP, 0, 130);
                                    toast.show()
                                } else if (award < 0) {
                                    tv.text = "$username  - ${-award} points"
                                    val toast = Toast(applicationContext)
                                    toast.duration = Toast.LENGTH_SHORT
                                    toast.view = layout
                                    toast.setGravity(Gravity.TOP, 0, 130);
                                    toast.show()
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d(TAG, "Modified city: ${dc.document.data}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d(TAG, "Removed city: ${dc.document.data}")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun alreadyGuessListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasGuessRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)

        hasGuessRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    currentPlayer?.hasAlreadyGuessed =
                        snapshot.getBoolean("hasAlreadyGuessed") as Boolean
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }

    }

    private fun listenGuesses() {
        val guessRef = db.collection("rooms").document(roomCode)
            .collection("guesses").orderBy("timeStamp")

        var guessListened: String

        guessRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (dc in snapshot.documentChanges) {
                        Log.d(TAG, "Current data: $snapshot")
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                Log.d(TAG, "New guess: ${dc.document.data}")
                                guessListened = dc.document.getString("guess").toString()
                                val username = dc.document.getString("username").toString()
                                val profileImageUrl =
                                    dc.document.getString("profileImageUrl").toString()
                                var timeStamp = dc.document.get("timeStamp") as Long?
                                val hasAlreadyGuessed = dc.document.getBoolean("hasAlreadyGuessed")
                                val hasGuess = hasAlreadyGuessed ?: return@addSnapshotListener
                                val uid =
                                    dc.document.getString("playerUid") ?: return@addSnapshotListener
                                val timeLeft = (dc.document.get("timeRemaining") as Long).toInt()
                                checkGuess(
                                    guessListened,
                                    username,
                                    profileImageUrl,
                                    hasGuess,
                                    uid,
                                    timeLeft,
                                    timeStamp
                                )
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d(TAG, "Modified city: ${dc.document.data}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d(TAG, "Removed city: ${dc.document.data}")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun checkNearlyEqual(l: Int, checkGuess: String, correctWord: String) {
        var count = 0
        for (i in 0 until l) {
            if (checkGuess[i] == correctWord[i]) {
                count++
            }
        }
        nearlyEqual = l - count == 0 || l - count == 1 || l - count == 2
    }

    private fun checkGuess(
        guessWord: String, username: String, profileImageUrl: String, hasAlreadyGuessed: Boolean,
        playerUid: String, timeRemain: Int, timestamp: Long?
    ) {

        nearlyEqual = false
        val checkGuess = guessWord.toLowerCase()
        val correctWord = chosenWordByDrawer.toLowerCase()
        val l1 = checkGuess.length
        val l2 = correctWord.length
        val l: Int
        if (l1 != l2) {
            l = min(l1, l2)
        } else {
            l = l1
        }
        checkNearlyEqual(l, checkGuess, correctWord)

        val guessMessage = GuessText("")
        guessMessage.username = username

        val random = Random()
        val index = random.nextInt(21) + 1
//        guessMessage.colorPlayer = colorArrayPlayers[index]

        if (!timerFinished) {
            if (checkGuess == correctWord) {
                if (username == currentDrawerUsername) {
//                    guessMessage.textColor = Color.parseColor("#")
//                    guessMessage.guessText = "$username is penalized with -100 points!"
                    guessMessage.guessText = guessWord
                    guessAdapter.add(guessMessage)
                    nearlyEqual = false
                    vibrateExecute().execute("abhay")
//                    vibrate()
//                    soundPool.play(penaltySound!!, 1.0F, 1.0F, 0, 0, 1.0F);

                    Log.d("GuessingCorrect", "username == currentDrawerUsername getting executed")

                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                    val drawerReward = 0

                    if (currentPlayer?.username == username) {
                        awardPoints(playerUid, -100, username, drawerReward)
                        Log.d(
                            "GuessingCorrect",
                            "currentPlayer?.username == username getting executed"
                        )
                    }

                } else {
                    if (!hasAlreadyGuessed) {
//                        guessMessage.guessText = "$username guessed the correct word!"
                        guessMessage.textColor = Color.parseColor("#000000")
                        guessMessage.guessText = guessWord

                        guessAdapter.add(guessMessage)
                        vibrateExecute().execute("garg")
//                        vibrate()
                        Log.d("GuessingCorrect", "!hasAlreadyGuessed getting executed")
//                        soundPool.play(correctGuessSound!!, 1.0F, 1.0F, 0, 0, 1.0F);

                        chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                        if (currentPlayer?.username == username) {
                            val stampMap =
                                hashMapOf(
                                    "uid" to playerUid,
                                    "username" to username,
                                    "timeLeft" to timeRemain,
                                    "timeStamp" to timestamp
                                )

                            val timeStampRef = db.collection("rooms").document(roomCode)
                                .collection("correctGuessTimeStamp").document(playerUid)
                            timeStampRef
                                .set(stampMap)

                            val playerRef = db.collection("rooms").document(roomCode)
                                .collection("leaderBoardPlayers").document(playerUid)
                            playerRef
                                .update("hasAlreadyGuessed", true)
                        }
                    } else {
//                        guessMessage.guessText = "$username already guessed the correct word!"
                        guessMessage.guessText = guessWord
                        guessAdapter.add(guessMessage)
                        chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                        Log.d("GuessingCorrect", "!hasAlreadyGuessed  else getting executed")
                    }
                }
            } else if (nearlyEqual && !hasAlreadyGuessed) {
                if (username == currentDrawerUsername) {
                    guessMessage.guessText = guessWord
                    guessAdapter.add(guessMessage)
                    Log.d("GuessingCorrect", "nearlu eequal if getting executed")
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                } else {
//                    guessMessage.guessText = "$username's guess is nearly correct!"
                    guessMessage.guessText = guessWord
                    guessAdapter.add(guessMessage)
                    guessMessage.textColor = Color.parseColor("#000000")
                    nearlyEqual = false
                    Log.d("GuessingCorrect", "nearly equal  else getting executed")
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                }
            } else {
                guessMessage.guessText = guessWord
                guessAdapter.add(guessMessage)
                chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                Log.d("GuessingCorrect", "correctword  else getting executed")
            }
        } else {
            guessMessage.guessText = guessWord
            guessAdapter.add(guessMessage)
            chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
            Log.d("GuessingCorrect", "timerfinished else  else getting executed")

        }


    }

    private fun awardPoints(uid: String, reward: Int, username: String, drawerReward: Int) {
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)


        playerRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    val prevPoints = (document.get("points") as Long).toInt()
                    finalAwardPoints(prevPoints, reward, uid, username)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

    }

    private fun finalAwardPoints(prevPoints: Int, reward: Int, uid: String, username: String) {
        val newPoints = prevPoints + reward
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        playerRef
            .update("points", newPoints)


        val msgMap =
            hashMapOf("username" to username, "reward" to reward)
        val msgRef = db.collection("rooms").document(roomCode)
            .collection("message")

        msgRef
            .add(msgMap)
    }

    inner class vibrateExecute : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String?): String {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                v.vibrate(200)
            }
            return "vibrated"
        }

    }

    private fun detectTimerStart() {
        val timerRef = db.collection("rooms").document(roomCode)
            .collection("game").document("timer")

        timerRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Timer start : ${snapshot.data}")
                    startTimer = snapshot.get("startTimer") as Boolean

                    if (startTimer && !currentPlayer?.currentDrawer!!) {
                        count = 0
                        startTimer()
                        timerFinished = false
                        someone_choosing_a_word_textview.visibility = View.INVISIBLE
                    }

                } else {
                    Log.d(TAG, "Current data in turn: null")
                }
            }
    }

    private fun startTimer() {

        otherPlayerTimer =
            object : CountDownTimer(90000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    show_correct_word_textview.visibility = View.GONE
                    timerFinished = false
                    timer_textview.text = (millisUntilFinished / 1000).toString()
                    timeRemaining = (millisUntilFinished / 1000).toInt()

                    val l = chosenWordByDrawer.length
                    val charAt0 = chosenWordByDrawer[0]
                    val charAtlby2 = chosenWordByDrawer[l / 2]
                    if (timeRemaining == 60 && !currentPlayer?.currentDrawer!!) {
                        text = charAt0 + text.substring(1)
                        chosen_word_textview.text = text
                    }
                    if (timeRemaining == 30 && !currentPlayer?.currentDrawer!!) {
                        text = text.substring(0, (l / 2) * 2) + charAtlby2 +
                                text.substring((l / 2) * 2 + 1)
                        chosen_word_textview.text = text
                    }

                }

                override fun onFinish() {
                    myPaintView?.clearDrawingForAll()
                    if (!currentPlayer?.currentDrawer!!) {
                        show_correct_word_textview.animation = bottom_anim
                        show_correct_word_textview.visibility = View.VISIBLE
                        show_correct_word_textview.text =
                            "The correct word was \" $chosenWordByDrawer \"!!"
                    }
                    timer_textview.text = ""
                    timerFinished = true
                    chosen_word_textview.text = ""
                    val guessMessage = GuessText("")
                    guessMessage.username = currentDrawerUsername
                    guessMessage.profileImageUrl = currentPlayer?.profileImageUrl.toString()
                    guessMessage.guessText = "The correct word was \" $chosenWordByDrawer \"!!"
                    guessMessage.textColor = Color.parseColor("#0097a7")
                    guessAdapter.add(guessMessage)
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                }
            }
        otherPlayerTimer?.start()
    }

    override fun onBackPressed() {
        Log.i("Back pressed", "onBackPressed--")
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this._doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press again to quit", Toast.LENGTH_SHORT).show()
        Handler().postDelayed({ _doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun performGuess() {

        val guessRef = db.collection("rooms").document(roomCode)
            .collection("guesses")

        val guessMsg = guess_editText.text.toString()

        val guessMap =
            hashMapOf(
                "guess" to guessMsg,
                "timeStamp" to System.currentTimeMillis(),
                "playerUid" to myUid,
                "hasAlreadyGuessed" to currentPlayer?.hasAlreadyGuessed,
                "username" to name,
                "profileImageUrl" to currentPlayer?.profileImageUrl,
                "timeRemaining" to timeRemaining
            )

        guessRef
            .add(guessMap)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
        guess_editText.text?.clear()

    }


    override fun onClick(v: View?) {
        if (currentPlayer?.currentDrawer!!) {
            when (v?.id) {
                R.id.first_word_textview -> {
                    eraser.visibility = View.VISIBLE
                    pencil.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    stroke_imageButton.visibility = View.VISIBLE
                    first_word_textview.animation = fade_out
                    second_word_textview.animation = fade_out
                    third_word_textview.animation = fade_out
                    eraser.animation = ltr
                    color_pallette.animation = ltr
                    clear_button.animation = ltr
                    stroke_imageButton.animation = ltr
                    pencil.animation = ltr
                    choose_a_word_textview.animation = top_anim
                    activateBoard()
                    timerForChoosingWord.cancel()
                    uploadChosenWord(first_word_textview.text.toString())
                    first_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    third_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.GONE
//                    first_word_textview.visibility = View.INVISIBLE
//                    second_word_textview.visibility = View.INVISIBLE
//                    third_word_textview.visibility = View.INVISIBLE
//                    imageView_clear_button.setBackgroundColor(transparent)

                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)
                    count = 0
                    startTimerCurrentDrawer()
                    clear_button.performClick()

                }
                R.id.second_word_textview -> {
                    activateBoard()
                    first_word_textview.animation = fade_out
                    second_word_textview.animation = fade_out
                    third_word_textview.animation = fade_out
                    eraser.animation = ltr
                    color_pallette.animation = ltr
                    clear_button.animation = ltr
                    stroke_imageButton.animation = ltr
                    pencil.animation = ltr
//                    imageView_clear_button.setBackgroundColor(transparent)

                    timerForChoosingWord.cancel()
                    uploadChosenWord(second_word_textview.text.toString())
                    first_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    third_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.GONE
//                    first_word_textview.visibility = View.INVISIBLE
//                    second_word_textview.visibility = View.INVISIBLE
//                    third_word_textview.visibility = View.INVISIBLE
                    eraser.visibility = View.VISIBLE
                    pencil.visibility = View.VISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    stroke_imageButton.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()
                    clear_button.performClick()
                    count = 0
                }
                R.id.third_word_textview -> {
                    activateBoard()
                    first_word_textview.animation = fade_out
                    second_word_textview.animation = fade_out
                    third_word_textview.animation = fade_out
                    eraser.animation = ltr
                    color_pallette.animation = ltr
                    clear_button.animation = ltr
                    stroke_imageButton.animation = ltr
                    pencil.animation = ltr
//                    imageView_clear_button.setBackgroundColor(transparent)

                    timerForChoosingWord.cancel()
                    uploadChosenWord(third_word_textview.text.toString())
                    third_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    first_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.GONE
//                    first_word_textview.visibility = View.INVISIBLE
//                    second_word_textview.visibility = View.INVISIBLE
//                    third_word_textview.visibility = View.INVISIBLE
                    pencil.visibility = View.VISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    stroke_imageButton.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()
                    clear_button.performClick()

                    count = 0
                }

                R.id.eraser -> {
                    myPaintView?.eraser()
                    imageView_eraser_button.background = resources.getDrawable(R.drawable.highlight)
                    imageView_clear_button.setBackgroundColor(transparent)
                    imageView_pencil_button.setBackgroundColor(transparent)
                    imageView_stroke_width.setBackgroundColor(transparent)
                    imageView_color_pallette.setBackgroundColor(transparent)

                }
                R.id.clear_button -> {
                    myPaintView?.clearDrawingForAll()
                    imageView_clear_button.background = resources.getDrawable(R.drawable.highlight)
                    imageView_eraser_button.setBackgroundColor(transparent)
                    imageView_pencil_button.setBackgroundColor(transparent)
                    imageView_stroke_width.setBackgroundColor(transparent)
                    imageView_color_pallette.setBackgroundColor(transparent)
                }
                R.id.color_pallette -> {
                    colorPaletteDisplay()
                    imageView_color_pallette.background =
                        resources.getDrawable(R.drawable.highlight)
                    imageView_clear_button.setBackgroundColor(transparent)
                    imageView_pencil_button.setBackgroundColor(transparent)
                    imageView_stroke_width.setBackgroundColor(transparent)
                    imageView_eraser_button.setBackgroundColor(transparent)
                }
                R.id.stroke_imageButton -> {
                    imageView_stroke_width.background = resources.getDrawable(R.drawable.highlight)
                    imageView_clear_button.setBackgroundColor(transparent)
                    imageView_pencil_button.setBackgroundColor(transparent)
                    imageView_eraser_button.setBackgroundColor(transparent)
                    imageView_color_pallette.setBackgroundColor(transparent)
                    val cdd = DialogStroke(this)
                    cdd.show()
                }
                R.id.pencil -> {
                    imageView_pencil_button.background = resources.getDrawable(R.drawable.highlight)
                    imageView_clear_button.setBackgroundColor(transparent)
                    imageView_eraser_button.setBackgroundColor(transparent)
                    imageView_stroke_width.setBackgroundColor(transparent)
                    imageView_color_pallette.setBackgroundColor(transparent)
                    myPaintView?.paint?.color = Color.BLACK
                    myPaintView?.paint?.strokeWidth = 10f
                }
            }

        }
    }

    @SuppressLint("ResourceType")
    private fun colorPaletteDisplay() {
        val mColor: Int = myPaintView?.paint?.color ?: Color.GREEN


        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Chọn màu cho bút")
            .initialColor(mColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .setOnColorSelectedListener { selectedColor ->
                Toast.makeText(this, "selected color: $selectedColor", Toast.LENGTH_SHORT)
            }
            .setPositiveButton(
                "Chọn"
            ) { dialog, selectedColor, allColors -> changeColor(selectedColor) }
            .setNegativeButton(
                "Huỷ"
            ) { dialog, which -> }
            .build()
            .show()
    }

    private fun changeColor(selectedColor: Int) {
        myPaintView?.changeStrokeColor(selectedColor)
    }

    private fun startTimerCurrentDrawer() {
        currentDrawerTimer =
            object : CountDownTimer(90000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timer_textview.text = (millisUntilFinished / 1000).toString()
                    timeRemaining = millisUntilFinished.toInt()
                    timerFinished = false
                }

                override fun onFinish() {
                    if (myUid != null) {
                        drawerRewardListener(currentDrawerUsername, myUid)
                    }
                    timer_textview.text = ""
                    currentPlayer?.currentDrawer = false
                    myPaintView?.clearDrawingForAll()
                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")
                    val turnRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("turn")

                    currentTurnInGame += 1
                    if (currentTurnInGame > maxPlayersNum) {
                        currentTurnInGame = 1
                        roundNo += 1
                        updateRound()
                    }
                    timerRef
                        .update("startTimer", false)
                        .addOnSuccessListener {
                            Log.d(TAG, "timer updated: false")
                        }
                    turnRef
                        .update("currentTurn", currentTurnInGame)
                        .addOnSuccessListener {
                            Log.d(TAG, "turn updated: $currentTurnInGame")
                        }
                    chosen_word_textview.text = ""
                    val guessMessage = GuessText("")

                    guessMessage.username = currentDrawerUsername
                    guessMessage.profileImageUrl = currentPlayer?.profileImageUrl.toString()
                    guessMessage.guessText = "The correct word was \" $chosenWordByDrawer \"!!"
                    guessMessage.textColor = Color.parseColor("#0097a7")
                    guessAdapter.add(guessMessage)
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
//                    writeCorrectWord()
                }
            }
        currentDrawerTimer.start()

    }

    private fun updateRound() {
        val roundRef = db.collection("rooms").document(roomCode)
            .collection("game").document("round")
        roundRef
            .update("currentRound", roundNo)
    }

    private fun drawerRewardListener(username: String, uid: String) {
        val ref = db.collection("rooms").document(roomCode)
            .collection("drawerReward")

        var points = 0

        ref
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    points += (document.get("drawReward") as Long).toInt()
                }
                readDrawerPoints(points, username, uid)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

        ref
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")

                    val docRef = db.collection("rooms").document(roomCode)
                        .collection("drawerReward").document(document.id)
                    docRef
                        .delete()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

    }

    private fun readDrawerPoints(drawerReward: Int, username: String, uid: String) {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)

        drawerRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    val prevPoints = (document.get("points") as Long).toInt()
                    finalAwardPointsToDrawer(prevPoints, drawerReward, uid, username)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun finalAwardPointsToDrawer(
        prevPoints: Int,
        drawerReward: Int,
        uid: String,
        username: String
    ) {
        val newPoints = prevPoints + drawerReward
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        playerRef
            .update("points", newPoints)

        val msgMap =
            hashMapOf("username" to username, "reward" to drawerReward)
        val msgRef = db.collection("rooms").document(roomCode)
            .collection("message")

        msgRef
            .add(msgMap)
    }

    private fun uploadChosenWord(chosenWord: String) {
        val wordRef = db.collection("rooms").document(roomCode)
            .collection("game").document("correctWord")

        val chosenWordMap = hashMapOf("chosenWord" to chosenWord)

        wordRef
            .set(chosenWordMap)
    }

    private fun detectCurrentDrawerUsername() {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("game").document("currentDrawer")

        drawerRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current drawer data: ${snapshot.data}")
                    currentDrawerUsername = snapshot.getString("currentDrawerName").toString()
                    someone_choosing_a_word_textview.text =
                        "$currentDrawerUsername is choosing a word!!"

                    if (!currentPlayer?.currentDrawer!!) {
                        someone_choosing_a_word_textview.visibility = View.VISIBLE
                    } else
                        someone_choosing_a_word_textview.visibility = View.INVISIBLE

                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun listenChosenWord() {
        val wordRef = db.collection("rooms").document(roomCode)
            .collection("game").document("correctWord")

        wordRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    chosenWordByDrawer = snapshot.getString("chosenWord").toString()
                    if (currentPlayer?.currentDrawer!!) {
                        chosen_word_textview.text = chosenWordByDrawer
                    } else {
                        text = ""
                        val l = chosenWordByDrawer.length
                        for (i in 0..l - 1) {
                            if( chosenWordByDrawer[i] == ' '){
                                text += "  "
                            }else if (chosenWordByDrawer[i] == ','){
                                text += ", "
                            }else{
                                text += "_ "
                            }
                        }
                        chosen_word_textview.text = text
                    }
                    Log.d(TAG, "Current word: $chosenWordByDrawer")

                } else {
                    Log.d(TAG, "Current data in word : null")
                }
            }
    }

    private fun obtainCurrentTurn() {
        val turnRef = db.collection("rooms").document(roomCode)
            .collection("game").document("turn")

        turnRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data in turn to initiate view: ${snapshot.data}")
                    currentTurnInGame = (snapshot.get("currentTurn") as Long).toInt()
                    Log.d(TAG, "Current data in turn to initiate view: ${currentTurnInGame}")

                    currentPlayer?.hasAlreadyGuessed = false
                    writeHasAlreadyGuessed()
                    checkCurrentDrawer()
                    if (otherPlayerTimer != null) {
                        otherPlayerTimer?.cancel()
                        otherPlayerTimer?.onFinish()
                    }
                } else {
                    Log.d(TAG, "Current data in turn: null")
                }
            }
    }

    private fun  writeHasAlreadyGuessed() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasGuessRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        hasGuessRef
            .update("hasAlreadyGuessed", false)
    }


    private fun checkCurrentDrawer() {
        currentPlayer?.currentDrawer = currentTurnInGame == currentPlayer?.indexOfTurn
        Log.d("MainActivity", "Check  Drawer : ${currentPlayer?.currentDrawer}")
        if (currentPlayer?.currentDrawer!!) {
            currentPlayer?.username?.let { updateDrawerName(it) }
            count = 0
            finishTimer = false
            timerFinished = false
            flagOfWritingFinishTimer = false
//            show_correct_word_textview.visibility = View.GONE
            resetCorrectGuessNum()
            showViewToPlayer()
        } else {
            showViewToPlayer()
        }
    }

    private fun updateDrawerName(currentDrawerName: String) {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("game").document("currentDrawer")

        val drawerMap = hashMapOf("currentDrawerName" to currentDrawerName)

        drawerRef
            .set(drawerMap)
    }

    private fun resetCorrectGuessNum() {
        val ref = db.collection("rooms").document(roomCode)
            .collection("correctGuess")

        ref
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    val docRef = db.collection("rooms").document(roomCode)
                        .collection("correctGuess").document(document.id)
                    docRef
                        .delete()

                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    private fun showViewToPlayer() {
        count = 0
        if (currentPlayer?.currentDrawer!!) {
            setWords()
            wordChoosingTimer()
            first_word_textview.animation = fade_in
            second_word_textview.animation = fade_in
            third_word_textview.animation = fade_in
            choose_a_word_textview.animation = fade_in
            choose_a_word_textview.visibility = View.VISIBLE
            first_word_textview.visibility = View.VISIBLE
            second_word_textview.visibility = View.VISIBLE
            third_word_textview.visibility = View.VISIBLE
            first_word_textview.setOnClickListener(this)
            second_word_textview.setOnClickListener(this)
            third_word_textview.setOnClickListener(this)
            first_word_textview.isEnabled = true
            second_word_textview.isEnabled = true
            third_word_textview.isEnabled = true
            show_correct_word_textview.visibility = View.GONE

        } else {
            imageView_pencil_button.setBackgroundColor(transparent)
            imageView_clear_button.setBackgroundColor(transparent)
            imageView_eraser_button.setBackgroundColor(transparent)
            imageView_stroke_width.setBackgroundColor(transparent)
            imageView_color_pallette.setBackgroundColor(transparent)
            eraser.animation = rtl_remove
            color_pallette.animation = rtl_remove
            clear_button.animation = rtl_remove
            stroke_imageButton.animation = rtl_remove
            pencil.animation = rtl_remove
            pencil.visibility = View.GONE
            eraser.visibility = View.GONE
            color_pallette.visibility = View.GONE
            clear_button.visibility = View.GONE
            stroke_imageButton.visibility = View.GONE
            choose_a_word_textview.visibility = View.GONE
            first_word_textview.visibility = View.GONE
            second_word_textview.visibility = View.GONE
            third_word_textview.visibility = View.GONE

            show_correct_word_textview.visibility = View.GONE

            myPaintView?.allowDraw = false
        }
    }

    private fun setWords() {
        val random = Random()
//        val first = random.nextInt(6800) + 1
//        val second = random.nextInt(6800) + 1
//        val third = random.nextInt(6800) + 1
        val first = random.nextInt(70)
        val second = random.nextInt(70)
        val third = random.nextInt(70)
        first_word_textview.text = WordsArray.words[first]
        second_word_textview.text = WordsArray.words[second]
        third_word_textview.text = WordsArray.words[third]
    }

    private fun wordChoosingTimer() {
        timerForChoosingWord = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerRunning = true
            }

            override fun onFinish() {
                timerRunning = false
                autoSelectWord()
            }
        }
        timerForChoosingWord.start()
    }

    private fun autoSelectWord() {
        val random = Random()
        when (random.nextInt(2)) {
            0 -> {
                first_word_textview.performClick()
            }
            1 -> {
                second_word_textview.performClick()
            }
            else -> {
                third_word_textview.performClick()
            }
        }
    }

    private fun activateBoard() {
        val segment = DrawSegment()
        val scale = 1.0f
        segment.addColor(Color.BLACK)

//        segment.addPoint(243, 0)
//        segment.addPoint(252, 0)
        segment.addPoint(260, 0)

        segment.addStrokeWidth(10f)
        myPaintView?.invalidate()
        myPaintView?.canvas!!.drawPath(
            PaintView.getPathForPoints(segment.points, scale),
            myPaintView?.paint ?: return
        )
        val drawId = UUID.randomUUID().toString().substring(0, 15)

        val db = FirebaseDatabase.getInstance()

        val keyRef = db.getReference("games/${roomCode}/drawing/$drawId")
        Log.d("MainActivity", "Saving segment to firebase")
        keyRef
            .setValue(segment)
        keyRef.child("$drawId").removeValue()

    }


}

