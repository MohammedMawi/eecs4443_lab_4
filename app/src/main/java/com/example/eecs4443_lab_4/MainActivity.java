package com.example.eecs4443_lab_4;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Used for saving and restoring selected image URI between configuration changes (like rotation)
    private static final String KEY_SELECTED_URI = "selected_uri";

    // URI of the currently displayed image (from camera or gallery)
    private Uri currentPhotoUri;

    // Temporary URI where a captured photo will be saved
    private Uri photoUri;

    // UI references
    private ImageView img;
    private TextView statusText;

    // Uses the modern Activity Result API to ask the user for permission
    // and automatically handle their response.
    private final ActivityResultLauncher<String> requestStoragePermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // If user allows permission → open the gallery
                    openGallery();
                } else {
                    // If denied → show message on screen and a short Toast
                    Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show();
                    statusText.setText("Permission denied.");
                }
            });

    // Handles the result when the user picks (or cancels picking) a photo.
    private final ActivityResultLauncher<Intent> pickImageFromGallery =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // RESULT_OK means user selected a photo
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Save and display the chosen image
                        currentPhotoUri = uri;
                        img.setImageURI(uri);
                        statusText.setText("Selected image from gallery.");
                    }
                } else {
                    // If user cancels → reset to placeholder image
                    img.setImageResource(R.drawable.placeholder);
                    statusText.setText("No image selected. Showing default.");
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // If permission granted → open the camera
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
                    statusText.setText("Camera permission denied.");
                }
            });

    // Handles what happens after the camera activity returns.
    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // RESULT_OK means photo was successfully captured
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (photoUri != null) {
                        currentPhotoUri = photoUri;
                        img.setImageURI(photoUri);
                        statusText.setText("Photo captured successfully.");
                    }
                } else {
                    // User canceled or something went wrong
                    statusText.setText("Camera cancelled or failed.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Get references to UI elements
        img = findViewById(R.id.pfPic);
        statusText = findViewById(R.id.statusText);
        Button selectButton = findViewById(R.id.selectButton);
        Button takeButton = findViewById(R.id.takeButton);

        // "Select from Gallery" button behavior
        selectButton.setOnClickListener(v -> {
            if (hasGalleryPermission()) {
                openGallery(); // Already has permission → go straight to gallery
            } else {
                // Request the correct permission based on Android version
                requestStoragePermission.launch(requiredGalleryPermission());
            }
        });

        // "Take Photo" button behavior
        takeButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
            }
        });

        // Restore previously selected or captured image (if screen rotated)
        if (savedInstanceState != null) {
            String savedUri = savedInstanceState.getString(KEY_SELECTED_URI);
            if (savedUri != null) {
                currentPhotoUri = Uri.parse(savedUri);
                img.setImageURI(currentPhotoUri);
            }
        }

        // Keeps layout spacing consistent with system bars (top, bottom, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean hasGalleryPermission() {
        return ContextCompat.checkSelfPermission(this, requiredGalleryPermission())
                == PackageManager.PERMISSION_GRANTED;
    }

    // Android 13+ (TIRAMISU) uses new READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
    private String requiredGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    private void openGallery() {
        // ACTION_GET_CONTENT opens the system file picker for selecting an image
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*"); // only show image files
        pickImageFromGallery.launch(Intent.createChooser(galleryIntent, "Select Picture"));
    }

    private void openCamera() {
        // ACTION_IMAGE_CAPTURE opens the default camera app
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create a temporary file in app’s external cache directory to store the image
        File photoFile = new File(getExternalCacheDir(), "temp_photo.jpg");

        // FileProvider gives other apps (camera) temporary permission to write the image
        photoUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider",
                photoFile);

        // Tell the camera app to save the photo to this location
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        // Launch the camera intent
        takePhotoLauncher.launch(cameraIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoUri != null) {
            // Save URI as a string so it can be restored later
            outState.putString(KEY_SELECTED_URI, currentPhotoUri.toString());
        }
    }
}
