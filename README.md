# 多人在线表格台账系统

前后端分离的多人协同在线表格台账系统，支持部门隔离、格子级编辑锁、JSON 文件存储（无需数据库）。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | SpringBoot 2.7 + JWT + Jackson |
| 前端 | Vue 3 + Vite + Element Plus + Vue Router + Axios |
| 存储 | JSON 文件（`backend/data/` 目录） |

## 功能特性

- **账号注册**：用户可自行注册账号，注册后默认无部门，需管理员分配
- **管理员账号**：系统启动时自动创建默认管理员（admin / admin123）
- **部门管理**：管理员可创建、编辑、删除部门
- **用户分配**：管理员可为用户分配/变更所属部门
- **部门隔离**：同一部门成员共享同一份在线表格，不同部门数据完全隔离
- **格子级编辑锁**：同一时间一个格子只允许一个用户编辑，其他人点击时提示"正在被 XX 编辑"
- **锁自动续租**：编辑中每 30 秒自动续租，防止超时释放
- **锁超时释放**：5 分钟未操作自动释放锁，避免死锁
- **实时锁状态**：前端每 5 秒轮询，被他人锁定的格子高亮显示并带锁图标
- **行列管理**：支持新增/删除行、新增/删除/重命名列

## 项目结构

```
doubao_demo/
├── backend/                          # SpringBoot 后端
│   ├── src/main/java/com/ledger/
│   │   ├── LedgerApplication.java    # 启动类
│   │   ├── config/                   # 配置（CORS、拦截器、初始化）
│   │   ├── controller/               # 控制器（Auth/User/Department/Table）
│   │   ├── service/                  # 业务逻辑（User/Department/Table/Lock）
│   │   ├── model/                    # 数据模型
│   │   └── util/                     # 工具类（JSON读写/JWT/ID生成）
│   ├── src/main/resources/
│   │   └── application.properties    # 配置文件
│   ├── data/                         # JSON 数据存储目录（运行时生成）
│   └── pom.xml
└── frontend/                         # Vue 3 前端
    ├── src/
    │   ├── views/                    # 页面（Login/Register/Layout/Admin/Table）
    │   ├── router/                   # 路由配置
    │   ├── api/                      # API 接口封装
    │   ├── utils/                    # 工具（Axios 封装）
    │   ├── App.vue
    │   └── main.js
    ├── index.html
    ├── vite.config.js
    └── package.json
```

## 快速启动

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问：http://localhost:8080/api

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:3000

### 3. 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

## 使用流程

1. 使用管理员账号登录，进入「管理后台」
2. 创建部门（如"技术部"、"市场部"）
3. 在用户管理中，为已注册的用户分配部门
4. 普通用户登录后进入「在线台账」，即可看到本部门的共享表格
5. 点击任意格子开始编辑：
   - 如果格子空闲，获取编辑权后可输入内容，回车或失焦自动保存
   - 如果格子正被他人编辑，会弹出提示"该格子正在被 XX 编辑"
   - 被他人锁定的格子会高亮为橙色并显示锁图标

## API 接口一览

### 认证
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 用户
- `GET /api/users` - 获取所有用户（管理员）
- `GET /api/users/me` - 获取当前用户信息
- `PUT /api/users/{userId}/department` - 分配部门（管理员）
- `GET /api/users/department/{departmentId}` - 获取部门用户列表

### 部门
- `GET /api/departments` - 获取所有部门
- `POST /api/departments` - 创建部门（管理员）
- `PUT /api/departments/{id}` - 更新部门（管理员）
- `DELETE /api/departments/{id}` - 删除部门（管理员）

### 表格
- `GET /api/table` - 获取当前用户部门的表格
- `GET /api/table/department/{departmentId}` - 获取指定部门表格
- `PUT /api/table/cell` - 更新格子内容
- `POST /api/table/row` - 新增行
- `DELETE /api/table/row/{rowId}` - 删除行
- `POST /api/table/column` - 新增列
- `DELETE /api/table/column/{colId}` - 删除列
- `PUT /api/table/column/{colId}` - 重命名列

### 格子锁
- `POST /api/table/cell/lock` - 尝试获取编辑锁
- `POST /api/table/cell/unlock` - 释放编辑锁
- `POST /api/table/cell/renew` - 续租锁
- `GET /api/table/locks` - 获取当前部门所有锁状态

## 数据存储说明

所有数据以 JSON 文件形式存储在 `backend/data/` 目录下：

| 文件 | 内容 |
|------|------|
| `users.json` | 用户列表（含密码，生产环境建议加密） |
| `departments.json` | 部门列表 |
| `tables.json` | 各部门的表格数据 |
| `locks.json` | 当前活跃的格子锁 |

删除对应文件即可重置数据。

## 配置说明

`backend/src/main/resources/application.properties`：

```properties
server.port=8080              # 后端端口
ledger.data.path=data         # JSON 数据存储目录
jwt.secret=...                # JWT 密钥（生产环境请修改）
jwt.expiration=86400000       # Token 有效期（毫秒），默认24小时
admin.username=admin          # 默认管理员用户名
admin.password=admin123       # 默认管理员密码
lock.timeout=300000           # 格子锁超时时间（毫秒），默认5分钟
```

## 注意事项

- 本系统为演示/小型团队使用设计，JSON 文件存储不适合高并发大规模场景
- 密码明文存储，生产环境请改为 BCrypt 等加密方式
- JWT 密钥请在生产环境修改
- 格子锁采用轮询机制（前端每5秒查询），非实时推送；如需实时可升级为 WebSocket
