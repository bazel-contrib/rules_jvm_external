// Copyright 2026 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.bazelbuild.rules_jvm_external.jar;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import kotlin.Metadata;
import kotlin.metadata.Attributes;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.KmTypeAlias;
import kotlin.metadata.Visibility;
import kotlin.metadata.jvm.KotlinClassMetadata;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Extracts the simple names of top-level Kotlin declarations (functions, properties, and type
 * aliases) from a compiled class.
 *
 * <p>Kotlin compiles file-level declarations into a synthetic "file facade" class (named {@code
 * <FileName>Kt} by default). The declarations themselves are not visible as classes, so they are
 * invisible to a JAR index that only records class file names. We read the {@code kotlin.Metadata}
 * annotation that the compiler stamps onto the facade and recover the declared names so callers can
 * attribute imports such as {@code com.example.someTopLevelFunction} to the providing artifact.
 */
class KotlinTopLevel {

  private static final int ASM_API = Opcodes.ASM9;

  private KotlinTopLevel() {}

  /**
   * Returns the simple names of top-level declarations if {@code classBytes} is a Kotlin file
   * facade or multi-file class part, otherwise an empty set. Never throws: any class that cannot be
   * parsed (for example one compiled for a newer bytecode version than the bundled ASM understands)
   * yields an empty set.
   */
  static SortedSet<String> topLevelDeclarationNames(byte[] classBytes) {
    SortedSet<String> names = new TreeSet<>();
    try {
      Metadata metadata = readMetadata(classBytes);
      if (metadata == null) {
        return names;
      }

      // readLenient tolerates metadata produced by a Kotlin compiler newer than this library.
      KotlinClassMetadata parsed = KotlinClassMetadata.readLenient(metadata);

      KmPackage kmPackage = null;
      if (parsed instanceof KotlinClassMetadata.FileFacade) {
        kmPackage = ((KotlinClassMetadata.FileFacade) parsed).getKmPackage();
      } else if (parsed instanceof KotlinClassMetadata.MultiFileClassPart) {
        kmPackage = ((KotlinClassMetadata.MultiFileClassPart) parsed).getKmPackage();
      }
      if (kmPackage == null) {
        return names;
      }

      // Only public declarations can be imported from another artifact; internal and private ones
      // would just add unreferenceable noise to the index. We emit the Kotlin source names (what an
      // import statement uses), which can differ from the JVM method names when @JvmName is
      // applied.
      for (KmFunction function : kmPackage.getFunctions()) {
        if (Attributes.getVisibility(function) == Visibility.PUBLIC) {
          names.add(function.getName());
        }
      }
      for (KmProperty property : kmPackage.getProperties()) {
        if (Attributes.getVisibility(property) == Visibility.PUBLIC) {
          names.add(property.getName());
        }
      }
      for (KmTypeAlias typeAlias : kmPackage.getTypeAliases()) {
        if (Attributes.getVisibility(typeAlias) == Visibility.PUBLIC) {
          names.add(typeAlias.getName());
        }
      }
    } catch (RuntimeException e) {
      // Unsupported class file version, malformed metadata, etc. Skip rather than fail the index.
      return new TreeSet<>();
    }
    return names;
  }

  private static Metadata readMetadata(byte[] classBytes) {
    MetadataReader reader = new MetadataReader();
    new ClassReader(classBytes)
        .accept(reader, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return reader.toMetadata();
  }

  /**
   * Collects the fields of a {@code kotlin.Metadata} annotation as ASM visits them.
   *
   * <p>The terse member names (k, mv, bv, d1, d2, xs, pn, xi) below are the annotation's
   * {@code @JvmName} accessors, derived from
   * https://github.com/JetBrains/kotlin/blob/v2.2.0/libraries/stdlib/jvm/runtime/kotlin/Metadata.kt
   */
  private static final class MetadataReader extends ClassVisitor {
    private boolean isKotlin = false;
    private int kind = 1;
    private int extraInt = 0;
    private String extraString = "";
    private String packageName = "";
    // ASM delivers primitive arrays (the int[] mv and bv) directly to visit(); only the String[]
    // values d1 and d2 arrive element-by-element through visitArray().
    private int[] metadataVersion = new int[0];
    private int[] bytecodeVersion = new int[0];
    private final List<String> data1 = new ArrayList<>();
    private final List<String> data2 = new ArrayList<>();

    private MetadataReader() {
      super(ASM_API);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (!"Lkotlin/Metadata;".equals(descriptor)) {
        return null;
      }
      isKotlin = true;
      return new AnnotationVisitor(ASM_API) {
        @Override
        public void visit(String name, Object value) {
          switch (name) {
            case "k":
              kind = (Integer) value;
              break;
            case "xi":
              extraInt = (Integer) value;
              break;
            case "xs":
              extraString = (String) value;
              break;
            case "pn":
              packageName = (String) value;
              break;
            case "mv":
              metadataVersion = (int[]) value;
              break;
            case "bv":
              bytecodeVersion = (int[]) value;
              break;
            default:
              break;
          }
        }

        @Override
        public AnnotationVisitor visitArray(String arrayName) {
          return new AnnotationVisitor(ASM_API) {
            @Override
            public void visit(String elementName, Object value) {
              switch (arrayName) {
                case "d1":
                  data1.add((String) value);
                  break;
                case "d2":
                  data2.add((String) value);
                  break;
                default:
                  break;
              }
            }
          };
        }
      };
    }

    // kotlin-metadata-jvm reads header data from a kotlin.Metadata instance, so we synthesize one
    // from the values ASM read off the class. The parser only calls the accessors below, so the
    // equals/hashCode that the annotation contract would otherwise require are unnecessary here.
    @SuppressWarnings("BadAnnotationImplementation")
    private Metadata toMetadata() {
      if (!isKotlin) {
        return null;
      }
      int[] mv = metadataVersion;
      int[] bv = bytecodeVersion;
      String[] d1 = data1.toArray(new String[0]);
      String[] d2 = data2.toArray(new String[0]);
      return new Metadata() {
        @Override
        public Class<? extends Annotation> annotationType() {
          return Metadata.class;
        }

        @Override
        public int k() {
          return kind;
        }

        @Override
        public int[] mv() {
          return mv;
        }

        @Override
        public int[] bv() {
          return bv;
        }

        @Override
        public String[] d1() {
          return d1;
        }

        @Override
        public String[] d2() {
          return d2;
        }

        @Override
        public String xs() {
          return extraString;
        }

        @Override
        public String pn() {
          return packageName;
        }

        @Override
        public int xi() {
          return extraInt;
        }
      };
    }
  }
}
