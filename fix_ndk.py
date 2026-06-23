import os, shutil

base_ndk = r'C:\Users\adij7\AppData\Local\Android\Sdk\ndk'
src_dir = os.path.join(base_ndk, '27.0.12077973')
dst_dir = os.path.join(base_ndk, '25.0.2')

if os.path.islink(dst_dir) or os.path.exists(dst_dir):
    try:
        os.rmdir(dst_dir)
    except:
        shutil.rmtree(dst_dir, ignore_errors=True)

os.makedirs(dst_dir, exist_ok=True)

for item in os.listdir(src_dir):
    s = os.path.join(src_dir, item)
    d = os.path.join(dst_dir, item)
    if item == 'source.properties':
        with open(s, 'r') as f:
            content = f.read()
        content = content.replace('27.0.12077973', '25.0.2')
        with open(d, 'w') as f:
            f.write(content)
    elif os.path.isdir(s):
        os.system(f'mklink /J "{d}" "{s}"')
    else:
        shutil.copy2(s, d)

print('Fake NDK 25.0.2 created successfully')
