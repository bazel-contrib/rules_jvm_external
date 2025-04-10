@echo off

:: Bazel Runfiles lookup library for Windows BATCH files
:: If using bat_binary, BAT_RUNFILES_LIB should be already set. If not, you can set it yoruself or use the path directly when using 'call'.
:: Functions:
::   - initialize <binaryName>: 
::      Initializes runfileslib and sets runfile envvars. Top level binary needs to call initialize (but this is done by the launcher if using bat_binary).
::      args: %1 = name of the binary (used to determine runfiles directory).
::      example: call %BAT_RUNFILES_LIB% initialize %~0
::   - rlocation <Varname> <rlocationpath>:
::      Looks up the rlocationpath and sets the Varname Variable to the actual path. The path returned will have windows backslashes '\'. 
::      args: %1 = Variable Name where the result of the lookup will go. It will have the actual path for the rpathlocation.
::            %2 = THe rlocation path that is to be looked up. This should be created using the Bazel make variable $(rlocationpath).
::      example: 
::         call %BAT_RUNFILES_LIB% rlocation ActualPath %MyRlocationpath%
::         echo %MyRlocationpath% mapped to %ActualPath%
:: 
:: NOTE: repo_mapping functionality is not currently implemented. This is OK since this library is currently only used in rules_jvm_external which uses 
::     rpathlocation for all its paths.  repo_mapping should probably be added to this lib if it is ever used outside rules_jvm_external
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:runfiles_call

set function_call=%1
shift

if "%function_call%" == "rlocation" (
    goto runfiles_rlocation
) else if "%function_call%" == "initialize" (    
    goto runfiles_initialize
) else (
    echo "invalid runfiles function: %function_call%"
    exit /b 1
)

:::::::::::::::::::::::::::::::::::::::::::::::
:runfiles_initialize
::args: %1 = name of the binary (used to determine runfiles directory).
::example: call BAT_RUNFILES_LIB initialize %~0

rem Force MANFIEST_ONLY since Bazel doesn't have a way for a binary to know if runfiles are enabled or not (yet).
set RUNFILES_MANIFEST_ONLY=1

if not defined RUNFILES_DIR (    

    rem Assuming the runfiles directory is the name of the binary + ".runfiles"

    set RUNFILES_DIR=%1.runfiles
)
if not defined RUNFILES_MANIFEST_FILE (
    set RUNFILES_MANIFEST_FILE=%RUNFILES_DIR%\Manifest
)

exit /b 0
:::::::::::::::::::::::::::::::::::::::::::::::

:::::::::::::::::::::::::::::::::::::::::::::::
:runfiles_rlocation
::args: %1 = Variable Name where the result of the lookup will go. It will have the actual path for the rpathlocation.
::      %2 = THe rlocation path that is to be looked up. This should be created using the Bazel make variable $(rlocationpath).
::example: call BAT_RUNFILES_LIB rlocation ACTUAL_PATH %MYRLOCATIONPATH%

setlocal enableDelayedExpansion    

set result=

if "%RUNFILES_MANIFEST_ONLY%" == "1" (    
    if "%RUNFILES_MANIFEST_FILE%" == "" (
        echo ERROR: rlocation failed. RUNFILES_MANIFEST_FILE is not set. Make sure runfiles are initialized by calling the initialize function.
        exit /b 1 
    )

    for /f "tokens=1,2" %%a in ( %RUNFILES_MANIFEST_FILE% ) do (
        if "%%a" == "%2" (
            set result=%%b
            goto runfiles_rlocation_processresult
        )
    )
) else (
    if "%RUNFILES_DIR%" == "" (
        echo ERROR: rlocation failed. RUNFILES_DIR is not set. Make sure runfiles are initialized by calling the initialize function.
        exit /b 1 
    )

    set result=%RUNFILES_DIR%/%2
)

:runfiles_rlocation_processresult

if "%result%" == "" (
  echo ERROR: rlocation failed on %2
  exit /b 1 
)
set "result=%result:/=\%"

endlocal & set %1=%result%
exit /b 0
:::::::::::::::::::::::::::::::::::::::::::::::


