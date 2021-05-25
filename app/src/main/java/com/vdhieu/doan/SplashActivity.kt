package com.vdhieu.doan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.vdhieu.doan.room.MainActivity
import com.vdhieu.doan.room.StartGameActivity
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import org.jetbrains.anko.startActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().uid == null)
            startActivity<LoginActivity>()
        else {
            AuthUI.getInstance().signOut(this@SplashActivity).addOnCompleteListener {
                startActivity(intentFor<LoginActivity>().newTask().clearTask())
            }
//            startActivity<MainActivity>()

        }
        finish()
    }
}