package com.bankdata.account.support;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;

@ApplicationScoped
public class AccountNumberGenerator {
    private final SecureRandom random = new SecureRandom();

    //important: SecureRandom does not guarantee uniqueness. On a DB layer constraints we have unique
    //keys, but in AccountService we have to make retries if we hit unique violations!
    public String next() {
        long value = Math.abs(random.nextLong()) % 1_000_000_0000L;
        return String.format("%010d", value); //making 10 symb string starting with zeroes
    }
}
