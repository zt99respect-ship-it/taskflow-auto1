import os
import glob
import re

replacements = {
    r'Color\(0xFF0B0F19\)': 'MaterialTheme.colorScheme.background',
    r'Color\(0xFF1E293B\)': 'MaterialTheme.colorScheme.surfaceVariant',
    r'Color\(0xFF334155\)': 'MaterialTheme.colorScheme.outline',
    r'Color\(0xFF94A3B8\)': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'Color\(0xFF0F172A\)': 'MaterialTheme.colorScheme.surface',
    r'Color\(0xFF151D2C\)': 'MaterialTheme.colorScheme.surface',
    r'Color\(0xFF243044\)': 'MaterialTheme.colorScheme.outline',
    r'Color\(0xFF6B7280\)': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'Color\(0xFF475569\)': 'MaterialTheme.colorScheme.outline'
}

files = glob.glob('app/src/main/java/com/example/ui/**/*.kt', recursive=True)
for file_path in files:
    if 'theme' in file_path: continue
    
    with open(file_path, 'r') as f:
        content = f.read()
        
    original = content
    for old, new in replacements.items():
        content = re.sub(old, new, content)
        
    if content != original:
        with open(file_path, 'w') as f:
            f.write(content)
        print(f'Updated {file_path}')

