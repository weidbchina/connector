import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, message, Space, Card, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ApiOutlined } from '@ant-design/icons';
import { getConnections, saveConnection, deleteConnection, testConnection } from '../api';

const Connections: React.FC = () => {
  const [connections, setConnections] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingConn, setEditingConn] = useState<any>(null);
  const [dbType, setDbType] = useState('ORACLE');
  const [form] = Form.useForm();

  const loadData = () => getConnections().then(setConnections).catch(e => {
    console.error(e);
    message.error("Failed to load connections. Is the backend running?");
  });

  useEffect(() => { loadData(); }, []);

  const handleDbTypeChange = (val: string) => {
    setDbType(val);
    const currentUrl = form.getFieldValue('jdbcUrl');
    // Only update URL if it's default or empty
    if (!currentUrl || currentUrl.includes('jdbc:oracle') || currentUrl.includes('jdbc:mysql')) {
       if (val === 'MYSQL') {
         form.setFieldsValue({ jdbcUrl: 'jdbc:mysql://localhost:3306/db_name' });
       } else {
         form.setFieldsValue({ jdbcUrl: 'jdbc:oracle:thin:@//localhost:1521/ORCL' });
       }
    }
  };

  const handleSave = async (values: any) => {
    try {
      await saveConnection({ ...editingConn, ...values });
      message.success('Saved successfully');
      setIsModalOpen(false);
      loadData();
    } catch (e) {
      message.error('Save failed');
    }
  };

  const handleDelete = async (id: number) => {
    await deleteConnection(id);
    message.success('Deleted');
    loadData();
  };

  const handleTest = async () => {
    try {
      const values = await form.validateFields();
      const res = await testConnection({ ...editingConn, ...values });
      if (res.success) message.success('Connection Successful');
      else message.error('Connection Failed: ' + res.message);
    } catch (e) {
      // Form validation error
    }
  };

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'JDBC URL', dataIndex: 'jdbcUrl', key: 'jdbcUrl' },
    { title: 'Username', dataIndex: 'username', key: 'username' },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => { 
            setEditingConn(record); 
            form.setFieldsValue(record); 
            setDbType(record.jdbcUrl.startsWith('jdbc:mysql') ? 'MYSQL' : 'ORACLE');
            setIsModalOpen(true); 
          }} />
          <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ];

  return (
    <Card title="Database Connections" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { 
        setEditingConn(null); 
        form.resetFields(); 
        setDbType('ORACLE');
        setIsModalOpen(true); 
    }}>New Connection</Button>}>
      <Table dataSource={connections} columns={columns} rowKey="id" />
      <Modal 
        title={editingConn ? "Edit Connection" : "New Connection"} 
        open={isModalOpen} 
        onCancel={() => setIsModalOpen(false)}
        footer={[
          <Button key="test" icon={<ApiOutlined />} onClick={handleTest}>Test Connection</Button>,
          <Button key="cancel" onClick={() => setIsModalOpen(false)}>Cancel</Button>,
          <Button key="submit" type="primary" onClick={() => form.submit()}>Save</Button>,
        ]}
      >
        <Form form={form} onFinish={handleSave} layout="vertical">
          <Form.Item label="Database Type">
             <Select value={dbType} onChange={handleDbTypeChange}>
               <Select.Option value="ORACLE">Oracle 19c</Select.Option>
               <Select.Option value="MYSQL">MySQL</Select.Option>
             </Select>
          </Form.Item>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="jdbcUrl" label="JDBC URL" rules={[{ required: true }]} initialValue="jdbc:oracle:thin:@//localhost:1521/ORCL">
            <Input />
          </Form.Item>
          <Form.Item name="username" label="Username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true }]}>
            <Input.Password />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default Connections;
