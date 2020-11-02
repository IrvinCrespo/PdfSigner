package com.matrix.lumston.pdfsigner

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class PdfPageAdapter(lifecycle : Lifecycle,fm : FragmentManager, val fragments : List<Fragment>)
    : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int {
        return fragments.count()
    }

    override fun createFragment(position: Int): Fragment {
        return fragments.get(position)
    }

}