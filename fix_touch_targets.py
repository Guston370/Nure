import os
import re

layout_dir = r"e:\HealthScanner\Nure\app\src\main\res\layout"

files_to_fix_44 = [
    "activity_analytics_enhanced.xml",
    "activity_history_enhanced.xml",
    "activity_profile_enhanced.xml",
    "activity_profile_modern.xml",
    "activity_settings_enhanced.xml"
]

for filename in files_to_fix_44:
    filepath = os.path.join(layout_dir, filename)
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Replace 44dp with 48dp for layout_width and layout_height
        content = re.sub(r'layout_width="44dp"', 'layout_width="48dp"', content)
        content = re.sub(r'layout_height="44dp"', 'layout_height="48dp"', content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
            
# Special fix for 28dp in activity_profile_modern.xml
profile_modern = os.path.join(layout_dir, "activity_profile_modern.xml")
if os.path.exists(profile_modern):
    with open(profile_modern, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We want to increase the touch target without making the icon gigantic.
    # If they are ImageView, we can add padding="10dp" and increase size to 48dp.
    # Let's find editHealthConcerns and editDietaryPreferences
    
    def expand_28dp(match):
        block = match.group(0)
        block = block.replace('layout_width="28dp"', 'layout_width="48dp"')
        block = block.replace('layout_height="28dp"', 'layout_height="48dp"')
        if 'padding' not in block:
            block = block.replace('android:layout_width="48dp"', 'android:layout_width="48dp"\n                                android:padding="10dp"')
        return block
        
    content = re.sub(r'<ImageView[^>]+android:id="@+id/editHealthConcerns"[^>]+/>', expand_28dp, content)
    content = re.sub(r'<ImageView[^>]+android:id="@+id/editDietaryPreferences"[^>]+/>', expand_28dp, content)

    with open(profile_modern, 'w', encoding='utf-8') as f:
        f.write(content)

print("Touch targets updated.")
