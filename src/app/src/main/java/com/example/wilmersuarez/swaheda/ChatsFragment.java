package com.example.wilmersuarez.swaheda;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {
    private RecyclerView chatsList_RV;
    private DatabaseReference friendsRef;
    private DatabaseReference userRef;
    String onlineUserId;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_chats, container, false);

        // Link mainView with the RecyclerView used to present the Chats list
        chatsList_RV = mView.findViewById(R.id.chats_list_RV);
        chatsList_RV.setHasFixedSize(true);

        // Get current user ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();

        // Get reference to Friends Node with current user in database
        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child(onlineUserId);
        friendsRef.keepSynced(true);    // Allow offline
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.keepSynced(true);   // Allow offline

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);  // Show old chats last

        chatsList_RV.setLayoutManager(linearLayoutManager);

        // Inflate the layout for this fragment
        return mView;
    }

    // Update Chat list
    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Chats, ChatsFragment.ChatsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Chats, ChatsFragment.ChatsViewHolder>
                (
                        Chats.class,
                        R.layout.all_users_display_layout,
                        ChatsFragment.ChatsViewHolder.class,
                        friendsRef
                ) {

            @Override
            protected void populateViewHolder(final ChatsFragment.ChatsViewHolder viewHolder, Chats model, final int position) {
                final String listUser_id = getRef(position).getKey();
                userRef.child(listUser_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                        String thumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                        if(dataSnapshot.hasChild("online")) {
                            String onlineStatus = dataSnapshot.child("online").getValue().toString();
                            viewHolder.setUserStatus(onlineStatus);
                        }

                        viewHolder.setUserName(userName);
                        viewHolder.setThumbImage(thumbImage);

                        // When the user selects one of the friends
                        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // If user has 'online' node exists in firebase database
                                if(dataSnapshot.child("online").exists()) {
                                    // Go to chat activity with selected user
                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    // Send id of selected user
                                    chatIntent.putExtra("visit_user_id", listUser_id);
                                    // Send the selected users name
                                    chatIntent.putExtra("user_name", userName);
                                    startActivity(chatIntent);
                                } else {
                                    // add 'online' node
                                    userRef.child(listUser_id).child("online")
                                            .setValue(ServerValue.TIMESTAMP).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Go to chat activity with selected user
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            // Send id of selected user
                                            chatIntent.putExtra("visit_user_id", listUser_id);
                                            // Send the selected users name
                                            chatIntent.putExtra("user_name", userName);
                                            startActivity(chatIntent);
                                        }
                                    });
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });

            }
        };

        chatsList_RV.setAdapter(firebaseRecyclerAdapter);
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder{
        View mView;

        public ChatsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        // Friend name
        public void setUserName(String userName) {
            TextView userNameDisplay = mView.findViewById(R.id.allUsers_Username_id);
            userNameDisplay.setText(userName);
        }

        // Friend profile image
        public void setThumbImage(final String thumbImage) {
            // Get profile image from database
            final CircleImageView profileImage = mView.findViewById(R.id.allUsersProfileImage_id);
            // Set the profile image
            Picasso.get()
                    .load(thumbImage)
                    .networkPolicy(NetworkPolicy.OFFLINE)   // Allow offline picture loading
                    .placeholder(R.drawable.default_profile_picture)
                    .into(profileImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            // If images failed to load
                            Picasso.get()
                                    .load(thumbImage)
                                    .placeholder(R.drawable.default_profile_picture)
                                    .into(profileImage);
                        }
                    });
        }

        // Friend online status
        public void setUserStatus(String onlineStatus) {
            ImageView onlineStatusIcon = mView.findViewById(R.id.onlineStatus_icon);
            if(onlineStatus.equals("true")) {
                onlineStatusIcon.setVisibility(View.VISIBLE);
            } else {
                onlineStatusIcon.setVisibility(View.INVISIBLE);
            }
        }
    }
}
