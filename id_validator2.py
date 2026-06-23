import os
import re
import xml.etree.ElementTree as ET

java_dir = r"e:\HealthScanner\Nure\app\src\main\java\com\example\healthscanner"
layout_dir = r"e:\HealthScanner\Nure\app\src\main\res\layout"

def get_layout_ids(layout_name):
    ids = set()
    filepath = os.path.join(layout_dir, f"{layout_name}.xml")
    if not os.path.exists(filepath):
        return ids
    
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        for elem in root.iter():
            id_attr = elem.get('{http://schemas.android.com/apk/res/android}id')
            if id_attr and id_attr.startswith('@+id/'):
                ids.add(id_attr.replace('@+id/', ''))
        
        # Check includes
        for elem in root.iter('include'):
            layout_attr = elem.get('layout')
            if layout_attr and layout_attr.startswith('@layout/'):
                ids.update(get_layout_ids(layout_attr.replace('@layout/', '')))
    except Exception as e:
        print(f"Error parsing {filepath}: {e}")
        
    return ids

issues = []

for filename in os.listdir(java_dir):
    if filename.endswith(".java"):
        filepath = os.path.join(java_dir, filename)
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            
            layout_match = re.search(r'setContentView\s*\(\s*R\.layout\.([a-zA-Z0-9_]+)\s*\)', content)
            if not layout_match:
                continue
                
            layout_name = layout_match.group(1)
            layout_ids = get_layout_ids(layout_name)
            
            find_matches = re.findall(r'findViewById\s*\(\s*R\.id\.([a-zA-Z0-9_]+)\s*\)', content)
            for view_id in find_matches:
                if view_id not in layout_ids:
                    issues.append(f"[{filename}] looks for R.id.{view_id} but missing in {layout_name}")

with open("id_validation_report2.txt", "w") as f:
    for issue in issues:
        f.write(issue + "\n")

print(f"Validation complete. Found {len(issues)} missing IDs.")
