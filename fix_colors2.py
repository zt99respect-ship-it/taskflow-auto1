import os
import glob
import re

replacements = {
    r'Color\(0xFF111827\)': 'MaterialTheme.colorScheme.surface',
    r'Color\(0xFF1F2937\)': 'MaterialTheme.colorScheme.surfaceVariant',
    r'Color\(0xFFF87171\)': 'TerminalRed'
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

