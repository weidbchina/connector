import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, InputNumber, Switch, Space, message, Card, Tabs, List } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, HistoryOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { getTasks, saveTask, deleteTask, getConnections, getTaskLogs } from '../api';

const Tasks: React.FC = () => {
  const [tasks, setTasks] = useState([]);
  const [connections, setConnections] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<any>(null);
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [logs, setLogs] = useState([]);
  const [form] = Form.useForm();

  const loadData = () => {
    getTasks().then(setTasks).catch(console.error);
    getConnections().then(setConnections).catch(e => {
       console.error(e);
       message.error("Failed to connect to server.");
    });
  };

  useEffect(() => { loadData(); }, []);

  const handleSave = async (values: any) => {
    // Map connectionId to object
    const task = { 
      ...editingTask, 
      ...values, 
      dbConnection: { id: values.connectionId } 
    };
    try {
      await saveTask(task);
      message.success('Saved');
      setIsModalOpen(false);
      loadData();
    } catch (e) {
      message.error('Failed');
    }
  };

  const handleShowLogs = async (taskId: number) => {
    const data = await getTaskLogs(taskId);
    setLogs(data.content); // Page content
    setIsLogModalOpen(true);
  };

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Cron', dataIndex: 'cronExpression', key: 'cron' },
    { title: 'Active', dataIndex: 'active', render: (v: boolean) => <Switch checked={v} disabled /> },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button icon={<HistoryOutlined />} onClick={() => handleShowLogs(record.id)} />
          <Button icon={<EditOutlined />} onClick={() => { 
            setEditingTask(record); 
            form.setFieldsValue({ ...record, connectionId: record.dbConnection.id }); 
            setIsModalOpen(true); 
          }} />
          <Button icon={<DeleteOutlined />} danger onClick={() => deleteTask(record.id).then(loadData)} />
        </Space>
      ),
    },
  ];

  return (
    <Card title="Monitoring Tasks" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingTask(null); form.resetFields(); setIsModalOpen(true); }}>New Task</Button>}>
      <Table dataSource={tasks} columns={columns} rowKey="id" />
      
      <Modal title="Task Configuration" open={isModalOpen} onCancel={() => setIsModalOpen(false)} onOk={() => form.submit()} width={800}>
        <Form form={form} onFinish={handleSave} layout="vertical">
          <Form.Item name="name" label="Task Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="connectionId" label="Connection" rules={[{ required: true }]}>
            <Select options={connections.map(c => ({ label: c.name, value: c.id }))} />
          </Form.Item>
          <Form.Item name="monitorSql" label="Monitor SQL (SELECT only)" rules={[{ required: true }]}>
            <Input.TextArea autoSize={{ minRows: 24 }} style={{ minHeight: '50vh', overflow: 'hidden', resize: 'none' }} placeholder="Enter your SQL query here..." />
          </Form.Item>
          <Space>
             <Form.Item name="cronExpression" label="Cron Expression" rules={[{ required: true }]} initialValue="0 */5 * * * ?">
               <Input />
             </Form.Item>
             <Form.Item name="timeoutSeconds" label="Timeout (s)" initialValue={30}>
               <InputNumber />
             </Form.Item>
             <Form.Item name="active" label="Active" valuePropName="checked" initialValue={true}>
               <Switch />
             </Form.Item>
          </Space>
          
          <Form.Item label="Validation Rules">
            <Form.List name="validationRules">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...restField }) => (
                    <Card size="small" key={key} style={{ marginBottom: 16 }}>
                        <Space style={{ display: 'flex', width: '100%' }} align="baseline">
                          <Form.Item
                            {...restField}
                            name={[name, 'ruleType']}
                            label="Type"
                            rules={[{ required: true, message: 'Missing rule type' }]}
                            style={{ minWidth: 200 }}
                          >
                            <Select placeholder="Select Rule Type">
                               <Select.Option value="THRESHOLD_COUNT">Numeric Threshold (Count/Value)</Select.Option>
                               <Select.Option value="RESULT_EMPTY">Result Is Empty</Select.Option>
                               <Select.Option value="RESULT_NOT_EMPTY">Result Not Empty</Select.Option>
                               <Select.Option value="REGEX_MATCH">Regex Match (Any Column)</Select.Option>
                               <Select.Option value="CONTAINS_TEXT">Contains Text</Select.Option>
                            </Select>
                          </Form.Item>
                          
                          <Form.Item
                            noStyle
                            shouldUpdate={(prevValues, currentValues) => {
                                const prevRules = prevValues.validationRules || [];
                                const currRules = currentValues.validationRules || [];
                                return prevRules[name]?.ruleType !== currRules[name]?.ruleType;
                            }}
                          >
                            {({ getFieldValue }) => {
                                const rules = getFieldValue('validationRules') || [];
                                const ruleType = rules[name]?.ruleType;
                                
                                if (ruleType === 'RESULT_EMPTY' || ruleType === 'RESULT_NOT_EMPTY') {
                                    return null;
                                }

                                let placeholder = "Value";
                                if (ruleType === 'THRESHOLD_COUNT') placeholder = "> 10, < 5, = 0";
                                else if (ruleType === 'REGEX_MATCH') placeholder = "Regex Pattern (e.g. ^Error.*)";
                                else if (ruleType === 'CONTAINS_TEXT') placeholder = "Text to find";

                                return (
                                  <Form.Item
                                    {...restField}
                                    name={[name, 'expectedValue']}
                                    label="Expected Condition"
                                    rules={[{ required: true, message: 'Missing value' }]}
                                    style={{ flex: 1 }}
                                  >
                                    <Input placeholder={placeholder} />
                                  </Form.Item>
                                );
                            }}
                          </Form.Item>
                          
                          <MinusCircleOutlined onClick={() => remove(name)} style={{ color: 'red' }} />
                        </Space>
                        
                        <div style={{ color: '#888', fontSize: '12px', marginTop: '-10px', marginBottom: '10px' }}>
                           <Form.Item shouldUpdate noStyle>
                             {({ getFieldValue }) => {
                                 const rules = getFieldValue('validationRules') || [];
                                 const ruleType = rules[name]?.ruleType;
                                 if (ruleType === 'THRESHOLD_COUNT') return "Checks the first column of the first row against the condition.";
                                 if (ruleType === 'REGEX_MATCH') return "Scans all columns in all rows for the pattern.";
                                 if (ruleType === 'RESULT_EMPTY') return "Alerts if result contains rows.";
                                 if (ruleType === 'RESULT_NOT_EMPTY') return "Alerts if result is empty.";
                                 return "";
                             }}
                           </Form.Item>
                        </div>
                    </Card>
                  ))}
                  <Form.Item>
                    <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                      Add Validation Rule
                    </Button>
                  </Form.Item>
                </>
              )}
            </Form.List>
          </Form.Item>

          <Card size="small" title="Alert Configuration">
             <Form.Item name="alertPhoneNumbers" label="Phone Numbers (comma separated)">
               <Input />
             </Form.Item>
             <Form.Item name="alertTemplate" label="Message Template">
               <Input.TextArea placeholder="Alert for ${taskName}: ${error}" />
             </Form.Item>
             <Form.Item name="alertIntervalMinutes" label="Min Interval (Minutes)" initialValue={10}>
               <InputNumber />
             </Form.Item>
          </Card>
        </Form>
      </Modal>

      <Modal title="Execution Logs" open={isLogModalOpen} onCancel={() => setIsLogModalOpen(false)} footer={null} width={800}>
        <Table dataSource={logs} rowKey="id" columns={[
           { title: 'Time', dataIndex: 'executionTime' },
           { title: 'Status', dataIndex: 'status', render: (s: string) => <span style={{ color: s === 'SUCCESS' ? 'green' : 'red' }}>{s}</span> },
           { title: 'Duration (ms)', dataIndex: 'executionDurationMs' },
           { title: 'Result', dataIndex: 'resultSummary', ellipsis: true }
        ]} />
      </Modal>
    </Card>
  );
};

export default Tasks;
