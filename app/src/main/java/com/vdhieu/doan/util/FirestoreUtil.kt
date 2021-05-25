package com.vdhieu.doan.util

import android.content.Intent
import com.example.firebase.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreUtil {

    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val chatChannelsCollectionRef = firestoreInstance.collection("Rooms").document("123")


    private val currentUserDocRef: DocumentReference
        get() = firestoreInstance.document(
            "users/${
                FirebaseAuth.getInstance().uid ?: throw NullPointerException(
                    "UID is null."
                )
            }"
        )

    fun initCurrentUserIfFirstTime(onComplete: () -> Unit) {
        val uid = FirebaseAuth.getInstance().uid

        currentUserDocRef.get().addOnSuccessListener { documentSnapShot ->
            if (!documentSnapShot.exists()) {
                val newUser = User(
                    uid,FirebaseAuth.getInstance().currentUser?.displayName ?: "",""
                )
                currentUserDocRef.set(newUser).addOnSuccessListener {
                    onComplete()
                }
            } else
                onComplete()
        }
    }


}