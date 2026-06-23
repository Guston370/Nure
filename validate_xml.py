import os
import xml.etree.ElementTree as ET

res_dir = r'e:\HealthScanner\Nure\app\src\main\res'
errors = []
count = 0

for root_dir, dirs, files in os.walk(res_dir):
    for f in files:
        if f.endswith('.xml'):
            count += 1
            path = os.path.join(root_dir, f)
            try:
                ET.parse(path)
            except ET.ParseError as e:
                errors.append(f'{path}: {e}')

if errors:
    print('XML ERRORS FOUND:')
    for e in errors:
        print(e)
else:
    print(f'All {count} XML files are well-formed!')
