<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>多人在线表格台账系统</h2>
        <p>协同编辑 · 部门隔离 · 格子级锁</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
      <div class="admin-tip">
        <el-alert type="info" :closable="false" show-icon>
          默认管理员账号：admin / admin123
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    const res = await login(form)
    const data = res.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('departmentId', data.departmentId || '')
    ElMessage.success('登录成功')
    if (data.role === 'ADMIN') {
      router.push('/admin')
    } else {
      router.push('/table')
    }
  } catch (e) {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 22px;
  color: #303133;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 13px;
  color: #909399;
}

.login-footer {
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}

.link:hover {
  text-decoration: underline;
}

.admin-tip {
  margin-top: 20px;
}
</style>
