package com.ledger.util;
import java.io.File;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JSON 文件读写工具
 * 所有数据以 JSON 文件形式存储，不使用数据库
 */
public class JsonFileUtil {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static String dataPath;

    public static void setDataPath(String path) {
        dataPath = path;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static String getDataPath() {
        return dataPath;
    }

    /**
     * 读取 JSON 文件并反序列化为对象（普通实体 Class 方式）
     */
    public static <T> T read(String fileName, Class<T> clazz, T defaultValue) {
        try {
            Path path = Paths.get(dataPath, fileName);
            if (!Files.exists(path)) {
                return defaultValue;
            }
            String content = new String(Files.readAllBytes(path), "UTF-8");
            return mapper.readValue(content, clazz);
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 读取 JSON 文件，支持泛型集合 List / Map（TypeReference 方式）
     * @param fileName 文件名称
     * @param typeReference new TypeReference<List<User>>(){} / new TypeReference<Map<Long,TableData>>(){}
     * @param defaultValue 文件不存在/异常返回的默认值
     * @return 解析后的泛型对象
     */
    public static <T> T read(String fileName, TypeReference<T> typeReference, T defaultValue) {
        try {
            Path path = Paths.get(dataPath, fileName);
            if (!Files.exists(path)) {
                return defaultValue;
            }
            String content = new String(Files.readAllBytes(path), "UTF-8");
            return mapper.readValue(content, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 将对象序列化为 JSON 并写入文件
     */
    public static synchronized void write(String fileName, Object data) {
        try {
            Path path = Paths.get(dataPath, fileName);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            String json = mapper.writeValueAsString(data);
            Files.write(path, json.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断文件是否存在
     */
    public static boolean exists(String fileName) {
        return Files.exists(Paths.get(dataPath, fileName));
    }
}