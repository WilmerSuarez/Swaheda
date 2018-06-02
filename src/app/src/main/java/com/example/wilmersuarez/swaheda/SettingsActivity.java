package com.example.wilmersuarez.swaheda;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {
    private static final int GALLERY_REQUEST = 1;
    private CircleImageView settingProfileImage;
    private TextView settingName;
    private StorageReference storeProfileImageRef;
    private DatabaseReference getUserData;
    private FirebaseAuth mAuth;
    private Bitmap thumbImageBitmap = null;
    private StorageReference thumbImageRef;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Database Reference setup
        mAuth = FirebaseAuth.getInstance();
        String currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // Storage reference setup
        storeProfileImageRef = FirebaseStorage.getInstance().getReference("Profile_Pictures");
        thumbImageRef = FirebaseStorage.getInstance().getReference().child("ThumbImags");

        //Reference now points to the currents user information in the database
        getUserData = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        getUserData.keepSynced(true);

        // Setup Views
        settingProfileImage = findViewById(R.id.userImage_id);
        settingName = findViewById(R.id.user_image_name_id);
        Button settingChangeImage = findViewById(R.id.change_pictureBtn_id);

        // Loading bar
        loadingBar = new ProgressDialog(this);

        getUserData.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Data snapshot object is used to get all the user data
                // Get the current users data stored in the database
                String name = Objects.requireNonNull(dataSnapshot.child("user_name").getValue()).toString();
                final String image = Objects.requireNonNull(dataSnapshot.child("user_image").getValue()).toString();
                Objects.requireNonNull(dataSnapshot.child("user_thumb_image").getValue()).toString();

                settingName.setText(name);
                if(!image.equals("default_profile_picture")) {
                    Picasso.get()
                            .load(image)
                            .networkPolicy(NetworkPolicy.OFFLINE)   // Allow images to load offline
                            .placeholder(R.drawable.default_profile_picture)
                            .resize(900, 900).centerInside()
                            .into(settingProfileImage, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError(Exception e) {
                                    // If image fails to load
                                    Picasso.get().load(image)
                                            .placeholder(R.drawable.default_profile_picture)
                                            .resize(900, 900)
                                            .centerInside()
                                            .into(settingProfileImage);
                                }
                            });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Allow the user to change their profile image
        settingChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        openFileChooser();
                    } else {
                        ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    }
                } else {
                    openFileChooser();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                Toast.makeText(this, "External write permission has not been granted.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Open gallery to choose file
    private void openFileChooser() {
        Intent imageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageIntent.setType("image/*");
        startActivityForResult(imageIntent, GALLERY_REQUEST);
    }

    private void uploadImage(Uri croppedUri, final byte thumbByte[]) {
        if(croppedUri != null) {
            // Use the users ID to name the users profile image
            String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
            StorageReference filePath = storeProfileImageRef.child(userId + ".jpg");   // Create user profile image name

            final StorageReference thumbFilePath = thumbImageRef.child(userId + ".jpg");

            // Store the file in the Firebase storage
            filePath.putFile(croppedUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()) {
                        // Get Image Url from Firebase Storage
                        final String imageDownloadUrl= Objects.requireNonNull(task.getResult().getDownloadUrl()).toString();

                        UploadTask uploadTask = thumbFilePath.putBytes(thumbByte);

                        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbImage_task) {
                                String thumbDownloadUrl = Objects.requireNonNull(thumbImage_task.getResult().getDownloadUrl()).toString();
                                if(task.isSuccessful()) {
                                    HashMap<String, Object> updateUserData = new HashMap<>();
                                    updateUserData.put("user_image", imageDownloadUrl);
                                    updateUserData.put("user_thumb_image", thumbDownloadUrl);

                                    // Upload Url to firebase database for current user
                                    getUserData.updateChildren(updateUserData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                loadingBar.dismiss();
                                            } else {
                                                Toast.makeText(SettingsActivity.this, "Image upload failed! Try again...", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            }
                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                loadingBar.setMessage("Please wait... " + ((int) progress) + "%");
                            }
                        });
                    } else {
                        Toast.makeText(SettingsActivity.this, "Image upload failed! Try again...", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        } else {
            Toast.makeText(this, "No File Selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri mImageUri = data.getData();
            // Crop the image
            CropImage.activity(mImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            // Get results of cropped image
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                loadingBar.setTitle("Updating Profile Image");
                loadingBar.show();

                Uri resultUri = result.getUri();

                // Get the path of the cropped image
                File thumbImagePathUri = new File(resultUri.getPath());

                // Compress image (for faster loading)
                try {
                    thumbImageBitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(100)
                            .compressToBitmap(thumbImagePathUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                thumbImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                final byte[] thumbImageByte = byteArrayOutputStream.toByteArray();

                // Upload image to Firebase Storage
                uploadImage(resultUri, thumbImageByte);
            }
        }
    }
}
