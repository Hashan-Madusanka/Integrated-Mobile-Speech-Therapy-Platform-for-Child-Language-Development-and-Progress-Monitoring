package com.example.speechtherapy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;

public class ThreapyReportActivity extends AppCompatActivity {
    ImageView home;
    Button button1,button2,share_report;
    private Button generateReportBtn,downloadReportBtn;
    private ProgressDialog progressDialog;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private LinearLayout pdfPreviewLayout;
    private ImageView pdfImageView;
    private String currentUserId;

    private File pdfFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.threapy_report);
        home = findViewById(R.id.home);
        button1= findViewById(R.id.button1);
        button2= findViewById(R.id.button2);

        generateReportBtn = findViewById(R.id.generete_report);
        downloadReportBtn = findViewById(R.id.download_report);
        pdfPreviewLayout = findViewById(R.id.pdf);
        pdfImageView = new ImageView(this); // ImageView to display PDF preview
        pdfPreviewLayout.addView(pdfImageView); // Add ImageView dynamically
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProgressTrackingActivity();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPremiumStatus();
            }
        });

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("user");

        // Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Generating Report...");
        progressDialog.setCancelable(false);

        // Button Listeners
        generateReportBtn.setOnClickListener(v -> generatePDFReport());
        downloadReportBtn.setOnClickListener(v -> downloadPDF());


    }
    private void checkPremiumStatus() {
        FirebaseUser currentUser =  mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child(userId).child("isPremium").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            Boolean isPremium = snapshot.getValue(Boolean.class);
                            if (isPremium != null && isPremium) {
                                // User is a premium user, open chat activity
                                openchatActivity();
                            } else {
                                // User is not a premium user, show Snackbar
                                View rootView = findViewById(android.R.id.content);
                                Snackbar.make(rootView, "You are not a premium user, Activate premium status", Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("Firebase", "isPremium field does not exist");
                            Toast.makeText(this, "Error: Unable to determine premium status", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firebase", "Error fetching isPremium status", e));
        } else {
            Log.e("Firebase", "User is not authenticated");
            Toast.makeText(this, "Please log in to access this feature", Toast.LENGTH_SHORT).show();
        }
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    public void openProgressTrackingActivity() {
        Intent intent = new Intent(this, ProgressTrackingActivity.class);
        startActivity(intent);
    }

    private void generatePDFReport() {
        progressDialog.show();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            databaseReference.child(currentUserId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DataSnapshot snapshot = task.getResult();

                    String uname = snapshot.child("uname").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String contact = snapshot.child("contact").getValue(String.class);
                    Float voiceAnalysis = snapshot.child("voice_analysis").getValue(Float.class);
                    Boolean hearingLoss = snapshot.child("hearingloss").getValue(Boolean.class);
                    Boolean cleftPalate = snapshot.child("cleft_palate").getValue(Boolean.class);
                    Boolean languageDelay = snapshot.child("languagedelay").getValue(Boolean.class);

                    // Handle missing integer values with a default of 0
                    int singingSongScore = snapshot.child("sing_song").getValue(Integer.class) != null
                            ? snapshot.child("sing_song").getValue(Integer.class) : 0;
                    int singingSongScoreEn = snapshot.child("sing_songEn").getValue(Integer.class) != null
                            ? snapshot.child("sing_songEn").getValue(Integer.class) : 0;
                    int spellWordScore = snapshot.child("spell_word").getValue(Integer.class) != null
                            ? snapshot.child("spell_word").getValue(Integer.class) : 0;
                    int spellWordScoreEn = snapshot.child("spell_wordEn").getValue(Integer.class) != null
                            ? snapshot.child("spell_wordEn").getValue(Integer.class) : 0;
                    int volumeScore = snapshot.child("hear_volume").getValue(Integer.class) != null
                            ? snapshot.child("hear_volume").getValue(Integer.class) : 0;
                    int storyScore = snapshot.child("listen_story").getValue(Integer.class) != null
                            ? snapshot.child("listen_story").getValue(Integer.class) : 0;
                    int storyScoreEn = snapshot.child("listen_storyEn").getValue(Integer.class) != null
                            ? snapshot.child("listen_storyEn").getValue(Integer.class) : 0;
                    int trysound = snapshot.child("try_sound").getValue(Integer.class) != null
                            ? snapshot.child("try_sound").getValue(Integer.class) : 0;
                    int catchsound = snapshot.child("catch_sound").getValue(Integer.class) != null
                            ? snapshot.child("catch_sound").getValue(Integer.class) : 0;

                    // Provide default values for strings if they are null
                    uname = uname != null ? uname : "Unknown";
                    email = email != null ? email : "No Email";
                    contact = contact != null ? contact : "No Contact";
                    

                    createPDF(uname, email, contact, voiceAnalysis, hearingLoss, cleftPalate, languageDelay,
                            singingSongScore,singingSongScoreEn, spellWordScore,spellWordScoreEn, volumeScore, storyScore,storyScoreEn, trysound, catchsound);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
        }
    }


    private void createPDF(String uname, String email, String contact, Float voiceAnalysis,
                           Boolean hearingLoss, Boolean cleftPalate, Boolean languageDelay,
                           int singingSongScore,int singingSongScoreEn, int spellWordScore, int spellWordScoreEn, int volumeScore,
                           int storyScore,int storyScoreEn, int trysound, int catchsound) {
        try {
            // File path for internal storage
            String fileName = uname + "_Report.pdf";
            pdfFile = new File(getFilesDir(), fileName);

            // Generate PDF
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Define fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 18);

            // Add data to PDF with fonts
            document.add(new Paragraph("---------------------------------------------------------------------", subtitleFont));
            document.add(new Paragraph("------------------Therapy Report-----------------", titleFont));
            document.add(new Paragraph("---------------------------------------------------------------------", subtitleFont));
            document.add(new Paragraph("Kid Name: " + uname, contentFont));
            document.add(new Paragraph("Email: " + email, contentFont));
            document.add(new Paragraph("Contact Number: " + contact, contentFont));
            document.add(new Paragraph("---Voice analysis Result---", titleFont));
            document.add(new Paragraph("Voice analysis result: " + voiceAnalysis, contentFont));
            document.add(new Paragraph("--------------------------------------------------", subtitleFont));

            // Add conditional paragraphs with fonts
            if (hearingLoss != null) {
                document.add(new Paragraph("Hearing loss result: " +
                        (hearingLoss ? "has hearing loss issue" : "no hearing loss issue"), contentFont));
            }
            if (cleftPalate != null) {
                document.add(new Paragraph("Cleft palate result: " +
                        (cleftPalate ? "has cleft palate issue" : "no cleft palate issue"), contentFont));
            }
            if (languageDelay != null) {
                document.add(new Paragraph("Language delay result: " +
                        (languageDelay ? "has language delay" : "no language delay"), contentFont));
            }

            // Add game results with fonts
            document.add(new Paragraph("----------------------------------------------------------------------", subtitleFont));
            document.add(new Paragraph("---Game Results---", titleFont));
            document.add(new Paragraph("Singing Sinhala Song Score: " + singingSongScore + "%", contentFont));
            document.add(new Paragraph("Singing English Song Score: " + singingSongScoreEn + "%", contentFont));
            document.add(new Paragraph("Spell Word Sinhala Language Score: " + spellWordScore + "%", contentFont));
            document.add(new Paragraph("Spell Word English Language Score: " + spellWordScoreEn + "%", contentFont));
            document.add(new Paragraph("Hearing Hirtz Range Volume Score: " + volumeScore + "%", contentFont));
            document.add(new Paragraph("Listening Sinhala Story Reading Score: " + storyScore + "%", contentFont));
            document.add(new Paragraph("Listening English Story Reading Score: " + storyScoreEn + "%", contentFont));
            document.add(new Paragraph("Try different sound Score: " + trysound + "%", contentFont));
            document.add(new Paragraph("Catch different sound Score: " + catchsound + "%", contentFont));
            document.add(new Paragraph("----------------------------------------------------------------------", subtitleFont));

            document.close();

            progressDialog.dismiss();
            renderPDFPreview(); // Preview the PDF
            Toast.makeText(this, "Report Generated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private void downloadPDF() {
        if (pdfFile != null && pdfFile.exists()) {
            try {
                // Copy file to Downloads folder
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File downloadFile = new File(downloadsDir, pdfFile.getName());
                FileOutputStream outputStream = new FileOutputStream(downloadFile);
                outputStream.write(java.nio.file.Files.readAllBytes(pdfFile.toPath()));
                outputStream.close();

                Toast.makeText(this, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No PDF available to download!", Toast.LENGTH_SHORT).show();
        }
    }
    private void renderPDFPreview() {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(this, "PDF file not found for preview!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Open the PDF file for rendering
            ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);

            // Render the first page of the PDF
            if (pdfRenderer.getPageCount() > 0) {
                PdfRenderer.Page page = pdfRenderer.openPage(0);

                // Create a bitmap for the page
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                // Set the bitmap to the ImageView
                pdfImageView.setImageBitmap(bitmap);

                // Clean up
                page.close();
            } else {
                Toast.makeText(this, "No pages in the PDF!", Toast.LENGTH_SHORT).show();
            }

            pdfRenderer.close();
            parcelFileDescriptor.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to render PDF preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void openchatActivity() {
        Intent intent = new Intent(this, TherapistListActivity.class);
        startActivity(intent);
    }
}