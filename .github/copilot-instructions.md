The Nure (HealthScanner) repo — quick guidance for AI coding agents

Be concise, make minimal safe changes, and follow existing Material 3 patterns used in this app.

Why you're here
- This Android app uses Material 3, Firebase auth, and a small set of custom styles. Your job is to implement UI/UX changes (layouts, drawables, styles) without touching backend data structures or Firebase integration.

High-level architecture (what matters)
- App is a single Android app under `app/` following Material 3 theming (`res/values/themes.xml`, `res/values/colors.xml`).
- Activities live in `app/src/main/java/com/example/healthscanner/` (e.g. `ProfileActivity.java`, `MainActivity.java`).
- Layouts under `app/src/main/res/layout/`. The active profile screens are `activity_profile.xml` (legacy) and `activity_profile_enhanced.xml` (enhanced). Edit the enhanced file for new UI; keep the legacy file as a fallback.
- Common dimensions, strings and styles are in `res/values/{dimens.xml,strings.xml,styles.xml,colors.xml}`. Prefer adding styles to `styles.xml` and colors to `colors.xml` so themes pick them up.

Project-specific conventions
- Theme: Material 3 DayNight; use the md_theme_light_* and md_theme_dark_* color tokens defined in `res/values/colors.xml`.
- Primary/secondary colors are already Mint/Teal and Soft Blue. Use `@color/md_theme_light_primary` and `@color/md_theme_light_secondary` rather than hard-coding hex values.
- Card style: cards use 16dp radius and 4dp elevation across the app. Use existing `Widget.HealthScanner.CardView` styles where possible.
- Animations: layouts often have `android:animateLayoutChanges="true"`. Use simple fade-in animations for images/cards and ripple via `?attr/selectableItemBackground`.
- Scroll handling: use `NestedScrollView` with inner content set to `wrap_content`; avoid `match_parent` inside scrollable children to prevent Android measure crashes.
- Accessibility: add `contentDescription` for all ImageView/Icon views. Use placeholder text/images when Firebase data is null.

Key files & examples (jump-to)
- Profile activity: `app/src/main/java/com/example/healthscanner/ProfileActivity.java` — it calls `setContentView(R.layout.activity_profile)` in some versions. Enhanced layout: `app/src/main/res/layout/activity_profile_enhanced.xml` — prefer editing this.
- Main themes: `app/src/main/res/values/themes.xml` and `app/src/main/res/values/colors.xml` — add new color tokens here.
- Reusable components: styles in `app/src/main/res/values/styles.xml` and drawables under `res/drawable/`.

When editing the Profile UI (rules)
- Only change layout, styles, drawables, and icons. Don't change Java/Kotlin Firebase calls, data models, or database paths.
- Keep API/behavior: fields like user name and email come from `AuthManager` in `ProfileActivity.java`. Ensure `TextView` ids match existing code if code uses them (e.g. `user_name`, `user_email`) or update the activity accordingly if you must rename (prefer to keep ids).
- Provide crash-safety: guard views that display Firebase data with null checks in layout by using `tools:` attributes for preview; ensure `ProfileActivity` null-safely sets text (existing code already checks for nulls) and set default placeholder drawables (e.g. `@drawable/ic_person`).
- Use `ConstraintLayout` as the root and wrap body in `NestedScrollView` for larger screens.

Styling & assets guidance (quick)
- Colors: use `@color/gradient_primary_start` and `@color/gradient_primary_end` (see `res/values/colors.xml` for their hex values) for header gradients.
- Card radius & elevation: 16dp radius, 4dp elevation (use `app:cardCornerRadius="16dp"` and `app:cardElevation="4dp"` or use `MaterialCardView` with the shared style).
- Profile avatar: 100dp with 2dp stroke; prefer to use a `ShapeDrawable` or `drawable/profile_avatar_border.xml` for circular border and elevation.
- Icons: prefer outlined Material icons (vector XML). Name icons `ic_edit.xml`, `ic_stats.xml`, `ic_logout.xml`. Keep contentDescription set.

Common pitfalls to avoid
- Do not place `match_parent` children inside `ScrollView` for height — use `wrap_content` to avoid MeasureSpec exceptions.
- Don't hardcode strings; add them to `res/values/strings.xml` when they're visible to users (except short local-development placeholders).
- If modifying ids used by code (e.g. `logout_button`, `bottom_navigation`), update Java/Kotlin references or keep original ids.

Example snippets (what to follow)
- Header gradient: use a drawable referencing `@color/gradient_primary_start` → `@color/gradient_primary_end` and apply to the header container background.
- NestedScrollView inside ConstraintLayout: see `activity_profile_enhanced.xml` for pattern — keep `app:layout_constraintTop_toBottomOf="@+id/toolbar"` and `app:layout_constraintBottom_toTopOf="@+id/bottom_navigation"`.
- Stats cards: three equally weighted panels inside a horizontal `LinearLayout` with `layout_weight="1"` each; use mini progress bars (`View` with width matching percentage and 4dp height) if necessary.

When you finish
- Add/modify resources under `app/src/main/res/` only. Keep changes minimal and compile-safe.
- Run a quick XML lint: request the repo's errors via the build/lint pipeline (or ask the developer to run `./gradlew assembleDebug` locally). If you change ids used by Java/Kotlin, update code accordingly and ensure it compiles.

If something isn't discoverable
- If you need build credentials or to run the app on device, ask the repository owner — do not attempt to change Firebase configuration in `google-services.json`.

Questions to ask the maintainer
- Which layout is canonical: `activity_profile.xml` or `activity_profile_enhanced.xml`? I edited the enhanced file by default; confirm if you want the legacy layout replaced.
- Any brand assets (logo, exact avatar border) you prefer to use instead of placeholders?

End of file
