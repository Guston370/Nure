import os
import re
import xml.etree.ElementTree as ET

layout_dir = r"e:\HealthScanner\Nure\app\src\main\res\layout"
issues = []

def check_file(filepath):
    filename = os.path.basename(filepath)
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
    except Exception as e:
        issues.append(f"[{filename}] Failed to parse XML: {e}")
        return

    # Check for deep nesting
    def get_depth(node):
        if not list(node):
            return 1
        return 1 + max(get_depth(child) for child in node)

    depth = get_depth(root)
    if depth > 7:
        issues.append(f"[{filename}] Deep layout nesting (Depth: {depth}). Consider using ConstraintLayout.")

    # Iterate through all elements
    for elem in root.iter():
        tag = elem.tag.split('.')[-1]
        
        # Check hardcoded dimensions
        width = elem.attrib.get('{http://schemas.android.com/apk/res/android}layout_width', '')
        height = elem.attrib.get('{http://schemas.android.com/apk/res/android}layout_height', '')
        
        if re.match(r'^[1-9]\d*dp$', width) and tag not in ['ImageView', 'Space', 'View', 'CardView', 'com.google.android.material.imageview.ShapeableImageView']:
            issues.append(f"[{filename}] Hardcoded width ({width}) found in <{tag}>. Consider match_parent, wrap_content, or 0dp with constraints.")
            
        if re.match(r'^[1-9]\d*dp$', height) and tag not in ['ImageView', 'Space', 'View', 'CardView', 'com.google.android.material.imageview.ShapeableImageView', 'BottomNavigationView']:
            issues.append(f"[{filename}] Hardcoded height ({height}) found in <{tag}>. Consider match_parent, wrap_content, or 0dp with constraints.")

        # Check weights inside ScrollView
        if tag in ['ScrollView', 'NestedScrollView']:
            for child in elem.iter():
                weight = child.attrib.get('{http://schemas.android.com/apk/res/android}layout_weight', '')
                if weight:
                    issues.append(f"[{filename}] layout_weight found inside ScrollView in <{child.tag}>. This causes measurement issues.")

for f in os.listdir(layout_dir):
    if f.endswith('.xml'):
        check_file(os.path.join(layout_dir, f))

with open('ui_analysis_results.txt', 'w') as out:
    for issue in issues:
        out.write(issue + "\n")
print(f"UI Analysis complete. Found {len(issues)} potential issues.")
