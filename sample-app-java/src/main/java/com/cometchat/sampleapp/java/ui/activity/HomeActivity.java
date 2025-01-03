package com.cometchat.sampleapp.java.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.sampleapp.java.R;
import com.cometchat.sampleapp.java.data.interfaces.OnItemClickListener;
import com.cometchat.sampleapp.java.databinding.ActivityHomeBinding;
import com.cometchat.sampleapp.java.ui.fragments.CallsFragment;
import com.cometchat.sampleapp.java.ui.fragments.ChatsFragment;
import com.cometchat.sampleapp.java.ui.fragments.GroupsFragment;
import com.cometchat.sampleapp.java.ui.fragments.UsersFragment;


public class HomeActivity extends AppCompatActivity implements OnItemClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String SELECTED_FRAGMENT_KEY = "selected_fragment";
    private ActivityHomeBinding binding;
    private int currentFragment = R.id.nav_chats; // Default to the Chats fragment

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_FRAGMENT_KEY, currentFragment); // Save the selected fragment ID
    }

    @Override
    public void onItemClick() {
        Intent intent = new Intent(this, NewChatActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Restore the current fragment if available, otherwise load the default
        // (ChatsFragment)
        if (savedInstanceState != null) {
            currentFragment = savedInstanceState.getInt(SELECTED_FRAGMENT_KEY, R.id.nav_chats);
        }

        // Set the selected item in the bottom navigation to match the current fragment
        binding.bottomNavigationView.setSelectedItemId(currentFragment);
        loadFragment(getFragment(currentFragment));
        configureBottomNavigation();
    }

    /**
     * Loads the specified fragment into the fragment container.
     *
     * @param fragment The fragment to be loaded.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    /**
     * Returns the appropriate fragment based on the selected menu itemId.
     *
     * @param itemId The selected menu itemId ID.
     * @return The corresponding fragment, or null if no match is found.
     */
    private static Fragment getFragment(int itemId) {
        Fragment selectedFragment;
        if (itemId == R.id.nav_chats) {
            selectedFragment = new ChatsFragment();
        } else if (itemId == R.id.nav_calls) {
            selectedFragment = new CallsFragment();
        } else if (itemId == R.id.nav_users) {
            selectedFragment = new UsersFragment();
        } else if (itemId == R.id.nav_groups) {
            selectedFragment = new GroupsFragment();
        } else {
            selectedFragment = new ChatsFragment();
        }
        return selectedFragment;
    }

    /**
     * Configures the bottom navigation view and its item selection listener.
     * Updates the displayed fragment based on user selection.
     */
    private void configureBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (currentFragment == item.getItemId()) {
                return true; // No action needed if the fragment is already selected
            }
            currentFragment = item.getItemId();
            loadFragment(getFragment(currentFragment));
            return true;
        });

        // Create a ColorStateList for icon and text color based on the checked state
        ColorStateList colorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{CometChatTheme.getIconTintHighlight(this), CometChatTheme.getIconTintSecondary(this)}
        );

        binding.bottomNavigationView.setItemIconTintList(colorStateList);
        binding.bottomNavigationView.setItemTextColor(colorStateList);
    }

}
