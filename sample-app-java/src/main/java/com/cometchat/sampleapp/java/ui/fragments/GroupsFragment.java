package com.cometchat.sampleapp.java.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.chat.models.Group;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.resources.utils.AnimationUtils;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.resources.utils.itemclicklistener.OnItemClickListener;
import com.cometchat.sampleapp.java.R;
import com.cometchat.sampleapp.java.databinding.CreateGroupLayoutBinding;
import com.cometchat.sampleapp.java.databinding.FragmentGroupsBinding;
import com.cometchat.sampleapp.java.databinding.JoinPasswordGroupLayoutBinding;
import com.cometchat.sampleapp.java.databinding.OverflowMenuLayoutBinding;
import com.cometchat.sampleapp.java.ui.activity.MessagesActivity;
import com.cometchat.sampleapp.java.viewmodels.GroupsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.gson.Gson;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A fragment that displays a list of groups and allows users to join or create
 * new groups.
 */
public class GroupsFragment extends Fragment {
    private FragmentGroupsBinding binding;
    private GroupsViewModel viewModel;

    private BottomSheetDialog bottomSheetDialog;
    private TextView tvError;
    private TextView tvButtonText;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGroupsBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider.NewInstanceFactory().create(GroupsViewModel.class);
        bottomSheetDialog = new BottomSheetDialog(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Observe the ViewModel for various states and update UI accordingly
        viewModel.getJoinedGroup().observe(getViewLifecycleOwner(), this::openGroupChat);
        viewModel.getCreatedGroup().observe(getViewLifecycleOwner(), this::openGroupChat);
        viewModel.getError().observe(getViewLifecycleOwner(), this::setErrorMessage);
        viewModel.getDialogState().observe(getViewLifecycleOwner(), this::setDialogState);

        binding.group.setItemClickListener(new OnItemClickListener<Group>() {
            @Override
            public void OnItemClick(Group group, int position) {
                // Handle group item click based on group join status
                if (group.isJoined()) {
                    openGroupChat(group);
                } else {
                    if (group.getGroupType().equalsIgnoreCase(UIKitConstants.GroupType.PUBLIC)) {
                        viewModel.joinPasswordGroup(group, "");
                    } else if (group.getGroupType().equalsIgnoreCase(UIKitConstants.GroupType.PASSWORD)) {
                        openJoinPasswordGroupDialog(group);
                    }
                }
            }
        });
        setOverFlowMenu();
    }

    /**
     * Opens the chat interface for the specified group.
     *
     * @param group The group to open the chat for.
     */
    private void openGroupChat(Group group) {
        if (group.isJoined()) {
            if (bottomSheetDialog.isShowing()) bottomSheetDialog.dismiss();
            Intent intent = new Intent(requireActivity(), MessagesActivity.class);
            intent.putExtra(getString(R.string.app_group), new Gson().toJson(group));
            startActivity(intent);
        }
    }

    /**
     * Sets the error message displayed in the UI.
     *
     * @param message The error message to display.
     */
    private void setErrorMessage(String message) {
        if (tvError != null) tvError.setText(message);
    }

    /**
     * Updates the dialog state based on the provided status.
     *
     * @param status The dialog state to update.
     */
    private void setDialogState(UIKitConstants.DialogState status) {
        if (tvError == null || tvButtonText == null || progressBar == null) return;
        switch (status) {
            case INITIATED:
                tvError.setVisibility(View.GONE);
                tvButtonText.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case SUCCESS:
                if (bottomSheetDialog.isShowing()) {
                    bottomSheetDialog.dismiss();
                }
                break;
            case FAILURE:
                tvError.setVisibility(View.VISIBLE);
                tvButtonText.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Opens a dialog for joining a password-protected group.
     *
     * @param group The group that requires a password to join.
     */
    private void openJoinPasswordGroupDialog(Group group) {
        JoinPasswordGroupLayoutBinding joinPasswordGroupLayoutBinding = JoinPasswordGroupLayoutBinding.inflate(getLayoutInflater());
        tvError = joinPasswordGroupLayoutBinding.tvError;
        tvButtonText = joinPasswordGroupLayoutBinding.joinGroupBtnText;
        progressBar = joinPasswordGroupLayoutBinding.joinGroupProgress;
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel().toBuilder()
                                                                              .setTopLeftCorner(CornerFamily.ROUNDED,
                                                                                                getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_radius_4)
                                                                              )
                                                                              .setTopRightCorner(CornerFamily.ROUNDED,
                                                                                                 getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_radius_4)
                                                                              )
                                                                              .setBottomLeftCorner(
                                                                                      CornerFamily.ROUNDED,
                                                                                      0
                                                                              )
                                                                              .setBottomRightCorner(CornerFamily.ROUNDED, 0)
                                                                              .build();
        joinPasswordGroupLayoutBinding.joinGroupCard.setShapeAppearanceModel(shapeAppearanceModel);
        joinPasswordGroupLayoutBinding.groupAvatar.setAvatar(group.getName(), group.getIcon());
        joinPasswordGroupLayoutBinding.tvGroupName.setText(group.getName());
        joinPasswordGroupLayoutBinding.tvMemberCount.setText(group.getMembersCount() > 1 ? group.getMembersCount() + " " + getResources().getString(
                com.cometchat.chatuikit.R.string.cometchat_members) : group.getMembersCount() + " " + getResources().getString(com.cometchat.chatuikit.R.string.cometchat_member));
        joinPasswordGroupLayoutBinding.joinButton.setOnClickListener(view -> viewModel.joinPasswordGroup(group,
                                                                                                         joinPasswordGroupLayoutBinding.etPassword.getText()
                                                                                                                                                  .toString()
                                                                                                                                                  .trim()
        ));
        Utils.showBottomSheet(getContext(), bottomSheetDialog, true, false, joinPasswordGroupLayoutBinding.getRoot());
        bottomSheetDialog.show();
    }

    /**
     * Sets up the overflow menu for creating a new group.
     */
    private void setOverFlowMenu() {
        OverflowMenuLayoutBinding overflowMenuLayoutBinding = OverflowMenuLayoutBinding.inflate(getLayoutInflater());
        overflowMenuLayoutBinding.ivMenu.setOnClickListener(view -> openCreateGroupDialog());
        binding.group.setOverflowMenu(overflowMenuLayoutBinding.getRoot());
    }

    /**
     * Opens a dialog for creating a new group.
     */
    private void openCreateGroupDialog() {
        CreateGroupLayoutBinding createGroupLayoutBinding = CreateGroupLayoutBinding.inflate(getLayoutInflater());
        tvError = createGroupLayoutBinding.tvError;
        tvButtonText = createGroupLayoutBinding.createGroupBtnText;
        progressBar = createGroupLayoutBinding.createGroupProgress;
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel().toBuilder()
                                                                              .setTopLeftCorner(CornerFamily.ROUNDED,
                                                                                                getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_radius_4)
                                                                              )
                                                                              .setTopRightCorner(CornerFamily.ROUNDED,
                                                                                                 getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_radius_4)
                                                                              )
                                                                              .setBottomLeftCorner(
                                                                                      CornerFamily.ROUNDED,
                                                                                      0
                                                                              )
                                                                              .setBottomRightCorner(CornerFamily.ROUNDED, 0)
                                                                              .build();
        createGroupLayoutBinding.createGroupCard.setShapeAppearanceModel(shapeAppearanceModel);

        AtomicReference<String> groupType = new AtomicReference<>(UIKitConstants.GroupType.PUBLIC);
        createGroupLayoutBinding.toggle.setOnCheckedChangeListener((radio, i) -> {
            if (i == R.id.radio_public) {
                groupType.set(UIKitConstants.GroupType.PUBLIC);
                hidePasswordField(createGroupLayoutBinding);
            } else if (i == R.id.radio_private) {
                groupType.set(UIKitConstants.GroupType.PRIVATE);
                hidePasswordField(createGroupLayoutBinding);
            } else if (i == R.id.radio_password) {
                groupType.set(UIKitConstants.GroupType.PASSWORD);
                showPasswordField(createGroupLayoutBinding);
            }
        });
        createGroupLayoutBinding.createGroupBtn.setOnClickListener(view -> {
            Group group = new Group();
            group.setGuid(String.valueOf(System.currentTimeMillis()));
            group.setName(createGroupLayoutBinding.etName.getText().toString().trim());
            group.setGroupType(groupType.get());
            group.setPassword(createGroupLayoutBinding.etPassword.getText().toString().trim());
            viewModel.createGroup(group);
        });
        Utils.showBottomSheet(getContext(), bottomSheetDialog, true, false, createGroupLayoutBinding.getRoot());
        bottomSheetDialog.show();
    }

    /**
     * Hides the password field in the create group dialog.
     *
     * @param createGroupLayoutBinding The binding object for the create group layout.
     */
    private void hidePasswordField(CreateGroupLayoutBinding createGroupLayoutBinding) {
        AnimationUtils.animateVisibilityGone(createGroupLayoutBinding.groupPasswordCard);
    }

    /**
     * Shows the password field in the create group dialog.
     *
     * @param createGroupLayoutBinding The binding object for the create group layout.
     */
    private void showPasswordField(CreateGroupLayoutBinding createGroupLayoutBinding) {
        AnimationUtils.animateVisibilityVisible(createGroupLayoutBinding.groupPasswordCard);
    }
}
