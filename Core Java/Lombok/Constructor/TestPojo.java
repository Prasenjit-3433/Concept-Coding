package Lombok.Constructor;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class TestPojo {

    String name;
    boolean isCommitteeMember;
    @NonNull Integer age;
}
