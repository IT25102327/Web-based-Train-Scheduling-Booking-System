# Get the current date and time and format it.
$current_time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Set the commit message using the formatted date.
$commit_message = "Update - $current_time"

# Add all changes to the staging area.
git add .


# Commit with the automatically generated message.
git commit -m "Setup Data tranfer objects"

# Push the changes to the 'IT25102327' branch of the 'origin' remote.
git push -u origin IT25102327

Write-Host "✅ Changes have been added, committed, and pushed successfully with message: $commit_message"