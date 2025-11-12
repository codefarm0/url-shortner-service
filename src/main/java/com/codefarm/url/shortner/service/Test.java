package com.codefarm.url.shortner.service;

public class Test {

    interface CardPayment {
        default void process() {
            System.out.println("Processing via CardPayment system");
        }
    }

    interface WalletPayment {
        default void process() {
            System.out.println("Processing via WalletPayment system");
        }
    }

    static class PaymentGateway implements CardPayment, WalletPayment {
        // must resolve ambiguity
        public void process() {
//            CardPayment.super.process(); // choose one or combine both
            System.out.println("Merged Payment logic in PaymentGateway");
        }
    }

    public static void main(String[] args) {
        PaymentGateway pg = new PaymentGateway();
        pg.process();

        boolean flag= false;

        if (pg != null) {
            
        }
    }
}
