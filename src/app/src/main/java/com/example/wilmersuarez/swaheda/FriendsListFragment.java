package com.example.wilmersuarez.swaheda;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
public class FriendsListFragment extends Fragment {

    private RecyclerView friendsList;
    private DatabaseReference friendsRef;
    private DatabaseReference userRef;
    String onlineUserId;

    public FriendsListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_friends_list, container, false);

        // Link mainView with the RecyclerView used to present the Friends List
        friendsList = mainView.findViewById(R.id.friends_List_RV);
        friendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsList.setHasFixedSize(true);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();

        // Get reference to Friends Node with current user in database
        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child(onlineUserId);
        friendsRef.keepSynced(true);    // Allow offline
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.keepSynced(true);   // Allow offline

        // Inflate the layout for this fragment
        return mainView;
    }

    // Update Friend list on Activity Start
    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<AllFriends, AllFriendsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<AllFriends, AllFriendsViewHolder>
                (
                        AllFriends.class,
                        R.layout.all_users_display_layout,
                        AllFriendsViewHolder.class,
                        friendsRef
                ) {

            @Override
            protected void populateViewHolder(final AllFriendsViewHolder viewHolder, AllFriends model, final int position) {
                viewHolder.setDate(model.getDate());

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
                                CharSequence options[] = new CharSequence[] {
                                        "View Profile", // Option one
                                        "Send Message"  // Option two
                                };

                                // Create Dialog Box
                                android.support.v7.app.AlertDialog.Builder builder
                                        = new android.support.v7.app.AlertDialog.Builder(getContext());

                                // Interface
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int position) {
                                        // Profile
                                        if(position == 0) {
                                            // Send to selected user's profile
                                            Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                            // Send Profile activity id of user clicked
                                            profileIntent.putExtra("visit_user_id", listUser_id);
                                            startActivity(profileIntent);
                                        // Message
                                        } else if (position == 1) {
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
                                    }
                                });
                                // Show the created dialog box
                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        friendsList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class AllFriendsViewHolder extends RecyclerView.ViewHolder{
        View mView;

        public AllFriendsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        // Friend Date
        @SuppressLint("SetTextI18n")
        public void setDate(String date) {
            // Get date from database Friends node
            TextView friendsSinceDate = mView.findViewById(R.id.allUsers_date_id);
            friendsSinceDate.setText("Friends Since: " + date);
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
