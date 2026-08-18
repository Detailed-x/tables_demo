package com.ledger.model;

import lombok.Data;

/**
 * 格子锁信息
 */
@Data
public class LockInfo {
    private Long userId;
    private String username;
    private String lockedAt;
}
