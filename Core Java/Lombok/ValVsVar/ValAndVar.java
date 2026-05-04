package Lombok.ValVsVar;

import lombok.val;

public class ValAndVar {

    public static void main(String[] args) {

        // val, var -> used for local variables (not for fields or params)

        // val makes the local variable immutable (i.e. final)
        val a = 10;
//        a = 30; // error

        // var DOESN'T make local variable final
        var b = 10;
        b = 30;
    }
}
