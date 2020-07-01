package rules.jvm.external.jar;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.joining;

enum DuplicateEntryStrategy {

  LAST_IN_WINS("last-wins") {
    @Override
    public void resolve(Path current, InputStream inputStreamForDuplicate) throws IOException {
      try (OutputStream os = Files.newOutputStream(current, TRUNCATE_EXISTING)) {
        ByteStreams.copy(inputStreamForDuplicate, os);
      }
    }
  },
  FIRST_IN_WINS("first-wins") {
    @Override
    public void resolve(Path current, InputStream inputStreamForDuplicate) {
      // No need to do anything.
    }
  },
  IS_ERROR("are-errors") {
    @Override
    public void resolve(Path current, InputStream inputStreamForDuplicate) throws IOException {
      HashFunction hashFunction = Hashing.goodFastHash(64);

      HashCode first = hashFunction.hashBytes(Files.readAllBytes(current));
      HashCode second = hashFunction.hashBytes(ByteStreams.toByteArray(inputStreamForDuplicate));

      if (first.equals(second)) {
        return;
      }
      throw new IOException("Attempt to write different duplicate file for: " + current);
    }
  };

  private final String shortName;

  DuplicateEntryStrategy(String shortName) {
    this.shortName = shortName;
  }

  public static DuplicateEntryStrategy fromShortName(String name) {
    for (DuplicateEntryStrategy value : DuplicateEntryStrategy.values()) {
      if (value.shortName.equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Unable to find matching short name for %s. Valid options are: %s",
        name,
        Arrays.stream(values()).map(v -> v.shortName).collect(joining(", "))));
  }

  public String toString() {
    return shortName;
  }

  public abstract void resolve(Path current, InputStream inputStreamForDuplicate) throws IOException;
}
