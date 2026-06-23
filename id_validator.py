import os
import re

java_dir = r"e:\HealthScanner\Nure\app\src\main\java\com\example\healthscanner"
layout_dir = r"e:\HealthScanner\Nure\app\src\main\res\layout"

def get_layout_ids(layout_name):
    # Search for android:id="@+id/XXXX" in the layout and any <include layout="@layout/YYYY"/>
    ids = set()
    includes = set()
    
    filepath = os.path.join(layout_dir, f"{layout_name}.xml")
    if not os.path.exists(filepath):
        return ids
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
        # Find all IDs
        matches = re.findall(r'android:id="@+id/([^"]+)"', content)
        ids.update(matches)
        
        # Find all includes
        inc_matches = re.findall(r'layout="@layout/([^"]+)"', content)
        for inc in inc_matches:
            # recursively get ids from include
            ids.update(get_layout_ids(inc))
            
    return ids

issues = []

for filename in os.listdir(java_dir):
    if filename.endswith(".java"):
        filepath = os.path.join(java_dir, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
            # Find setContentView(R.layout.XXXX)
            layout_match = re.search(r'setContentView\s*\(\s*R\.layout\.([a-zA-Z0-9_]+)\s*\)', content)
            if not layout_match:
                continue # Maybe fragment or BaseActivity
                
            layout_name = layout_match.group(1)
            layout_ids = get_layout_ids(layout_name)
            
            # Find all findViewById(R.id.XXXX)
            find_matches = re.findall(r'findViewById\s*\(\s*R\.id\.([a-zA-Z0-9_]+)\s*\)', content)
            for view_id in find_matches:
                if view_id not in layout_ids:
                    issues.append(f"[{filename}] looks for R.id.{view_id} but it's missing in layout {layout_name}")

with open("id_validation_report.txt", "w") as f:
    for issue in issues:
        f.write(issue + "\n")

print(f"Validation complete. Found {len(issues)} missing IDs.")
