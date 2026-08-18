<template>
  <div class="admin-container">
    <el-row :gutter="20">
      <!-- 部门管理 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><OfficeBuilding /></el-icon> 部门管理</span>
              <el-button type="primary" size="small" @click="showDeptDialog()">
                <el-icon><Plus /></el-icon> 新建部门
              </el-button>
            </div>
          </template>
          <el-table :data="departments" stripe style="width:100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="部门名称" />
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="showDeptDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该部门吗？" @confirm="handleDeleteDept(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 用户管理 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><User /></el-icon> 用户管理（分配部门）</span>
              <el-button size="small" @click="loadUsers">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <el-table :data="users" stripe style="width:100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" width="130" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
                  {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="所属部门">
              <template #default="{ row }">
                <el-select
                  v-model="row.departmentId"
                  placeholder="未分配"
                  size="small"
                  clearable
                  style="width:160px"
                  @change="(val) => handleAssignDept(row, val)"
                >
                  <el-option
                    v-for="dept in departments"
                    :key="dept.id"
                    :label="dept.name"
                    :value="dept.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 部门编辑弹窗 -->
    <el-dialog v-model="deptDialogVisible" :title="editingDept ? '编辑部门' : '新建部门'" width="400px">
      <el-form :model="deptForm" label-width="80px">
        <el-form-item label="部门名称">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDept">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAllDepartments, createDepartment, updateDepartment, deleteDepartment,
  getAllUsers, assignDepartment
} from '@/api'

const departments = ref([])
const users = ref([])
const deptDialogVisible = ref(false)
const editingDept = ref(null)
const deptForm = reactive({ name: '' })

const loadDepartments = async () => {
  const res = await getAllDepartments()
  departments.value = res.data
}

const loadUsers = async () => {
  const res = await getAllUsers()
  users.value = res.data
}

const showDeptDialog = (row) => {
  if (row) {
    editingDept.value = row
    deptForm.name = row.name
  } else {
    editingDept.value = null
    deptForm.name = ''
  }
  deptDialogVisible.value = true
}

const handleSaveDept = async () => {
  if (!deptForm.name.trim()) {
    ElMessage.warning('请输入部门名称')
    return
  }
  if (editingDept.value) {
    await updateDepartment(editingDept.value.id, deptForm.name)
    ElMessage.success('部门更新成功')
  } else {
    await createDepartment(deptForm.name)
    ElMessage.success('部门创建成功')
  }
  deptDialogVisible.value = false
  loadDepartments()
}

const handleDeleteDept = async (id) => {
  await deleteDepartment(id)
  ElMessage.success('部门删除成功')
  loadDepartments()
  loadUsers()
}

const handleAssignDept = async (row, departmentId) => {
  try {
    await assignDepartment(row.id, departmentId)
    ElMessage.success(`已将「${row.username}」分配到对应部门`)
  } catch (e) {
    // 回滚
    loadUsers()
  }
}

onMounted(() => {
  loadDepartments()
  loadUsers()
})
</script>

<style scoped>
.admin-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}
</style>
