<template>
  <div class="table-container">
    <!-- 工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" size="small" @click="handleAddRow">
            <el-icon><Plus /></el-icon> 新增行
          </el-button>
          <el-button type="success" size="small" @click="showAddColDialog">
            <el-icon><Plus /></el-icon> 新增列
          </el-button>
          <el-button size="small" @click="loadTable">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-tag v-if="tableData" type="info" size="small">
            最后更新：{{ tableData.updatedAt }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 未分配部门提示 -->
    <el-card v-if="!hasDepartment" class="empty-card">
      <el-empty description="您尚未分配部门，请联系管理员分配后使用台账功能">
        <el-button type="primary" @click="$router.push('/admin')" v-if="isAdmin">前往管理后台</el-button>
      </el-empty>
    </el-card>

    <!-- 表格区域 -->
    <el-card v-else class="table-card" shadow="never">
      <div class="table-wrapper" v-if="tableData">
        <table class="ledger-table">
          <thead>
            <tr>
              <th class="row-header-col">#</th>
              <th
                v-for="col in tableData.columns"
                :key="col.id"
                :style="{ width: col.width + 'px', minWidth: col.width + 'px' }"
              >
                <div class="col-header">
                  <span class="col-name">{{ col.name }}</span>
                  <el-dropdown trigger="click" @command="(cmd) => handleColCommand(cmd, col)">
                    <el-icon class="col-more"><MoreFilled /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="rename">重命名</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除列</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </th>
              <th class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in tableData.rows" :key="row.id">
              <td class="row-index">{{ rowIndex + 1 }}</td>
              <td
                v-for="col in tableData.columns"
                :key="col.id"
                :class="getCellClass(row.id, col.id)"
                @click="handleCellClick(row, col)"
              >
                <!-- 编辑模式 -->
                <div v-if="isEditing(row.id, col.id)" class="cell-edit">
                  <el-input
                    v-model="editValue"
                    size="small"
                    ref="editInputRef"
                    @keyup.enter="handleSaveCell"
                    @keyup.esc="handleCancelEdit"
                    @blur="handleSaveCell"
                  />
                </div>
                <!-- 展示模式 -->
                <div v-else class="cell-display">
                  <span class="cell-value">{{ getCellValue(row, col) || '—' }}</span>
                  <el-tooltip
                    v-if="getLockInfo(row.id, col.id)"
                    :content="`正在被 ${getLockInfo(row.id, col.id).username} 编辑`"
                    placement="top"
                  >
                    <el-icon class="lock-icon"><Lock /></el-icon>
                  </el-tooltip>
                </div>
              </td>
              <td class="action-cell">
                <el-popconfirm title="确定删除该行吗？" @confirm="handleDeleteRow(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" link>
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </template>
                </el-popconfirm>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 空表格提示 -->
        <el-empty v-if="tableData.rows.length === 0" description="暂无数据，点击上方「新增行」开始" />
      </div>
    </el-card>

    <!-- 新增列弹窗 -->
    <el-dialog v-model="addColVisible" title="新增列" width="360px">
      <el-form label-width="80px">
        <el-form-item label="列名称">
          <el-input v-model="newColName" placeholder="请输入列名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addColVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddColumn">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名列弹窗 -->
    <el-dialog v-model="renameColVisible" title="重命名列" width="360px">
      <el-form label-width="80px">
        <el-form-item label="列名称">
          <el-input v-model="renameColName" placeholder="请输入列名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameColVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenameColumn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMyTable, updateCell, tryLock, unlock, renewLock, getLocks,
  addRow, deleteRow, addColumn, deleteColumn, renameColumn
} from '@/api'

const tableData = ref(null)
const locks = ref({})  // key: "deptId:rowId:colId", value: {userId, username, lockedAt}

// 编辑状态
const editingCell = reactive({ rowId: null, colId: null })
const editValue = ref('')
const editInputRef = ref(null)

// 弹窗
const addColVisible = ref(false)
const newColName = ref('')
const renameColVisible = ref(false)
const renameColName = ref('')
const renamingColId = ref(null)

// 轮询定时器
let lockPollTimer = null
let renewTimer = null

const username = computed(() => localStorage.getItem('username') || '')
const userId = computed(() => localStorage.getItem('userId') || '')
const role = computed(() => localStorage.getItem('role') || '')
const isAdmin = computed(() => role.value === 'ADMIN')
const hasDepartment = computed(() => !!localStorage.getItem('departmentId'))

// 获取格子值
const getCellValue = (row, col) => {
  const cell = row.cells?.[col.id]
  return cell?.value || ''
}

// 获取格子锁信息
const getLockInfo = (rowId, colId) => {
  const deptId = localStorage.getItem('departmentId')
  const key = `${deptId}:${rowId}:${colId}`
  const lock = locks.value[key]
  // 不显示自己的锁
  if (lock && String(lock.userId) === String(userId.value)) return null
  return lock
}

// 判断是否正在编辑
const isEditing = (rowId, colId) => {
  return editingCell.rowId === rowId && editingCell.colId === colId
}

// 获取格子样式类
const getCellClass = (rowId, colId) => {
  const classes = ['cell']
  if (isEditing(rowId, colId)) classes.push('editing')
  const lock = getLockInfo(rowId, colId)
  if (lock) classes.push('locked-by-other')
  return classes.join(' ')
}

// 加载表格
const loadTable = async () => {
  try {
    const res = await getMyTable()
    tableData.value = res.data
  } catch (e) {
    if (e.response?.status === 400) {
      // 未分配部门
    }
  }
}

// 加载锁状态
const loadLocks = async () => {
  try {
    const res = await getLocks()
    locks.value = res.data || {}
  } catch (e) {}
}

// 点击格子
const handleCellClick = async (row, col) => {
  // 如果正在编辑其他格子，先保存
  if (editingCell.rowId && (editingCell.rowId !== row.id || editingCell.colId !== col.id)) {
    await handleSaveCell()
  }

  // 如果已经在编辑这个格子，不处理
  if (isEditing(row.id, col.id)) return

  // 尝试获取锁
  try {
    const res = await tryLock(row.id, col.id)
    if (res.data.locked) {
      // 获取锁成功，进入编辑模式
      editingCell.rowId = row.id
      editingCell.colId = col.id
      editValue.value = getCellValue(row, col)
      await nextTick()
      if (editInputRef.value) {
        editInputRef.value.focus()
        editInputRef.value.select()
      }
      // 启动续租定时器（每30秒续租一次）
      startRenewTimer()
    } else {
      // 获取锁失败，提示
      ElMessage.warning(res.data.message || '该格子正在被他人编辑')
    }
  } catch (e) {}
}

// 保存格子
const handleSaveCell = async () => {
  if (!editingCell.rowId) return
  const rowId = editingCell.rowId
  const colId = editingCell.colId
  const value = editValue.value

  try {
    await updateCell({ rowId, colId, value })
    // 更新本地数据
    const row = tableData.value.rows.find(r => r.id === rowId)
    if (row) {
      if (!row.cells) row.cells = {}
      if (!row.cells[colId]) row.cells[colId] = {}
      row.cells[colId].value = value
      row.cells[colId].editor = username.value
    }
  } catch (e) {}

  // 释放锁
  try {
    await unlock(rowId, colId)
  } catch (e) {}

  // 清除编辑状态
  editingCell.rowId = null
  editingCell.colId = null
  editValue.value = ''
  stopRenewTimer()
  loadLocks()
}

// 取消编辑
const handleCancelEdit = () => {
  if (!editingCell.rowId) return
  const rowId = editingCell.rowId
  const colId = editingCell.colId
  unlock(rowId, colId).catch(() => {})
  editingCell.rowId = null
  editingCell.colId = null
  editValue.value = ''
  stopRenewTimer()
}

// 续租定时器
const startRenewTimer = () => {
  stopRenewTimer()
  renewTimer = setInterval(async () => {
    if (editingCell.rowId) {
      try {
        await renewLock(editingCell.rowId, editingCell.colId)
      } catch (e) {}
    }
  }, 30000)
}

const stopRenewTimer = () => {
  if (renewTimer) {
    clearInterval(renewTimer)
    renewTimer = null
  }
}

// 新增行
const handleAddRow = async () => {
  const res = await addRow()
  tableData.value = res.data
  ElMessage.success('已新增一行')
}

// 删除行
const handleDeleteRow = async (rowId) => {
  const res = await deleteRow(rowId)
  tableData.value = res.data
  ElMessage.success('已删除该行')
}

// 新增列
const showAddColDialog = () => {
  newColName.value = ''
  addColVisible.value = true
}

const handleAddColumn = async () => {
  if (!newColName.value.trim()) {
    ElMessage.warning('请输入列名称')
    return
  }
  const res = await addColumn(newColName.value)
  tableData.value = res.data
  addColVisible.value = false
  ElMessage.success('已新增列')
}

// 列操作
const handleColCommand = (cmd, col) => {
  if (cmd === 'rename') {
    renamingColId.value = col.id
    renameColName.value = col.name
    renameColVisible.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确定删除列「${col.name}」吗？该列所有数据将被清除`, '提示', {
      type: 'warning'
    }).then(async () => {
      const res = await deleteColumn(col.id)
      tableData.value = res.data
      ElMessage.success('已删除列')
    }).catch(() => {})
  }
}

const handleRenameColumn = async () => {
  if (!renameColName.value.trim()) {
    ElMessage.warning('请输入列名称')
    return
  }
  const res = await renameColumn(renamingColId.value, renameColName.value)
  tableData.value = res.data
  renameColVisible.value = false
  ElMessage.success('列已重命名')
}

onMounted(() => {
  if (hasDepartment.value) {
    loadTable()
    loadLocks()
    // 每5秒轮询一次锁状态
    lockPollTimer = setInterval(loadLocks, 5000)
  }
})

onBeforeUnmount(() => {
  if (lockPollTimer) clearInterval(lockPollTimer)
  stopRenewTimer()
  // 离开页面时释放正在编辑的锁
  if (editingCell.rowId) {
    unlock(editingCell.rowId, editingCell.colId).catch(() => {})
  }
})
</script>

<style scoped>
.table-container {
  padding: 0;
}

.toolbar-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 8px;
}

.table-card {
  min-height: 400px;
}

.table-wrapper {
  overflow-x: auto;
}

.ledger-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.ledger-table th,
.ledger-table td {
  border: 1px solid #ebeef5;
  padding: 0;
  text-align: left;
}

.ledger-table th {
  background: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

.row-header-col {
  width: 50px;
  min-width: 50px;
  text-align: center !important;
  background: #f5f7fa !important;
}

.action-col {
  width: 70px;
  min-width: 70px;
  text-align: center !important;
  background: #f5f7fa !important;
}

.col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
}

.col-more {
  cursor: pointer;
  color: #909399;
  font-size: 16px;
}

.col-more:hover {
  color: #409eff;
}

.row-index {
  text-align: center;
  background: #fafafa;
  color: #909399;
  font-size: 13px;
  padding: 10px 8px !important;
}

.cell {
  padding: 0;
  cursor: pointer;
  transition: background 0.2s;
  min-height: 40px;
}

.cell:hover {
  background: #ecf5ff;
}

.cell.editing {
  background: #fff;
  padding: 4px;
}

.cell.locked-by-other {
  background: #fdf6ec;
  cursor: not-allowed;
}

.cell.locked-by-other:hover {
  background: #faecd8;
}

.cell-display {
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 40px;
}

.cell-value {
  flex: 1;
  color: #303133;
  word-break: break-all;
}

.cell-value:empty::before {
  content: '点击编辑';
  color: #c0c4cc;
  font-size: 13px;
}

.lock-icon {
  color: #e6a23c;
  font-size: 14px;
  flex-shrink: 0;
}

.cell-edit {
  padding: 4px;
}

.action-cell {
  text-align: center;
  padding: 8px !important;
}

.empty-card {
  margin-top: 16px;
}
</style>
