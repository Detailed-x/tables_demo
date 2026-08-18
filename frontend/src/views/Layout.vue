<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="24"><Document /></el-icon>
        <span>在线台账系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/table">
          <el-icon><Grid /></el-icon>
          <span>在线台账</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag v-if="departmentName" type="success" size="small" effect="light">
            {{ departmentName }}
          </el-tag>
          <el-tag v-else type="warning" size="small" effect="light">
            未分配部门
          </el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="28" style="background:#409eff">
                {{ username.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ username }}</span>
              <el-tag :type="isAdmin ? 'danger' : 'info'" size="small" effect="dark">
                {{ isAdmin ? '管理员' : '普通用户' }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAllDepartments } from '@/api'

const route = useRoute()
const router = useRouter()

const username = ref(localStorage.getItem('username') || '')
const role = ref(localStorage.getItem('role') || '')
const departmentId = ref(localStorage.getItem('departmentId') || '')
const departmentName = ref('')
const departments = ref([])

const isAdmin = computed(() => role.value === 'ADMIN')
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '')

const loadDepartments = async () => {
  try {
    const res = await getAllDepartments()
    departments.value = res.data
    if (departmentId.value) {
      const dept = res.data.find(d => String(d.id) === String(departmentId.value))
      if (dept) departmentName.value = dept.name
    }
  } catch (e) {}
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.clear()
    router.push('/login')
  }
}

onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #1f2d3d;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #2d3e50;
}

.el-menu {
  border-right: none;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #606266;
}

.main {
  background: #f5f7fa;
  padding: 20px;
  overflow: auto;
}
</style>
