package com.ledger.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单的 ID 生成器
 */
public class IdGenerator {
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 100000);

    public static Long nextId() {
        return counter.incrementAndGet();
    }

    public static String nextRowId() {
        return "row_" + nextId();
    }

    public static String nextColId() {
        return "col_" + nextId();
    }
}
