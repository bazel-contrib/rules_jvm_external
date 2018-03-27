def gmaven_artifact(fqn):
  parts = fqn.split(":")
  packaging = "aar"

  if len(parts) == 3:
    group_id, artifact_id, version = parts
  elif len(parts) == 4:
    group_id, artifact_id, version, packaging = parts
  elif len(parts) == 5:
    _, _, _, _, classifier = parts
    fail("Classifiers are currently not supported. Please remove it from the coordinate: %s" % classifier)
  else:
    fail("Invalid qualified name for artifact: %s" % fqn)

  return "@%s_%s_%s//%s" % (
      escape(group_id),
      escape(artifact_id),
      escape(version),
      packaging
      )

def escape(string):
  return string.replace(".", "_")
