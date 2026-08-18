import request from '@/utils/request'

// 认证相关
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// 用户相关
export const getAllUsers = () => request.get('/users')
export const getCurrentUser = () => request.get('/users/me')
export const assignDepartment = (userId, departmentId) =>
  request.put(`/users/${userId}/department`, { departmentId })
export const getUsersByDepartment = (departmentId) =>
  request.get(`/users/department/${departmentId}`)

// 部门相关
export const getAllDepartments = () => request.get('/departments')
export const createDepartment = (name) => request.post('/departments', { name })
export const updateDepartment = (id, name) => request.put(`/departments/${id}`, { name })
export const deleteDepartment = (id) => request.delete(`/departments/${id}`)

// 表格相关
export const getMyTable = () => request.get('/table')
export const getTableByDepartment = (departmentId) =>
  request.get(`/table/department/${departmentId}`)
export const updateCell = (data) => request.put('/table/cell', data)

// 格子锁相关
export const tryLock = (rowId, colId) =>
  request.post('/table/cell/lock', { rowId, colId })
export const unlock = (rowId, colId) =>
  request.post('/table/cell/unlock', { rowId, colId })
export const renewLock = (rowId, colId) =>
  request.post('/table/cell/renew', { rowId, colId })
export const getLocks = () => request.get('/table/locks')

// 行列管理
export const addRow = () => request.post('/table/row')
export const deleteRow = (rowId) => request.delete(`/table/row/${rowId}`)
export const addColumn = (name) => request.post('/table/column', { name })
export const deleteColumn = (colId) => request.delete(`/table/column/${colId}`)
export const renameColumn = (colId, name) =>
  request.put(`/table/column/${colId}`, { name })
