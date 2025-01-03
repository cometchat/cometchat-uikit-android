-- Get the list of file paths passed from Gradle
set filePaths to {}
try
    set filePaths to paragraphs of (do shell script "cat " & quoted form of "/Users/rohit/Rohit/CometChatWork/cometchat-sdks/android-uikit/scripts/fileList.txt")
on error errMsg
    display dialog errMsg
end try

tell application "Android Studio"
    activate
    -- delay 1

    repeat with filePath in filePaths
    -- Open each file by providing its full path
        open filePath
        -- delay 1

        -- Select all text (Cmd + A)
        tell application "System Events"
            keystroke "a" using {command down}  -- Select all text
            -- delay 0.5

            -- Perform option+command+L (3 times for formatting)
            keystroke "l" using {command down, option down}  -- Format the code (1st time)
            -- delay 0.5
            keystroke "l" using {command down, option down}  -- Format the code (2nd time)
            -- delay 0.5
            keystroke "l" using {command down, option down}  -- Format the code (3rd time)
            -- delay 0.5

            -- Perform control+option+O (Optimize imports)
            keystroke "o" using {control down, option down}  -- Optimize imports
            -- delay 1
        end tell

        -- Close the active editor window (targeting the correct window type)
        tell application "System Events"
            tell process "Android Studio"
                set windowList to windows
                repeat with aWindow in windowList
                -- Check if the window is an editor window (not a tool window)
                    try
                        if (name of aWindow) contains ".java" or (name of aWindow) contains ".xml" then
                        -- Close the window if it's an editor window
                            close aWindow
                            exit repeat
                        end if
                    end try
                end repeat
            end tell
        end tell

        -- delay 1
    end repeat
end tell
