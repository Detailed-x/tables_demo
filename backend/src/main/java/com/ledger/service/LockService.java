package com.ledger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ledger.model.LockInfo;
import com.ledger.util.JsonFileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 格子编辑锁服务
 * 核心规则：同一时间一个格子只允许一个用户编辑，其他人编辑时提示被占用
 */
@Service
public class LockService {

    private static final String FILE = "locks.json";

    @Value("${lock.timeout}")
    private long lockTimeout;

    /**
     * 生成锁的 key
     */
    private String buildKey(Long departmentId, String rowId, String colId) {
        return departmentId + ":" + rowId + ":" + colId;
    }

    /**
     * 获取所有锁（会自动清理超时锁）
     */
    private Map<String, LockInfo> getLocks() {
        Map<String, LockInfo> locks = JsonFileUtil.read(FILE,
                new TypeReference<Map<String, LockInfo>>() {}, new HashMap<>());
        // 清理超时锁
        boolean changed = false;
        Iterator<Map.Entry<String, LockInfo>> it = locks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LockInfo> entry = it.next();
            if (isExpired(entry.getValue())) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            JsonFileUtil.write(FILE, locks);
        }
        return locks;
    }

    /**
     * 判断锁是否超时
     */
    private boolean isExpired(LockInfo lock) {
        try {
            LocalDateTime lockedAt = LocalDateTime.parse(lock.getLockedAt(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            long seconds = ChronoUnit.SECONDS.between(lockedAt, LocalDateTime.now());
            return seconds * 1000 > lockTimeout;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 尝试获取格子锁
     * @return null 表示获取成功，否则返回当前持锁用户信息
     */
    public synchronized LockInfo tryLock(Long departmentId, String rowId, String colId,
                                          Long userId, String username) {
        String key = buildKey(departmentId, rowId, colId);
        Map<String, LockInfo> locks = getLocks();

        LockInfo existing = locks.get(key);
        if (existing != null && !existing.getUserId().equals(userId)) {
            // 被其他人占用
            return existing;
        }

        // 获取成功（自己已持有或无人持有）
        LockInfo lock = new LockInfo();
        lock.setUserId(userId);
        lock.setUsername(username);
        lock.setLockedAt(now());
        locks.put(key, lock);
        JsonFileUtil.write(FILE, locks);
        return null;
    }

    /**
     * 释放格子锁
     */
    public synchronized void unlock(Long departmentId, String rowId, String colId, Long userId) {
        String key = buildKey(departmentId, rowId, colId);
        Map<String, LockInfo> locks = getLocks();
        LockInfo existing = locks.get(key);
        if (existing != null && existing.getUserId().equals(userId)) {
            locks.remove(key);
            JsonFileUtil.write(FILE, locks);
        }
    }

    /**
     * 查询格子锁状态
     */
    public LockInfo getLockStatus(Long departmentId, String rowId, String colId) {
        String key = buildKey(departmentId, rowId, colId);
        Map<String, LockInfo> locks = getLocks();
        return locks.get(key);
    }

    /**
     * 获取某部门所有被锁定的格子
     */
    public Map<String, LockInfo> getLocksByDepartment(Long departmentId) {
        Map<String, LockInfo> all = getLocks();
        Map<String, LockInfo> result = new HashMap<>();
        String prefix = departmentId + ":";
        for (Map.Entry<String, LockInfo> entry : all.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 续租锁（用户正在编辑时定时调用，防止超时）
     */
    public synchronized boolean renewLock(Long departmentId, String rowId, String colId, Long userId) {
        String key = buildKey(departmentId, rowId, colId);
        Map<String, LockInfo> locks = getLocks();
        LockInfo existing = locks.get(key);
        if (existing != null && existing.getUserId().equals(userId)) {
            existing.setLockedAt(now());
            JsonFileUtil.write(FILE, locks);
            return true;
        }
        return false;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
