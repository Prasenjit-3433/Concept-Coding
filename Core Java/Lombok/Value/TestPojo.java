package Lombok.Value;

import lombok.NonNull;
import lombok.Value;

@Value
public class TestPojo {

    String name;
    final Integer age;
    @NonNull String address;
}
