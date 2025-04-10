@echo off

::Ensure using windows backslashes
set OUTFILE=%OUTFILE:/=\%
set RUNFILE_LIB_PATH=%RUNFILE_LIB_PATH:/=\%
set BATLAUNCHER_PATH=%BATLAUNCHER_PATH:/=\%

::Make launcher template by combining launcher.bat and runfileslib.bat
echo @echo off > %OUTFILE%
echo goto launcher_start >> %OUTFILE%
echo rem =========START EMBEDDED RUNFILES_LIB===================== >> %OUTFILE%
type %RUNFILE_LIB_PATH% >> %OUTFILE%
echo rem =========END EMBEDDED RUNFILES_LIB===================== >> %OUTFILE%
echo:  >> %OUTFILE%
echo:  >> %OUTFILE%
type %BATLAUNCHER_PATH% >> %OUTFILE%