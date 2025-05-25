package com.example.speechtherapy.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for managing progress tracking data with date support
 */
public class ProgressDataManager {
    private static final String TAG = "ProgressDataManager";
    
    // Database paths
    private static final String USER_PATH = "user";
    private static final String PROGRESS_HISTORY_PATH = "progress_history";
    private static final String PRONUNCIATION_ATTEMPTS_PATH = "pronunciation_attempts";
    private static final String GAME_SCORES_PATH = "game_scores";
    private static final String THERAPIST_COMMUNICATION_PATH = "therapist_communication";
    
    // Language constants
    public static final String LANGUAGE_SINHALA = "sinhala";
    public static final String LANGUAGE_ENGLISH = "english";
    
    // Date formatter for consistency
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    
    // Get today's date formatted as yyyy-MM-dd
    public static String getTodayDateString() {
        return DATE_FORMAT.format(new Date());
    }
    
    /**
     * Record a pronunciation attempt for the current user
     * @param isCorrect whether the pronunciation was correct
     * @param language the language of the attempt (LANGUAGE_SINHALA or LANGUAGE_ENGLISH)
     */
    public static void recordPronunciationAttempt(boolean isCorrect, String language) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        String today = getTodayDateString();
        
        DatabaseReference attemptRef = FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(PROGRESS_HISTORY_PATH)
                .child(today)
                .child(PRONUNCIATION_ATTEMPTS_PATH)
                .child(language);
        
        // Increment total and correct attempt counters
        attemptRef.child("total").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer totalAttempts = task.getResult().getValue(Integer.class);
                totalAttempts = (totalAttempts == null) ? 1 : totalAttempts + 1;
                attemptRef.child("total").setValue(totalAttempts);
                
                // Only update correct if the pronunciation was correct
                if (isCorrect) {
                    attemptRef.child("correct").get().addOnCompleteListener(correctTask -> {
                        if (correctTask.isSuccessful()) {
                            Integer correctAttempts = correctTask.getResult().getValue(Integer.class);
                            correctAttempts = (correctAttempts == null) ? 1 : correctAttempts + 1;
                            attemptRef.child("correct").setValue(correctAttempts);
                        }
                    });
                }
                
                // Calculate and update speech accuracy (percentage)
                updateSpeechAccuracy(userId, today, language);
            }
        });
    }
    
    /**
     * For backward compatibility
     * @param isCorrect whether the pronunciation was correct
     */
    public static void recordPronunciationAttempt(boolean isCorrect) {
        // Default to Sinhala for backward compatibility
        recordPronunciationAttempt(isCorrect, LANGUAGE_SINHALA);
    }
    
    /**
     * Update the speech accuracy calculation based on pronunciation attempts
     */
    private static void updateSpeechAccuracy(String userId, String date, String language) {
        DatabaseReference attemptRef = FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(PROGRESS_HISTORY_PATH)
                .child(date)
                .child(PRONUNCIATION_ATTEMPTS_PATH)
                .child(language);
        
        attemptRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();
                
                Integer totalAttempts = snapshot.child("total").getValue(Integer.class);
                Integer correctAttempts = snapshot.child("correct").getValue(Integer.class);
                
                if (totalAttempts != null && totalAttempts > 0) {
                    // Default to 0 if correctAttempts is null
                    correctAttempts = (correctAttempts == null) ? 0 : correctAttempts;
                    
                    // Calculate accuracy as a percentage
                    int accuracy = (int) (((float) correctAttempts / totalAttempts) * 100);
                    
                    // Update the speech_accuracy value with language specific key
                    FirebaseDatabase.getInstance()
                            .getReference(USER_PATH)
                            .child(userId)
                            .child(PROGRESS_HISTORY_PATH)
                            .child(date)
                            .child("speech_accuracy_" + language)
                            .setValue(accuracy);
                    
                    // Also update the general speech_accuracy for backward compatibility
                    if (language.equals(LANGUAGE_SINHALA)) {
                        FirebaseDatabase.getInstance()
                                .getReference(USER_PATH)
                                .child(userId)
                                .child(PROGRESS_HISTORY_PATH)
                                .child(date)
                                .child("speech_accuracy")
                                .setValue(accuracy);
                    }
                }
            }
        });
    }
    
    /**
     * Record a game score with the game type and language
     * @param gameType the type of game (e.g., "sing_song", "spell_word")
     * @param score the score achieved (0-100)
     * @param language the language of the game (LANGUAGE_SINHALA or LANGUAGE_ENGLISH)
     */
    public static void recordGameScore(String gameType, int score, String language) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        String today = getTodayDateString();
        
        // Create language-specific game type identifier
        String languageGameType = gameType + "_" + language;
        
        // Update the game score in progress history under language
        DatabaseReference gameScoreRef = FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(PROGRESS_HISTORY_PATH)
                .child(today)
                .child(GAME_SCORES_PATH)
                .child(languageGameType);
        
        gameScoreRef.setValue(score);
        
        // Also update the regular game type (for backward compatibility)
        DatabaseReference regularGameScoreRef = FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(PROGRESS_HISTORY_PATH)
                .child(today)
                .child(GAME_SCORES_PATH)
                .child(gameType);
        
        regularGameScoreRef.setValue(score);
        
        // Also update the main user data (for backward compatibility)
        FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(gameType)
                .setValue(score);
                
        // Also update the language-specific game data in user root
        FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(languageGameType)
                .setValue(score);
    }
    
    /**
     * For backward compatibility
     * @param gameType the type of game (e.g., "sing_song", "spell_word")
     * @param score the score achieved (0-100)
     */
    public static void recordGameScore(String gameType, int score) {
        // Default to Sinhala for backward compatibility
        recordGameScore(gameType, score, LANGUAGE_SINHALA);
    }
    
    /**
     * Record therapist communication
     * @param communicationType the type of communication (e.g., "message", "video", "voice_note")
     */
    public static void recordTherapistCommunication(String communicationType) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        String today = getTodayDateString();
        
        DatabaseReference communicationRef = FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(PROGRESS_HISTORY_PATH)
                .child(today)
                .child(THERAPIST_COMMUNICATION_PATH);
        
        // Increment the count for this communication type
        communicationRef.child(communicationType).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer count = task.getResult().getValue(Integer.class);
                count = (count == null) ? 1 : count + 1;
                communicationRef.child(communicationType).setValue(count);
                
                // Update total communication count
                communicationRef.child("total").get().addOnCompleteListener(totalTask -> {
                    if (totalTask.isSuccessful()) {
                        Integer totalCount = totalTask.getResult().getValue(Integer.class);
                        totalCount = (totalCount == null) ? 1 : totalCount + 1;
                        communicationRef.child("total").setValue(totalCount);
                        
                        // Update communication_count field for the chart
                        FirebaseDatabase.getInstance()
                                .getReference(USER_PATH)
                                .child(userId)
                                .child(PROGRESS_HISTORY_PATH)
                                .child(today)
                                .child("communication_count")
                                .setValue(totalCount);
                    }
                });
            }
        });
    }
    
    /**
     * Initialize daily data if not already present
     * Creates empty records for today to ensure data consistency
     */
    public static void initializeDailyData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        String today = getTodayDateString();
        
        DatabaseReference todayRef = FirebaseDatabase.getInstance()
                .getReference(USER_PATH)
                .child(userId)
                .child(PROGRESS_HISTORY_PATH)
                .child(today);
        
        todayRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // If today's data doesn't exist, initialize it
                if (!task.getResult().exists()) {
                    Map<String, Object> initialData = new HashMap<>();
                    
                    // Initialize Sinhala pronunciation attempts with zero values
                    Map<String, Object> sinhalaData = new HashMap<>();
                    sinhalaData.put("total", 0);
                    sinhalaData.put("correct", 0);
                    
                    // Initialize English pronunciation attempts with zero values
                    Map<String, Object> englishData = new HashMap<>();
                    englishData.put("total", 0);
                    englishData.put("correct", 0);
                    
                    // Create language structure for pronunciation attempts
                    Map<String, Object> pronunciationData = new HashMap<>();
                    pronunciationData.put(LANGUAGE_SINHALA, sinhalaData);
                    pronunciationData.put(LANGUAGE_ENGLISH, englishData);
                    
                    initialData.put(PRONUNCIATION_ATTEMPTS_PATH, pronunciationData);
                    
                    // Initialize game scores with empty object
                    initialData.put(GAME_SCORES_PATH, new HashMap<>());
                    
                    // Initialize communication with empty object and zero total
                    Map<String, Object> communicationData = new HashMap<>();
                    communicationData.put("total", 0);
                    initialData.put(THERAPIST_COMMUNICATION_PATH, communicationData);
                    
                    // Initialize with zero values for charts
                    initialData.put("speech_accuracy", 0);
                    initialData.put("speech_accuracy_sinhala", 0);
                    initialData.put("speech_accuracy_english", 0);
                    initialData.put("communication_count", 0);
                    
                    // Save the initial data
                    todayRef.updateChildren(initialData);
                }
            }
        });
    }
} 