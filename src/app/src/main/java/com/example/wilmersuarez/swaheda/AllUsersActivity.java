package com.example.wilmersuarez.swaheda;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersActivity extends AppCompatActivity {

    private RecyclerView allUsersList;
    private DatabaseReference allUsersDatabaseRef;
    private EditText searchInputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        // Reference to users in database
        allUsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        allUsersDatabaseRef.keepSynced(true);   // Load data offline

        // Toolbar for activity setup
        android.support.v7.widget.Toolbar mToolbar = findViewById(R.id.all_users_bar_id);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Find Friends");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));

        // Search views setup
        searchInputText = findViewById(R.id.inputSearch_id);
        ImageButton searchButton = findViewById(R.id.searchButton_id);

        // User list setup (RecycleR View)
        allUsersList = findViewById(R.id.allUsersList);
        allUsersList.setLayoutManager(new LinearLayoutManager(this));

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userNameSearch = searchInputText.getText().toString();

                // Only search if user entered a name
                if(!TextUtils.isEmpty(userNameSearch)) {
                    SearchFriends(userNameSearch);
                }
            }
        });
    }

    // Update user list on Activity Start
    private void SearchFriends(String userNameSearch){
        // Search for the User entered
        Query searchFriendsQ = allUsersDatabaseRef.orderByChild("user_name")
                .startAt(userNameSearch).endAt(userNameSearch + "\uf8ff");

        FirebaseRecyclerAdapter<AllUsers, AllUsersViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<AllUsers, AllUsersViewHolder>
                (
                        AllUsers.class,
                        R.layout.all_users_display_layout,
                        AllUsersViewHolder.class,
                        searchFriendsQ
                ) {

                        @Override
                        protected void populateViewHolder(AllUsersViewHolder viewHolder, AllUsers model, final int position) {
                            viewHolder.setUser_name(model.getUser_name());
                            viewHolder.setUser_image(model.getUser_image());

                            viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String visit_user_id = getRef(position).getKey();

                                    // When a user clicks on another user, send them to their profile
                                    Intent profileIntent = new Intent(AllUsersActivity.this, ProfileActivity.class);
                                    profileIntent.putExtra("visit_user_id", visit_user_id);
                                    startActivity(profileIntent);
                                }
                            });
                        }
                  };

        allUsersList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class AllUsersViewHolder extends RecyclerView.ViewHolder {

        View mView;
        public AllUsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setUser_name(String user_name) {
            // Get user name from database
            TextView name = mView.findViewById(R.id.allUsers_Username_id);
            // Set the username text in the Username field of the all_users_display_layout
            name.setText(user_name);
        }

        public void setUser_image(final String user_image) {
            // Get profile image from database
            final CircleImageView profileImage = mView.findViewById(R.id.allUsersProfileImage_id);
            // Set the profile image in the User image field of the all_users_display_layout

            Picasso.get()
                    .load(user_image)
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
                                    .load(user_image)
                                    .placeholder(R.drawable.default_profile_picture)
                                    .into(profileImage);
                        }
                    });

        }
    }
}
