package com.vdhieu.doan.model


import com.vdhieu.doan.R
import com.vdhieu.doan.play.Player
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.leaderboard_player.view.player_photo
import kotlinx.android.synthetic.main.leaderboard_player.view.points_textview
import kotlinx.android.synthetic.main.leaderboard_player.view.username_textview
import kotlinx.android.synthetic.main.result.view.*

class ResultsItem(val player: Player, val rank: Int) : com.xwray.groupie.Item<GroupieViewHolder>() {

    override fun getLayout(): Int {
        return R.layout.result
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.username_textview.text = player.username
        viewHolder.itemView.points_textview.text = player.points.toString()
        viewHolder.itemView.rank_textview.text = "#" + "$rank"
//        Picasso.get().load(player.profileImageUrl).into(viewHolder.itemView.player_photo)
    }
}