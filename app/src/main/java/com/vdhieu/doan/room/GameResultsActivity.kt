package com.vdhieu.doan.room

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vdhieu.doan.LoginActivity.Companion.USER_KEY_SIGNUP
import com.vdhieu.doan.R
import com.vdhieu.doan.model.ResultsItem
import com.vdhieu.doan.room.MainActivity.Companion.playersList
import com.vdhieu.doan.room.RoomActivity.Companion.ROOM_CODE
import com.vdhieu.doan.room.RoomActivity.Companion.playerPlaying
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_game_results.*


class GameResultsActivity : AppCompatActivity() {
    private var _doubleBackToExitPressedOnce = false
    var user = User("", "", "")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_results)
        supportActionBar?.title = "LeaderBoard"

//        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);

        val right_anim = AnimationUtils.loadAnimation(this, R.anim.right_animation)
        val left_anim = AnimationUtils.loadAnimation(this, R.anim.left_animation)

        logo_results.animation = left_anim
        doodle_results.animation = right_anim

//        val roomCreatorUser: User? = intent.getParcelableExtra(USER_KEY_SIGNUP)
        val roomCode = intent.getStringExtra(ROOM_CODE).toString()

//        roomcode_textview.text = roomCode
        val db = Firebase.firestore
        val uid = FirebaseAuth.getInstance().uid

        user.username = playerPlaying!!.username
        user.uid = uid
        user.profileImageUrl=""
        val intent = Intent(this, RoomActivity::class.java)

        back_room.setOnClickListener {
            Log.d("Result","User for Room :${user}")
            intent.putExtra(USER_KEY_SIGNUP, user)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        val adapter = GroupAdapter<GroupieViewHolder>()
        game_results_recyclerview.adapter = adapter
        game_results_recyclerview.scheduleLayoutAnimation()

        var rank = 0
        for (player in playersList) {
            ++rank
            val item = ResultsItem(player, rank)
            adapter.add(item)
        }
        val roomRef = db.collection("rooms").document(roomCode)
        roomRef
            .delete()
    }

    override fun onPause() {
        super.onPause()
//        mediaPlayer.pause()
//        mediaPlayer.release()
    }

    override fun onResume() {
        super.onResume()
//        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);
//        mediaPlayer.start()
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
}
