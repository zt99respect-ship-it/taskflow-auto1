with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'else if (screen == Screen.Automation,' in line:
        lines[i] = '                                } else if (screen == Screen.Automation && activeScriptsCount > 0) {\n'
        lines[i+1] = '' # Clear the next line

with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'w') as f:
    f.writelines(lines)
