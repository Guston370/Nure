# Implementation Plan - Dark / Light Theme System Across Nure

## Existing Theme Architecture Analysis

1. **Application & DarkModeManager**:
   - `HealthScannerApplication` calls `DarkModeManager.getInstance(this).applyUserPreference()` at app startup.
   - `DarkModeManager` stores preference in `SharedPreferences` (`"HealthScannerPrefs"`, key `"dark_mode_enabled"`).
   - `DarkModeManager` contains `toggleDarkMode(boolean)` and `applyUserPreference()`, using `AppCompatDelegate.setDefaultNightMode(...)`.

2. **SettingsActivity Disconnect**:
   - Currently, `SettingsActivity` stores theme toggle setting under a separate preference file (`"HealthScannerSettings"`, key `"dark_mode_enabled"`) and only shows a Toast without delegating to `DarkModeManager` or updating `AppCompatDelegate`.

3. **Theme & Resource Setup**:
   - `Base.Theme.HealthScanner` extends `Theme.Material3.DayNight` in `values/themes.xml` and `Theme.Material3.DayNight.NoActionBar` in `values-night/themes.xml`.
   - `values/colors.xml` and `values-night/colors.xml` contain color definitions, with `adaptive_*` and `bottom_nav_*` color aliases.
   - Some color keys (such as `card_background_light`, `background_light`, `text_primary`, `text_secondary`, `health_text_primary`, `health_text_secondary`, `nutrition_card_background`, `divider_light`) are hardcoded to light values in `values/colors.xml` without overrides in `values-night/colors.xml`.

4. **Hardcoded UI Colors in Layouts & Drawables**:
   - Background drawables like `home_bg_clean.xml`, `bg_settings_group_card.xml`, `bg_recent_scan_card.xml`, `bg_notification_circle.xml`, `bg_stat_chip.xml`, `bg_search_bar.xml`, `bg_filter_button.xml`, `bg_tip_card_clean.xml`, `bg_sidebar_panel.xml` have fixed hex colors `#FFFFFF`, `#FAFBF9`, `#F2F3F0`, etc.
   - XML layouts (`activity_home_enhanced.xml`, `activity_settings_enhanced.xml`, `activity_profile_modern.xml`, `activity_history_enhanced.xml`, `activity_analytics_enhanced.xml`, `bottom_nav_classy.xml`, item view layouts) use hardcoded text colors `#1B1B1B`, `#000000`, `#999B96` and icon tints `#1B1B1B`.

---

## Target Architecture & Proposed Changes

### 1. Persistence & Theme Controller (Single Source of Truth)
- Standardize `SettingsActivity` to use `DarkModeManager` directly.
- `SettingsActivity.saveDarkModeSetting()` will invoke `DarkModeManager.getInstance(this).toggleDarkMode(isChecked)`.
- `SettingsActivity.loadSettings()` will read state from `DarkModeManager.getInstance(this).isDarkModeEnabled()`.
- Ensure `DarkModeManager` updates `AppCompatDelegate.setDefaultNightMode(isDarkMode ? MODE_NIGHT_YES : MODE_NIGHT_NO)` and persists to `"HealthScannerPrefs"`.

### 2. Resource Color System (Values & Values-Night)
- Add dark-mode color overrides in `values-night/colors.xml` for all theme color aliases:
  - `card_background_light` -> `@color/md_theme_dark_surface` (`#1E2328`)
  - `background_light` -> `@color/md_theme_dark_background` (`#0F1419`)
  - `text_primary` / `health_text_primary` -> `@color/md_theme_dark_onBackground` (`#E1E2E8`)
  - `text_secondary` / `health_text_secondary` -> `@color/md_theme_dark_onSurfaceVariant` (`#C4C7CF`)
  - `nutrition_card_background` -> `@color/md_theme_dark_surfaceContainer` (`#1E2328`)
  - `divider_light` -> `@color/md_theme_dark_outline` (`#424242`)
- Update `styles.xml` to use dynamic theme attributes (e.g. `?attr/colorPrimary`, `?attr/colorSurface`, `?attr/colorOnBackground`, `?attr/colorOnSurfaceVariant`) for buttons, text fields, cards, and bottom navigation.

### 3. Dark Drawable Variants (`res/drawable-night/`)
Create dark resource equivalents in `res/drawable-night/`:
- `home_bg_clean.xml` (solid: `@color/md_theme_dark_background`)
- `bg_settings_group_card.xml` (solid: `@color/md_theme_dark_surface`, stroke: `@color/md_theme_dark_outline`)
- `bg_recent_scan_card.xml` (solid: `@color/md_theme_dark_surface`)
- `bg_notification_circle.xml` (solid: `@color/md_theme_dark_surfaceVariant`)
- `bg_profile_circle.xml` (solid: `@color/md_theme_dark_surfaceVariant`)
- `bg_stat_chip.xml` (solid: `@color/md_theme_dark_surface`)
- `bg_search_bar.xml` (solid: `@color/md_theme_dark_surfaceVariant`)
- `bg_filter_button.xml` (solid: `@color/md_theme_dark_surfaceVariant`)
- `bg_tip_card_clean.xml` (solid: `@color/md_theme_dark_surfaceContainer`)
- `bg_sidebar_panel.xml` (solid: `@color/md_theme_dark_surface`)
- `bg_sidebar_menu_item.xml` (solid: `@color/md_theme_dark_surfaceVariant`)

### 4. Layout Hardcoded Color Replacements
- Replace hardcoded black/dark text `#1B1B1B`, `#000000` with `@color/text_primary` or `?attr/colorOnBackground` in layout files (`activity_home_enhanced.xml`, `activity_settings_enhanced.xml`, `activity_profile_modern.xml`, `activity_history_enhanced.xml`, `activity_analytics_enhanced.xml`, `activity_product_details_enhanced.xml`, `bottom_nav_classy.xml`, `item_recent_scan_enhanced.xml`, `item_history_enhanced.xml`).
- Replace hardcoded secondary text `#999B96` with `@color/text_secondary` or `?attr/colorOnSurfaceVariant`.
- Update icon tints `#1B1B1B` to `@color/text_primary` or `?attr/colorOnBackground`.
- Update `bottom_nav_classy.xml` `app:cardBackgroundColor` to `@color/bottom_nav_background`.

### 5. Bottom Navigation & Scanner Screen
- `BaseActivity.java`: Update `highlightCurrentTab()` to retrieve inactive icon tint dynamically from theme attributes/colors (`@color/bottom_nav_icon_inactive`) instead of hardcoding `0xFF9E9E9E`.
- `VerticalScannerActivity.java`: Preserve camera dark overlay and ensure all control icons/text maintain readable contrast.

---

## Files to Modify / Create

1. **Java Files**:
   - `app/src/main/java/com/example/healthscanner/DarkModeManager.java` (Ensure correct initial default & method signatures)
   - `app/src/main/java/com/example/healthscanner/SettingsActivity.java` (Connect dark mode switch directly to `DarkModeManager`)
   - `app/src/main/java/com/example/healthscanner/BaseActivity.java` (Ensure theme-aware icon tinting in bottom nav)

2. **Resource Files (Values)**:
   - `app/src/main/res/values/colors.xml`
   - `app/src/main/res/values-night/colors.xml`
   - `app/src/main/res/values/styles.xml`
   - `app/src/main/res/values/themes.xml`
   - `app/src/main/res/values-night/themes.xml`

3. **Drawable Resources (New/Modified in `res/drawable-night/`)**:
   - `app/src/main/res/drawable-night/home_bg_clean.xml`
   - `app/src/main/res/drawable-night/bg_settings_group_card.xml`
   - `app/src/main/res/drawable-night/bg_recent_scan_card.xml`
   - `app/src/main/res/drawable-night/bg_notification_circle.xml`
   - `app/src/main/res/drawable-night/bg_profile_circle.xml`
   - `app/src/main/res/drawable-night/bg_stat_chip.xml`
   - `app/src/main/res/drawable-night/bg_search_bar.xml`
   - `app/src/main/res/drawable-night/bg_filter_button.xml`
   - `app/src/main/res/drawable-night/bg_tip_card_clean.xml`
   - `app/src/main/res/drawable-night/bg_sidebar_panel.xml`
   - `app/src/main/res/drawable-night/bg_sidebar_menu_item.xml`

4. **Layout XML Files**:
   - `app/src/main/res/layout/bottom_nav_classy.xml`
   - `app/src/main/res/layout/activity_settings_enhanced.xml`
   - `app/src/main/res/layout/activity_home_enhanced.xml`
   - `app/src/main/res/layout/activity_profile_modern.xml`
   - `app/src/main/res/layout/activity_profile_enhanced.xml`
   - `app/src/main/res/layout/activity_history_enhanced.xml`
   - `app/src/main/res/layout/activity_analytics_enhanced.xml`
   - `app/src/main/res/layout/activity_product_details_enhanced.xml`
   - `app/src/main/res/layout/item_recent_scan_enhanced.xml`
   - `app/src/main/res/layout/item_history_enhanced.xml`

---

## Verification Plan

### Automated Verification
- Run `./gradlew assembleDebug` to verify project compiles clean with zero errors.

### Manual / UI Verification
- Toggle theme from Settings: Light -> Dark and Dark -> Light.
- Confirm immediate theme transition across Home, Scanner, Profile, History, Analytics, Settings, Product Details.
- App restart persistence check: Set Dark Mode, restart app, verify dark theme persists.
