import axios from 'axios';

const api = axios.create({
  baseURL: '/accounting-tool/api',
});

export const getConnections = () => api.get('/connections').then(res => res.data);
export const saveConnection = (conn: any) => conn.id ? api.put(`/connections/${conn.id}`, conn) : api.post('/connections', conn);
export const deleteConnection = (id: number) => api.delete(`/connections/${id}`);
export const testConnection = (conn: any) => api.post('/connections/test', conn).then(res => res.data);

export const getTasks = () => api.get('/tasks').then(res => res.data);
export const saveTask = (task: any) => task.id ? api.put(`/tasks/${task.id}`, task) : api.post('/tasks', task);
export const deleteTask = (id: number) => api.delete(`/tasks/${id}`);
export const getTaskLogs = (id: number) => api.get(`/tasks/${id}/logs`).then(res => res.data);

export const executeSql = (connId: number, sql: string, maxRows: number = 50) => 
  api.post('/sql-client/execute', { connectionId: connId, sql, maxRows }).then(res => res.data);

export const getSchemas = (connId: number) => api.get(`/sql-client/metadata/schemas?connectionId=${connId}`).then(res => res.data);
export const getTables = (connId: number, schema?: string) => {
    let url = `/sql-client/metadata/tables?connectionId=${connId}`;
    if (schema) url += `&schema=${schema}`;
    return api.get(url).then(res => res.data);
};
export const getColumns = (connId: number, tableName: string, schema?: string) => {
  let url = `/sql-client/metadata/columns?connectionId=${connId}&tableName=${encodeURIComponent(tableName)}`;
  if (schema) url += `&schema=${encodeURIComponent(schema)}`;
  return api.get(url).then(res => res.data);
};

export default api;
