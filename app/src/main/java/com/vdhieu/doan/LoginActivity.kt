package com.vdhieu.doan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase.model.User

import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vdhieu.doan.room.MainActivity
import com.vdhieu.doan.room.RoomActivity
import com.vdhieu.doan.room.StartGameActivity

import com.vdhieu.doan.util.FirestoreUtil
import kotlinx.android.synthetic.main.activity_login.*

import org.jetbrains.anko.clearTask
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask

class LoginActivity : AppCompatActivity() {

    companion object {

        val USER_KEY_SIGNUP = "USER_KEY_SIGNUP"

        var newUser: User? = null

    }

    private val TAG = "LoginActivity"
    private val RC_SIGN_IN = 1
    private val signInProviders = listOf(
        AuthUI.IdpConfig.EmailBuilder().setAllowNewAccounts(true)
            .setRequireName(true).build()
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.title = "Scribble It!"

        Account_sign_in.setOnClickListener {
            val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(signInProviders)
                .setLogo(R.drawable.ic_launcher_background)
                .build()

            startActivityForResult(intent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val intent = Intent(this, RoomActivity::class.java)
        val uid = FirebaseAuth.getInstance().uid

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                val progressDialog = indeterminateProgressDialog("Đang cài đặt tài khoản")
                FirestoreUtil.initCurrentUserIfFirstTime {
                    newUser = User(
                        uid,FirebaseAuth.getInstance().currentUser?.displayName ?: "",""
                    )
                    intent.putExtra(USER_KEY_SIGNUP, newUser)
                    //clear all other activities from stack by using the  below line
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    progressDialog.dismiss()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (response == null) return
                val constraint_layout: androidx.constraintlayout.widget.ConstraintLayout =
                    findViewById(R.id.constraint_layout)
                when (response.error?.errorCode) {
                    com.firebase.ui.auth.ErrorCodes.NO_NETWORK ->
                        org.jetbrains.anko.design.longSnackbar(constraint_layout, "No network")
                    com.firebase.ui.auth.ErrorCodes.UNKNOWN_ERROR ->
                        org.jetbrains.anko.design.longSnackbar(constraint_layout, "Unknown error")
                }
            }
        }

    }


}