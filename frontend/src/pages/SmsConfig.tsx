import React, { useState, useEffect } from 'react';
import { Layout, Form, Input, Button, message, Card } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Content } = Layout;

const SmsConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadConfig();
  }, []);

  const loadConfig = () => {
    setLoading(true);
    axios.get('/accounting-tool/api/config/sms')
      .then(res => {
        form.setFieldsValue(res.data);
      })
      .catch(() => message.error('Failed to load configuration'))
      .finally(() => setLoading(false));
  };

  const onFinish = (values: any) => {
    setLoading(true);
    axios.post('/accounting-tool/api/config/sms', values)
      .then(() => message.success('Configuration saved'))
      .catch(() => message.error('Failed to save configuration'))
      .finally(() => setLoading(false));
  };

  return (
    <Content style={{ padding: '24px' }}>
      <Card title="SMS Alert Configuration" style={{ maxWidth: 600, margin: '0 auto' }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
        >
          <Form.Item
            name="sms.url"
            label="SMS API URL"
            rules={[{ required: true, message: 'Please enter SMS API URL' }]}
            extra="The URL where POST requests will be sent."
          >
            <Input placeholder="https://api.sms-provider.com/send" />
          </Form.Item>

          <Form.Item
            name="sms.key.phone"
            label="Phone Number Field Name"
            rules={[{ required: true, message: 'Please enter field name' }]}
            extra="The JSON key for the phone number (e.g., 'mobile', 'to', 'phone')."
          >
            <Input placeholder="mobile" />
          </Form.Item>

          <Form.Item
            name="sms.key.content"
            label="Message Content Field Name"
            rules={[{ required: true, message: 'Please enter field name' }]}
            extra="The JSON key for the message content (e.g., 'content', 'message', 'text')."
          >
            <Input placeholder="content" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={loading}>
              Save Configuration
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </Content>
  );
};

export default SmsConfig;
