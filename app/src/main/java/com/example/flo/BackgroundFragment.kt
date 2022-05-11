package com.example.flo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.flo.databinding.FragmentBackgroundBinding

class BackgroundFragment(private val imgRes : Int) : Fragment() {
    lateinit var binding : FragmentBackgroundBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBackgroundBinding.inflate(inflater,container,false)

        binding.homePannelBackground.setImageResource(imgRes)
        return binding.root
    }
}