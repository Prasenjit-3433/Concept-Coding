package Lombok.ToString;

import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
public class TestPojo {

    String name;

    @ToString.Include
    boolean isCommitteeMember;
}
