import os

def aggregate_files(output_filename="all_code_output.txt"):
    # Define the specific folders and files you want to capture
    target_folders = ['Premium_Calculation', 'Trigger_System']
    target_files = ['requirements.txt']
    
    with open(output_filename, 'w', encoding='utf-8') as outfile:
        # 1. Process target folders for .py files
        for folder in target_folders:
            if os.path.exists(folder):
                outfile.write(f"{'='*50}\n")
                outfile.write(f"DIRECTORY: {folder}\n")
                outfile.write(f"{'='*50}\n\n")
                
                for root, dirs, files in os.walk(folder):
                    # Skip __pycache__ folders
                    if '__pycache__' in dirs:
                        dirs.remove('__pycache__')
                        
                    for file in files:
                        if file.endswith('.py'):
                            file_path = os.path.join(root, file)
                            write_file_to_out(file_path, outfile)
            else:
                print(f"Warning: Folder '{folder}' not found.")

        # 2. Process specific individual files
        for file_name in target_files:
            if os.path.exists(file_name):
                write_file_to_out(file_name, outfile)
            else:
                print(f"Warning: File '{file_name}' not found.")

    print(f"Successfully created {output_filename}")

def write_file_to_out(file_path, outfile):
    try:
        with open(file_path, 'r', encoding='utf-8') as infile:
            outfile.write(f"--- START OF FILE: {file_path} ---\n")
            outfile.write(infile.read())
            outfile.write(f"\n--- END OF FILE: {file_path} ---\n\n")
    except Exception as e:
        outfile.write(f"Error reading {file_path}: {e}\n\n")

if __name__ == "__main__":
    aggregate_files()