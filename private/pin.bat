::IMPORTANT: Keep functionality in this Bat in sync with the bash version (pin.sh)

@echo off
call %BAT_RUNFILES_LIB% rlocation maven_unsorted_file %1 || goto eof

set maven_install_json_loc=%BUILD_WORKSPACE_DIRECTORY%\{maven_install_location}

copy /y "%maven_unsorted_file%" "%maven_install_json_loc%" || goto eof

if  "{predefined_maven_install}" == "True" (
{success_msg_pinned}
) else (
{success_msg_unpinned}
)
echo:

:eof