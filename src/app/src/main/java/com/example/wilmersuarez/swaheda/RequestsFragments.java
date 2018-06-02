package com.example.wilmersuarez.swaheda;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragments extends Fragment {
    private RecyclerView requestListRV;
    private DatabaseReference requestRef;
    private DatabaseReference usersRef;
    private DatabaseReference friendsRef;
    private DatabaseReference friendReqRef;
    private String listUsersId;
    private String onlineUserID;
    private Button acceptRqstBtn;
    private Button declineRqstBtn;

    public RequestsFragments() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_requests_fragments, container, false);
        requestListRV = mView.findViewById(R.id.request_List_RV);
        requestListRV.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);  // Show recent request first

        requestListRV.setLayoutManager(linearLayoutManager);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        onlineUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // Get requests of current user reference
        requestRef = FirebaseDatabase.getInstance().getReference().child("FriendRequests").child(onlineUserID);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends");
        friendReqRef = FirebaseDatabase.getInstance().getReference().child("FriendRequests");

        // Inflate the layout for this fragment
        return mView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Requests, RequestViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Requests, RequestViewHolder>
                (
                        Requests.class,
                        R.layout.friend_request_user_layout,
                        RequestsFragments.RequestViewHolder.class,
                        requestRef
                )
        {
            @Override
            protected void populateViewHolder(final RequestViewHolder viewHolder, Requests model, int position) {
                listUsersId = getRef(position).getKey();

                // Get the reference to the request type(only display requests which are recieved)
                DatabaseReference getTypeRequestRef = getRef(position).child("request_type").getRef();

                getTypeRequestRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            // Retrieve request type value
                            String requestType = Objects.requireNonNull(dataSnapshot.getValue()).toString();

                            if(requestType.equals("friend_request_received")) {
                                usersRef.child(listUsersId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        final String userName = Objects.requireNonNull(dataSnapshot.child("user_name").getValue()).toString();
                                        final String thumbImage = Objects.requireNonNull(dataSnapshot.child("user_thumb_image").getValue()).toString();

                                        viewHolder.setUserName(userName);
                                        viewHolder.setThumbImage(thumbImage);

                                        acceptRqstBtn = viewHolder.mView.findViewById(R.id.accept_requestBtn_id);
                                        declineRqstBtn = viewHolder.mView.findViewById(R.id.decline_requestBtn_id);

                                        // If request accepted
                                        acceptRqstBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                // Save the date the two users became friends
                                                Calendar date = Calendar.getInstance();
                                                @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate = new SimpleDateFormat("MMMM-dd-yyyy");
                                                final String formattedDate = currentDate.format(date.getTime());

                                                friendsRef.child(onlineUserID).child(listUsersId).child("date").setValue(formattedDate)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                friendsRef.child(listUsersId).child(onlineUserID).child("date").setValue(formattedDate)
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {
                                                                                // Remove request of sender once they become friends
                                                                                friendReqRef.child(onlineUserID).child(listUsersId).removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if(task.isSuccessful()) {
                                                                                                    // Remove request received by receiver once they become friends
                                                                                                    friendReqRef.child(listUsersId).child(onlineUserID).removeValue();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        });
                                                            }
                                                        });
                                            }
                                        });
                                        // Decline request
                                        declineRqstBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                // Remove request of sender
                                                friendReqRef.child(onlineUserID).child(listUsersId).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()) {
                                                                    // Remove request received by receiver
                                                                    friendReqRef.child(listUsersId).child(onlineUserID).removeValue();
                                                                }
                                                            }
                                                        });
                                            }
                                        });

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            } else if(requestType.equals("friend_request_sent")) {
                                Button acc_sentBtn = viewHolder.mView.findViewById(R.id.accept_requestBtn_id);
                                acc_sentBtn.setText(R.string.request_sent_btn_change);
                                acc_sentBtn.setEnabled(false);  // Disable button

                                Button dec_sentBtn = viewHolder.mView.findViewById(R.id.decline_requestBtn_id);
                                dec_sentBtn.setVisibility(View.INVISIBLE);

                                usersRef.child(listUsersId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        final String userName = Objects.requireNonNull(dataSnapshot.child("user_name").getValue()).toString();
                                        final String thumbImage = Objects.requireNonNull(dataSnapshot.child("user_thumb_image").getValue()).toString();

                                        viewHolder.setUserName(userName);
                                        viewHolder.setThumbImage(thumbImage);

                                        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[] = new CharSequence[] {
                                                        "Cancel Request?",
                                                };

                                                // Create Dialog Box
                                                android.support.v7.app.AlertDialog.Builder builder
                                                        = new android.support.v7.app.AlertDialog.Builder(Objects.requireNonNull(getContext()));

                                                // Interface
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int position) {
                                                        // Profile
                                                        if(position == 0) {
                                                            // Remove request of sender
                                                            friendReqRef.child(onlineUserID).child(listUsersId).removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()) {
                                                                                // Remove request received by receiver
                                                                                friendReqRef.child(listUsersId).child(onlineUserID).removeValue();
                                                                            }
                                                                        }
                                                                    });
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
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };
        requestListRV.setAdapter(firebaseRecyclerAdapter);
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        View mView;

        public RequestViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        // User name
        public void setUserName(String userName) {
            TextView user_Name = mView.findViewById(R.id.request_username_id);
            user_Name.setText(userName);
        }

        public void setThumbImage(final String thumbImage) {
            // Get profile image from database
            final CircleImageView profileImage = mView.findViewById(R.id.request_profile_image_id);
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
    }
}
