package com.example.flo

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flo.databinding.ActivitySongBinding
import com.google.gson.Gson

class SongActivity : AppCompatActivity() {

    lateinit var binding: ActivitySongBinding
    lateinit var timer: Timer
    private var mediaPlayer : MediaPlayer? = null
    private var gson: Gson = Gson()

    private val songs = arrayListOf<Song>()
    lateinit var songDB: SongDatabase
    var nowPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPlayList()
        initSong()
        initClickListener()
    }

    private fun setPlayerStatus(isPlaying: Boolean) {
        songs[nowPos].isPlaying = isPlaying
        timer.isPlaying = isPlaying

        if (isPlaying) {
            binding.btnPlay.visibility = View.GONE
            binding.btnPause.visibility = View.VISIBLE
            mediaPlayer?.start()
        } else {
            binding.btnPlay.visibility = View.VISIBLE
            binding.btnPause.visibility = View.GONE
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
    }

    private fun initPlayList(){
        songDB = SongDatabase.getInstance(this)!!
        songs.addAll(songDB.songDao().getSongs())
    }

    private fun initClickListener() {
        binding.songDownIb.setOnClickListener{
            finish()
        }

        binding.btnPlay.setOnClickListener{
            setPlayerStatus(true)
        }
        binding.btnPause.setOnClickListener{
            setPlayerStatus(false)
        }
        binding.btnSkipNext.setOnClickListener{
            moveSong(+1)
        }
        binding.btnSkipPrevious.setOnClickListener{
            moveSong(-1)
        }
        binding.btnLikeOff.setOnClickListener{
            setLike(songs[nowPos].isLike)
        }
    }

    private fun initSong() { // 데이터를 받아옴
        val spf = getSharedPreferences("song", MODE_PRIVATE)
        val songId = spf.getInt("songId", 0)

        nowPos = getPlayingSongPosition(songId)

        Log.d("now Song ID", songs[nowPos].id.toString())
        startTimer()
        setPlayer(songs[nowPos])
    }

    private fun setLike(isLike: Boolean) {
        songs[nowPos].isLike = !isLike
        songDB.songDao().updateIsLikeById(!isLike, songs[nowPos].id)

        if (!isLike) {
            binding.btnLikeOff.setImageResource(R.drawable.ic_my_like_on)
        } else {
            binding.btnLikeOff.setImageResource(R.drawable.ic_my_like_off)
        }
    }

    private fun moveSong(direct: Int){
        if (nowPos + direct < 0) {
            Toast.makeText(this, "first song", Toast.LENGTH_SHORT).show()
            return
        }
        if (nowPos + direct >= songs.size){
            Toast.makeText(this, "last song", Toast.LENGTH_SHORT).show()
            return
        }
        nowPos += direct

        timer.interrupt()
        startTimer()

        mediaPlayer?.release()
        mediaPlayer = null

        setPlayer(songs[nowPos])
    }

    private fun getPlayingSongPosition(songId: Int) : Int{
        for (i in 0 until songs.size) {
            if (songs[i].id == songId) {
                return i
            }
        }
        return 0
    }

    private fun setPlayer(song: Song){ // 받아온 데이터를 가지고 view 작업
        binding.titleTv.text = song.title
        binding.singerTv.text = song.title
        binding.songStartTimeTv.text = String.format("%02d:%02d",song.second / 60, song.second % 60)
        binding.songEndTimeTv.text = String.format("%02d:%02d",song.playTime / 60, song.playTime % 60)
        binding.albumCover.setImageResource(song.coverImg!!)
        binding.seekBar.progress = (song.second * 1000 / song.playTime)

        val music = resources.getIdentifier(song.music, "raw", this.packageName)
        mediaPlayer = MediaPlayer.create(this, music)

        if (song.isLike) {
            binding.btnLikeOff.setImageResource(R.drawable.ic_my_like_on)
        } else {
            binding.btnLikeOff.setImageResource(R.drawable.ic_my_like_off)
        }
        setPlayerStatus(song.isPlaying)
    }

    private fun startTimer(){
        timer = Timer(songs[nowPos].playTime, songs[nowPos].isPlaying)
        timer.start()
    }
    inner class Timer(private val playTime: Int, var isPlaying: Boolean = true) : Thread(){

        private var second: Int = 0
        private var mills: Float = 0f

        override fun run() {
            super.run()
            try {
                while(true) {
                    if (second == playTime) {
                        break
                    }
                    if (isPlaying) {
                        sleep(50)
                        mills += 50

                        runOnUiThread{
                            binding.seekBar.progress = ((mills / playTime) * 100).toInt()
                        }
                        if (mills % 1000 == 0f) {
                            runOnUiThread {
                                binding.songStartTimeTv.text = String.format("%02d:%02d",second / 60, second % 60)
                            }
                            second++
                        }
                    }
                }
            } catch (e: InterruptedException){
                Log.d("Song","스레드 소멸, ${e.message}")
            }
        }
    }

    override fun onPause() { // 포커스를 잃었을때 음악 중지
        super.onPause()
        songs[nowPos].second = ((binding.seekBar.progress * songs[nowPos].playTime) / 100) / 1000
        songs[nowPos].isPlaying = false

        setPlayerStatus(false)

        val sharedPreferences = getSharedPreferences("song", MODE_PRIVATE)
        val editor = sharedPreferences.edit() // sp 에디터

        editor.putInt("songId", songs[nowPos].id)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.interrupt()
        mediaPlayer?.release() // mp가 갖고 있던 리소스 해제
        mediaPlayer = null // 미디어 해제
    }
}