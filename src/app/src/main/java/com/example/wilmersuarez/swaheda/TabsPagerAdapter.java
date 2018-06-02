package com.example.wilmersuarez.swaheda;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class TabsPagerAdapter extends FragmentPagerAdapter{
    TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        // Gets the Fragment item that is selected by the user
        switch (position) {
            case 0:
                return new ChatsFragment();
            case 1:
                return new FriendsListFragment();
            case 2:
                return new RequestsFragments();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        // Returns the number of Fragments (3: Friends List, Messages, Friend Requests
        return 3;
    }

    public CharSequence getPageTitle(int position) {
        // Gets the title of the Fragment selected by the user
        switch (position) {
            case 0:
                return "Messages";
            case 1:
                return "Friends List";
            case 2:
                return "Friend Requests";
            default:
                return null;
        }
    }
}
