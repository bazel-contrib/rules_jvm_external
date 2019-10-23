// src/Main.java
package rules.jvm.external;

import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.InvalidProtocolBufferException;
import rules.jvm.external.AddressBookProtos.Person;

public class Main {
  public static void main(String[] args) throws InvalidProtocolBufferException {
    System.out.println(JsonFormat.printer().print(makePerson(123, "John Doe")));
  }

  public static Person makePerson(Integer id, String name) {
    Person.Builder person = Person.newBuilder();
    person.setId(123);
    person.setName("John Doe");
    return person.build();
  }
}
