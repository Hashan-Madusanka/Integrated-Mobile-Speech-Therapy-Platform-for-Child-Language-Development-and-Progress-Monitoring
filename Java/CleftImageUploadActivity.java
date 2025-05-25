package com.example.speechtherapy;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CleftImageUploadActivity extends AppCompatActivity {
    ImageView home;
    TextView text;
    private ImageView imageView;
    private Button uploadButton;
    private Uri imageUri;
    private ProgressDialog progressDialog;
    private static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203;
    private static final int REQUEST_IMAGE_CROP = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private final ActivityResultLauncher<CropImageContractOptions> cropImage = registerForActivityResult(
            new CropImageContract(),
            result -> {
                if (result.isSuccessful()) {
                    // Use the cropped image URI.
                    Uri croppedImageUri = result.getUriContent();
                    imageView.setImageURI(croppedImageUri);
                    imageUri = croppedImageUri;
                } else {
                    // Handle the error.
                    Exception exception = result.getError();
                    Toast.makeText(this, "Crop error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
    );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cleft_image_upload);
        home = findViewById(R.id.home);
        text= findViewById(R.id.textView8);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait for image detection...");
        progressDialog.setCancelable(false);
        imageView = findViewById(R.id.imageView);
        findViewById(R.id.gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
        findViewById(R.id.cam).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission();
            }
        });
        uploadButton = findViewById(R.id.buttonupload);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void openhearingActivity() {
        Intent intent = new Intent(this, TherapyQuestion.class);
        startActivity(intent);
    }
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK);
    }
    private void uploadImage() {
        if (imageUri != null) {
            progressDialog.show();
            // Pass the Uri directly to the AsyncTask
            new CleftImageUploadActivity.UploadImageTask().execute(imageUri.toString());
            // Use imageUri directly, not imageUri.toString()
        } else {
            progressDialog.dismiss();
            Snackbar.make(findViewById(android.R.id.content), "No image selected", Snackbar.LENGTH_SHORT).show();
        }
    }
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // Permission has already been granted
            dispatchTakePictureIntent();
        }
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.speechtherapy", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                imageUri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
                dispatchTakePictureIntent();
            } else {
                // Camera permission denied
                Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                startCrop(imageUri);  // Start cropping the captured image
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
                startCrop(imageUri);  // Start cropping the selected image
            }
        }
    }
    // Function to start the crop activity
    private void startCrop(Uri uri) {
        CropImageContractOptions options = new CropImageContractOptions(uri, new CropImageOptions());
        cropImage.launch(options);
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    private class UploadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String imageUrl = strings[0];
            String responseMessage = "";
            try {
                // Get a file from the imageUri
                Uri imageUri = Uri.parse(imageUrl);
                // Convert URI to file (may need to copy content:// Uri to a temp file first)
                File file = createFileFromUri(imageUri);
                if (file == null) {
                    responseMessage = "Error: Could not find file";
                    return responseMessage;
                }
                // Create a HttpURLConnection to the URL endpoint
                URL url = new URL("https://cleft-389242265122.us-central1.run.app");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                // Create a MultipartEntityBuilder to build the request body
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                // Add the image file to the request body
                entityBuilder.addBinaryBody("image", file, ContentType.DEFAULT_BINARY, file.getName());
                HttpEntity entity = entityBuilder.build();
                urlConnection.setRequestProperty("Content-Type", entity.getContentType().getValue());
                // Write the request body to the connection output stream
                try (OutputStream out = urlConnection.getOutputStream()) {
                    entity.writeTo(out);
                }
                // Get the response from the server
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = urlConnection.getInputStream();
                    responseMessage = convertStreamToString(inputStream);
                } else {
                    responseMessage = "Error: " + responseCode;
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                responseMessage = "Exception: " + e.getMessage();
                Log.e("UploadImageTask", "Error uploading image: " + e.getMessage());
            }
            return responseMessage;
        }
        private File createFileFromUri(Uri uri) throws IOException {
            File tempFile = null;
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                String fileName = "upload_image.jpg";
                tempFile = new File(getCacheDir(), fileName);
                try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return tempFile;
        }
        private String convertStreamToString(InputStream inputStream) {
            java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
        @Override
        protected void onPostExecute(String responseMessage) {
            Log.e("UploadImageTask", "Response message: " + responseMessage);
            progressDialog.dismiss();
            try {
                JSONObject jsonObject = new JSONObject(responseMessage);
                if (jsonObject.has("error")) {
                    showAlertDialog("Error", "Please upload a valid image");
                    text.setText("Please upload a valid image");
                } else if (jsonObject.has("class")) {
                    String objects = jsonObject.getString("class");
                    // Check for "Unknown Class"
                    if (objects.equals("Unknown Class")) {
                        showAlertDialog("Error", "Please upload clear image");
                        text.setText("Please upload clear image");
                    } else if (objects.equals("[]")) {
                        showAlertDialog("Error", "Invalid image type");
                        text.setText("Please upload a valid image");
                    }else {

                        // Update Firebase based on response
                        if (objects.equals("Cleft-palate")) {
                            text.setText("Detected result: Cleft Palate");
                            updateFirebaseUserStatus(true);
                        } else if (objects.equals("NormalMouth")) {
                            text.setText("Detected result: Normal Mouth");
                            updateFirebaseUserStatus(false);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("JSON Parsing", "Error parsing JSON: " + e.getMessage());
            }
        }
    }
    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(CleftImageUploadActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with click action
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void updateFirebaseUserStatus(boolean isCleftPalate) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(currentUser.getUid());
            userRef.child("cleft_palate").setValue(isCleftPalate);
        } else {
            Log.e("Firebase", "No user is currently logged in");
        }
    }
}