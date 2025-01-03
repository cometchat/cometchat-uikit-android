package com.cometchat.chatuikit.extensions.sticker.keyboard.adapter;

import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class StickerTabAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = StickerTabAdapter.class.getSimpleName();


    private final List<Fragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentIconList = new ArrayList<>();

    public StickerTabAdapter(FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    public void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
    }

    public void addFragment(Fragment fragment, String title, String icon) {
        mFragmentList.add(fragment);
        mFragmentIconList.add(icon);
    }

    public String getPageIcon(int position) {
        return mFragmentIconList.get(position);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return new SpannableStringBuilder("");
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }
}
