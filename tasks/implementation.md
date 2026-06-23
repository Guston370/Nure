# Nure Home Screen UI Redesign – Implementation Plan

## Goal
Transform the Nure home screen into a premium dark health-tech dashboard comparable
to Yuka, Samsung Health, Fitbit, and Apple Health, while preserving ALL existing
functionality, IDs, click listeners, navigation, and backend integrations.

---

## Design System
- **Theme**: Dark (Deep Navy → Dark Graphite)
- **Background**: `#080F1A` → `#0D1526` gradient
- **Surface / Glass cards**: `rgba(255,255,255,0.05)` with 1dp white-12% border
- **Primary accent**: Emerald `#10B981` → Mint `#34D399`
- **Secondary accent**: Cyan `#06B6D4`
- **Text primary**: `#FFFFFF`
- **Text secondary**: `#94A3B8`
- **Text hint**: `#64748B`
- **Danger / warning**: `#F59E0B`

---

## Files Modified

### Layout Files
- `activity_home_enhanced.xml` — Full dark-theme premium dashboard redesign
- `item_recent_scan_enhanced.xml` — Dark glassmorphism product card

### Drawables (new/updated)
- `home_bg_clean.xml` — Dark navy background
- `bg_stat_chip.xml` — Glass stat card
- `bg_scan_button_pill.xml` — Emerald gradient CTA
- `bg_search_bar.xml` — Glass pill search bar
- `bg_tip_card_clean.xml` — Glass tip card
- `bg_glass_card.xml` — [NEW] Generic glassmorphism card
- `bg_insight_card.xml` — [NEW] Health insights card
- `bg_scan_cta_gradient.xml` — [NEW] Emerald→Teal gradient for scan button
- `bg_health_score_badge.xml` — [NEW] Health score badge on scan cards
- `bg_profile_circle.xml` — Updated dark glass circle
- `bg_notification_circle.xml` — Updated dark glass circle

### Java
- `MainActivity.java` — Add health insights text generation method

---

## Sections Redesigned
1. **Header** — Dark background, large greeting, glassmorphism avatar + bell
2. **Search bar** — Glass pill, updated placeholder, teal search icon
3. **Stats row** — Three glass metric chips with colored number accents
4. **Scan CTA** — Emerald gradient pill button, dominant visual
5. **Recent Scans** — Dark glass cards with health score badge, gradient image area
6. **Health Insights** — New section with personalized AI-style insight text
7. **Daily Tip** — Compact glass card with rotating health tips

---

## Steps

- [x] Create this implementation plan
- [ ] Update drawable resources (backgrounds, cards, buttons)
- [ ] Redesign `activity_home_enhanced.xml`
- [ ] Redesign `item_recent_scan_enhanced.xml`
- [ ] Update `MainActivity.java` for insights section
- [ ] Verify build compiles successfully
