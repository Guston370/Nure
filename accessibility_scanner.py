import os
import xml.etree.ElementTree as ET

layout_dir = r"e:\HealthScanner\Nure\app\src\main\res\layout"

touch_target_issues = []
contrast_issues = []
icon_issues = []

clickable_tags = {'Button', 'ImageButton', 'ImageView', 'com.google.android.material.button.MaterialButton', 'com.google.android.material.card.MaterialCardView', 'LinearLayout', 'RelativeLayout'}

def extract_dp(val):
    if not val:
        return 0
    if val.endswith('dp'):
        try:
            return float(val[:-2])
        except:
            return 0
    return 0

for filename in os.listdir(layout_dir):
    if not filename.endswith('.xml'):
        continue
    filepath = os.path.join(layout_dir, filename)
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        
        for elem in root.iter():
            tag = elem.tag.split('.')[-1]
            
            # Check touch targets for explicitly clickable elements or buttons
            is_clickable = elem.get('{http://schemas.android.com/apk/res/android}clickable') == 'true'
            if is_clickable or 'Button' in tag:
                w = elem.get('{http://schemas.android.com/apk/res/android}layout_width')
                h = elem.get('{http://schemas.android.com/apk/res/android}layout_height')
                w_dp = extract_dp(w)
                h_dp = extract_dp(h)
                
                if (w_dp > 0 and w_dp < 48) or (h_dp > 0 and h_dp < 48):
                    id_attr = elem.get('{http://schemas.android.com/apk/res/android}id', 'unknown_id')
                    touch_target_issues.append(f"[{filename}] {id_attr}: Touch target too small ({w} x {h})")

            # Check for icons missing tint or having very low opacity
            if 'ImageView' in tag:
                src = elem.get('{http://schemas.android.com/apk/res/android}src')
                tint = elem.get('{http://schemas.android.com/apk/res-auto}tint')
                if src and not tint:
                    id_attr = elem.get('{http://schemas.android.com/apk/res/android}id', 'unknown_id')
                    icon_issues.append(f"[{filename}] {id_attr}: ImageView has src but missing tint, may be invisible in some themes.")
            
            # Check text colors
            if 'TextView' in tag or 'EditText' in tag:
                text_color = elem.get('{http://schemas.android.com/apk/res/android}textColor')
                if text_color:
                    # simplistic check for low alpha or hardcoded light colors
                    if text_color.startswith('#') and len(text_color) == 9:
                        alpha = int(text_color[1:3], 16)
                        if alpha < 128: # Less than 50% opacity
                            id_attr = elem.get('{http://schemas.android.com/apk/res/android}id', 'unknown_id')
                            contrast_issues.append(f"[{filename}] {id_attr}: Text has low opacity ({text_color})")

    except Exception as e:
        print(f"Error parsing {filename}: {e}")

with open("accessibility_report_raw.txt", "w") as f:
    f.write("=== Touch Target Issues (< 48dp) ===\n")
    for i in touch_target_issues:
        f.write(i + "\n")
    f.write("\n=== Contrast/Opacity Issues ===\n")
    for i in contrast_issues:
        f.write(i + "\n")
    f.write("\n=== Icon Issues (Missing Tint) ===\n")
    for i in icon_issues:
        f.write(i + "\n")

print(f"Found {len(touch_target_issues)} touch issues, {len(contrast_issues)} contrast issues, {len(icon_issues)} icon issues.")
