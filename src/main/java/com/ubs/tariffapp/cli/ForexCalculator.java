package com.ubs.tariffapp.cli;

import java.util.Scanner;

import com.ubs.tariffapp.services.ForexService;

public class ForexCalculator {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            System.out.print("Enter amount: ");
            double amount = sc.nextDouble();
            System.out.print("From currency (e.g. USD): ");
            String base = sc.next().toUpperCase();
            System.out.print("To currency (e.g. SGD): ");
            String target = sc.next().toUpperCase();

            double result = ForexService.convert(base, target, amount);
            System.out.printf("%.2f %s = %.2f %s%n", amount, base, result, target);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
