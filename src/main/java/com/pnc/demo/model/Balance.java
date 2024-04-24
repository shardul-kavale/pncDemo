package com.pnc.demo.model;

import lombok.Data;

@Data
public class Balance {
    private String balanceId;
    private String accountId;
    private double balance;
}
