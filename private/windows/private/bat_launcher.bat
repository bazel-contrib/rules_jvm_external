@echo off

::Windows Bat Launcher
::This Bat launcher is needed since Bazel's win launcher doesn't currently support Bat files. 
::The main function is to initialize the Runfiles Mechanism:
::  -- The runfiles library is embedded into the launcher file, which is needed because locating the runfiles lib requires runfiles. Doing it this way makes it so the downstream bat files don't need to embed large runfiles fragment like Bash sh files do.
::  -- sets BAT_RUNFILES_LIB to the path of the runfiles lib so that downstream bat files will have correct path for Runfiles.

:launcher_start

set BAT_RUNFILES_LIB=:runfiles_call
 
call %BAT_RUNFILES_LIB% initialize %~0 || goto eof


call %BAT_RUNFILES_LIB% rlocation RUNFILES_LIB_PATH {runfiles_lib_rpath} || goto eof
call %BAT_RUNFILES_LIB% rlocation SCRIPT_PATH {script_rpath} || goto eof


::Use () block so that the vars will be expanded and we can unset local vars so the local vars don't get inherited by subscript.
(
    setlocal
    
    ::Unset local vars so subscript will not see them
    set SCRIPT_PATH=
    set RUNFILES_LIB_PATH=

    ::Setup the RUNFILES_LIB for the subscript
    set BAT_RUNFILES_LIB=call "%RUNFILES_LIB_PATH%"

    call "%SCRIPT_PATH%" %* || goto eof

    endlocal
)

:eof