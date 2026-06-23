import re
import xml.etree.ElementTree as ET

layout_path = r"e:\HealthScanner\Nure\app\src\main\res\layout\activity_home_enhanced.xml"
java_path = r"e:\HealthScanner\Nure\app\src\main\java\com\example\healthscanner\MainActivity.java"

tree = ET.parse(layout_path)
root = tree.getroot()

clickable_ids = []
for elem in root.iter():
    tag = elem.tag.split('.')[-1]
    is_clickable = elem.get('{http://schemas.android.com/apk/res/android}clickable') == 'true'
    is_btn = 'Button' in tag or 'MaterialCardView' in tag
    id_attr = elem.get('{http://schemas.android.com/apk/res/android}id')
    
    if id_attr and (is_clickable or is_btn):
        clickable_ids.append(id_attr.replace('@+id/', ''))

with open(java_path, 'r', encoding='utf-8') as f:
    java_content = f.read()

# BaseActivity handles the drawer header and bottom nav items usually
base_path = r"e:\HealthScanner\Nure\app\src\main\java\com\example\healthscanner\BaseActivity.java"
with open(base_path, 'r', encoding='utf-8') as f:
    base_content = f.read()

all_java = java_content + base_content

dead_buttons = []
for cid in clickable_ids:
    if f"R.id.{cid}" not in all_java:
        dead_buttons.append(cid)

print(f"Total clickable elements on Home Screen: {len(clickable_ids)}")
print(f"Dead buttons (no reference in MainActivity/BaseActivity): {len(dead_buttons)}")
for d in dead_buttons:
    print(f" - {d}")
