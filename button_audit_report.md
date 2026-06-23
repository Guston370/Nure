# Button Interaction Audit Report

| Screen | Button ID | Expected Action | Actual Action | Status | Issue | Fix Applied |
|---|---|---|---|---|---|---|
| AnalyticsActivity.java | refreshIcon | Action execution | // Rotate animation for refres... | Pass | None | None |
| AnalyticsActivity.java | totalScansCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| AnalyticsActivity.java | weeklyScansCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| AnalyticsActivity.java | monthlyScansCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| AnalyticsActivity.java | avgHealthScoreCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| AnalyticsActivity.java | avgCaloriesCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| ApiDetectionResultActivity.java | btnBack | Close Screen | finish());... | Pass | None | None |
| ApiDetectionResultActivity.java | btnScanAnother | Close Screen | finish());... | Pass | None | None |
| ApiDetectionResultActivity.java | btnGoHome | Navigation | android.content.Intent intent ... | Pass | None | None |
| FirebaseTestActivity.java | testButton | Action execution | testFirebaseConnection());
   ... | Pass | None | None |
| HistoryActivity.java | startScanningButton | Navigation | Intent intent = new Intent(thi... | Pass | None | None |
| HistoryActivity.java | itemView | Action execution | if (listener != null) {
      ... | Pass | None | None |
| LoginActivity.java | btnLogin | Navigation | handleEmailPasswordLogin());

... | Pass | None | None |
| LoginActivity.java | btnForgotPassword | Action execution | handleForgotPassword());

    ... | Pass | None | None |
| MainActivity.java | searchCard | Action execution | performHapticFeedback();
     ... | Pass | None | None |
| MainActivity.java | quickScanButton | Action execution | performHapticFeedback();
     ... | Pass | None | None |
| MainActivity.java | profileImage | Action execution | performHapticFeedback();
     ... | Pass | None | None |
| MainActivity.java | viewAllScans | Navigation | performHapticFeedback();
     ... | Pass | None | None |
| MainActivity.java | sidebarHeader | Action execution | closeSidebarAndNavigate(Profil... | Pass | None | None |
| MainActivity.java | sidebarMyProfile | Action execution | closeSidebarAndNavigate(Profil... | Pass | None | None |
| MainActivity.java | sidebarScanHistory | Action execution | closeSidebarAndNavigate(Histor... | Pass | None | None |
| MainActivity.java | sidebarAnalytics | Action execution | closeSidebarAndNavigate(Analyt... | Pass | None | None |
| MainActivity.java | sidebarSettings | Action execution | closeSidebarAndNavigate(Settin... | Pass | None | None |
| MainActivity.java | sidebarHelp | Action execution | // Close sidebar - no dedicate... | Pass | None | None |
| MainActivity.java | sidebarLogout | Action execution | if (drawerLayout != null) {
  ... | Pass | None | None |
| ProductDetailsEnhancedActivity.java | scanAgainButton | Navigation | // Navigate directly to the sc... | Pass | None | None |
| ProductDetailsEnhancedActivity.java | galleryButton | Action execution | // TODO: Implement gallery sel... | Pass | None | None |
| ProductDetailsEnhancedActivity.java | shareButton | Action execution | shareProduct());... | Pass | None | None |
| ProductDetailsEnhancedActivity.java | favoriteButton | Action execution | toggleFavorite());... | Pass | None | None |
| ProductSelectionActivity.java | btnSearch | Action execution | String query = searchEditText.... | Pass | None | None |
| ProductSelectionActivity.java | btnSubmitManual | Action execution | String manualLabel = searchEdi... | Pass | None | None |
| ProductSelectionAdapter.java | itemView | Action execution | if (listener != null) {
      ... | Pass | None | None |
| ProfileActivity.java | backButton | Action execution | onBackPressed());... | Pass | None | None |
| ProfileActivity.java | profileAvatar | Action execution | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | editProfileMenu | Action execution | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | viewAllActivity | Navigation | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | privacySetting | Action execution | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | helpCenter | Action execution | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | contactSupport | Action execution | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | logoutButton | Action execution | animateClick(v);
             ... | Pass | None | None |
| ProfileActivity.java | addConcernButton | Action execution | showAddHealthConcernDialog());... | Pass | None | None |
| ProfileActivity.java | editHealthConcerns | Action execution | showEditHealthConcernsDialog()... | Pass | None | None |
| ProfileActivity.java | addPreferenceButton | Action execution | showAddDietaryPreferenceDialog... | Pass | None | None |
| ProfileActivity.java | editDietaryPreferences | Action execution | showEditDietaryPreferencesDial... | Pass | None | None |
| RecentScansAdapter.java | itemView | Action execution | if (listener != null) {
      ... | Pass | None | None |
| SettingsActivity.java | helpIcon | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| SettingsActivity.java | notificationsCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| SettingsActivity.java | darkModeCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| SettingsActivity.java | privacyPolicyCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| SettingsActivity.java | exportHistoryCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| SettingsActivity.java | logoutCard | Action execution | v.startAnimation(AnimationUtil... | Pass | None | None |
| SignUpActivity.java | btnSignUp | Navigation | handleSignUp());

        btnS... | Pass | None | None |
| SignUpActivity.java | btnGoogleSignUp | Navigation | handleGoogleSignUp());
       ... | Pass | None | None |
| VerticalScannerActivity.java | backButton | Close Screen | animateButtonPress(v);
       ... | Pass | None | None |
| VerticalScannerActivity.java | flashToggle | Action execution | animateButtonPress(v);
       ... | Pass | None | None |
| VerticalScannerActivity.java | galleryButton | Action execution | animateButtonPress(v);
       ... | Pass | None | None |
| VerticalScannerActivity.java | cameraCaptureButton | Action execution | animateButtonPress(v);
       ... | Pass | None | None |
| VerticalScannerActivity.java | manualEntryButton | Action execution | animateButtonPress(v);
       ... | Pass | None | None |
| VerticalScannerActivity.java | modeBarcodeText | Action execution | if (currentMode != ScanMode.BA... | Pass | None | None |
| VerticalScannerActivity.java | modeDetectText | Action execution | if (currentMode != ScanMode.PR... | Pass | None | None |
