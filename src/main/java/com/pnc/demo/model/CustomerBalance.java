package com.pnc.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerBalance {
    private String accountId;
    private String customerId;
    private String phone;
    private double balance;
}
