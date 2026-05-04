package Lombok.NonNull;

import lombok.NonNull;

public class NonNullTest {

    public static void demoMethod(@NonNull String name) {
        System.out.println(name);
    }

    public static void main(String[] args) {
        demoMethod("hello");
    }
}
