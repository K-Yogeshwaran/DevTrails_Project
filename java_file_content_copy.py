import os

# 🔧 Change this to your project root folder
PROJECT_PATH = r"E:\Practice repository\Projects\DevTrails_Project\backend\backend"

# 🔧 Output file
OUTPUT_FILE = "java_all_code_output.txt"

# File extensions you want to include
VALID_EXTENSIONS = [".java", ".xml", ".properties", ".yml", ".txt"]

def should_include(file):
    return any(file.endswith(ext) for ext in VALID_EXTENSIONS)

def copy_files_to_txt(project_path, output_file):
    with open(output_file, "w", encoding="utf-8") as out:
        for root, dirs, files in os.walk(project_path):
            for file in files:
                if should_include(file):
                    file_path = os.path.join(root, file)

                    try:
                        with open(file_path, "r", encoding="utf-8") as f:
                            content = f.read()

                        out.write("\n" + "="*80 + "\n")
                        out.write(f"FILE: {file_path}\n")
                        out.write("="*80 + "\n\n")
                        out.write(content + "\n\n")

                    except Exception as e:
                        print(f"❌ Error reading {file_path}: {e}")

    print(f"\n✅ All files copied to {output_file}")

# Run the function
copy_files_to_txt(PROJECT_PATH, OUTPUT_FILE)