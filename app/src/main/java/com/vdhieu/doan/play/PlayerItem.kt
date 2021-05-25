
package com.vdhieu.doan.play


import com.example.firebase.model.User
import com.squareup.picasso.Picasso
import com.vdhieu.doan.R
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.player.view.*

class PlayerItem(val player: User) : com.xwray.groupie.Item<GroupieViewHolder>() {

  override fun getLayout(): Int {
    return R.layout.player
  }

  override fun bind(viewHolder: GroupieViewHolder, position: Int) {

    viewHolder.itemView.player_username.text = player.username

  }
}