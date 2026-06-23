import re
import os

filepath = r"e:\HealthScanner\Nure\app\src\main\res\layout\activity_home_enhanced.xml"
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
    
matches = re.findall(r'android:id="@+id/([^"]+)"', content)
print(f"Total IDs found: {len(matches)}")
print("IDs:", matches)
if "profileImage" in matches:
    print("profileImage IS found!")
else:
    print("profileImage IS NOT found!")
