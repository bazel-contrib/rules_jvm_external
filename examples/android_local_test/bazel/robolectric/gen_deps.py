# Copyright 2019 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http:#www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys


def getKey(jar):
  key = "org.robolectric\:android-all\:"
  version = jar.split("/")[-2:-1].pop()
  key = key + version
  return key


def getValue(jar):
  return "../../../" + jar[jar.index("maven"):]


def main(argv):
  for jar in argv[1:]:
    key = getKey(jar)
    value = getValue(jar)
    print("%s=%s" % (key, value))


if __name__ == "__main__":
  main(sys.argv)
