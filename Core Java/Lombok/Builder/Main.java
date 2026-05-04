package Lombok.Builder;

public class Main {

    public static void main(String[] args) {
        TestPojo object = TestPojo.builder().age(20).name("xyz").build();
    }
}
