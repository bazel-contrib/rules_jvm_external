load("//private/lib:utils.bzl", "file_to_rlocationpath")

def _bat_binary_imp(ctx):
    return bat_binary_action(
        ctx = ctx,
        src = ctx.file.src,
        src_defaultinfo = ctx.attr.src[DefaultInfo],
        data_defaultinfos = [d[DefaultInfo] for d in ctx.attr.data],
    )
    
"""bat_binary_action: Can be directly by rules to create a bat_binary executable as an action"""
def bat_binary_action(
    ctx,
    src, #File
    src_defaultinfo = None, #DefaultInfo or None. for transient runfiles
    data_files = [], #Seq[File]
    data_defaultinfos = [], #Seq[DefaultInfo] (brings in transient runfiles)
):
    if(src.extension.upper() != "BAT"):
        fail("bat_binary src needs to be a *.bat file.")
    
    #Create bat launcher    
    launcher = ctx.actions.declare_file(ctx.label.name + "_launcher.bat")    

    ctx.actions.expand_template(
        template = ctx.file._launcher_template, 
        output = launcher,
        substitutions = {
            "{runfiles_lib_rpath}" : file_to_rlocationpath(ctx, ctx.file._runfiles_bat),
            "{script_rpath}" : file_to_rlocationpath(ctx, src),
        }
    )
  
    data_runfiles_list = [
        data_item.default_runfiles.merge(ctx.runfiles(data_item.files.to_list()))
        for data_item in data_defaultinfos
    ]

    if(src_defaultinfo != None):
        data_runfiles_list.append(src_defaultinfo.default_runfiles)

    return DefaultInfo(
        executable = launcher,
        runfiles = ctx.runfiles([src, ctx.file._runfiles_bat] + data_files).merge_all(data_runfiles_list)
    )

BAT_BINARY_IMPLICIT_ATTRS = {
    "_runfiles_bat": attr.label(allow_single_file=True, default="runfiles.bat"),
    "_launcher_template": attr.label(allow_single_file=True, default="bat_launcher_template"),
}


bat_binary = rule(
    implementation = _bat_binary_imp,
    doc = "bat_binary is used to declare executable bat scripts for windows. This rule ensures " + 
    "all dependencies are built and appear in the runfiles area during execution. \n\n" + 
    "This rule also sets up the BAT_RUNFILES_LIB env var that contains the path to the bat runfiles lib" +  
    "which can be used by the bat file to perform rlocation lookups. See comments in runfiles.bat for more info. \n" + 
    "Example: call %BAT_RUNFILES_LIB% rlocation ACTUAL_PATH %MY_RLOCATIONPATH%",
        
    attrs = {
        "src" : attr.label(allow_single_file=[".bat"], mandatory=True),
        "data": attr.label_list(allow_files=True),
   
    } | BAT_BINARY_IMPLICIT_ATTRS,
    executable = True
)