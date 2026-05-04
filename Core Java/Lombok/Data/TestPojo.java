package Lombok.Data;

import lombok.Data;
import lombok.NonNull;

@Data
public class TestPojo {

    String name;
    final Integer age;
    @NonNull String address;
}
