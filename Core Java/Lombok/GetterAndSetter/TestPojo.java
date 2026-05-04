package Lombok.GetterAndSetter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TestPojo {

    String name;

    @Setter(AccessLevel.PRIVATE)
    boolean isCommitteeMember;
}
