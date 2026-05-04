package Lombok.EqualsAndHashCode;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TestPojo {

    String name;

    @EqualsAndHashCode.Exclude
    boolean isCommitteeMember; // won't be used

    static int maxTerm = 10; // being static, won't be used
}
