package com.vdhieu.doan.play

import com.squareup.picasso.Picasso
import com.vdhieu.doan.R
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.leaderboard_player.view.*

class LeaderboardItem(val player: Player, val color: Int) :
    com.xwray.groupie.Item<GroupieViewHolder>() {

    override fun getLayout(): Int {
        return R.layout.leaderboard_player
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.username_textview.text = player.username
        viewHolder.itemView.points_textview.text = player.points.toString()
        viewHolder.itemView.leader_layout.setBackgroundColor(color)
//        Picasso.get().load(player.profileImageUrl).into(viewHolder.itemView.player_photo)
    }
}