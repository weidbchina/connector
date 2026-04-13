import React from 'react';
import { Layout, Menu } from 'antd';
import { DatabaseOutlined, DesktopOutlined, ConsoleSqlOutlined, SettingOutlined } from '@ant-design/icons';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import Connections from './pages/Connections';
import Tasks from './pages/Tasks';
import SqlClient from './pages/SqlClient';
import SmsConfig from './pages/SmsConfig';

const { Header, Content, Footer, Sider } = Layout;

const App: React.FC = () => {
  return (
    <Router basename="/accounting-tool">
      <Layout style={{ minHeight: '100vh' }}>
        <Sider collapsible>
          <div className="demo-logo-vertical" style={{ height: 32, margin: 16, background: 'rgba(255, 255, 255, 0.2)' }} />
          <Menu theme="dark" defaultSelectedKeys={['1']} mode="inline">
            <Menu.Item key="1" icon={<DesktopOutlined />}>
              <Link to="/">Dashboard & Tasks</Link>
            </Menu.Item>
            <Menu.Item key="2" icon={<DatabaseOutlined />}>
              <Link to="/connections">Connections</Link>
            </Menu.Item>
            <Menu.Item key="3" icon={<ConsoleSqlOutlined />}>
              <Link to="/sql-client">SQL Client</Link>
            </Menu.Item>
            <Menu.Item key="4" icon={<SettingOutlined />}>
              <Link to="/sms-config">SMS Config</Link>
            </Menu.Item>
          </Menu>
        </Sider>
        <Layout>
          <Header style={{ padding: 0, background: '#fff' }} />
          <Content style={{ margin: '0 16px' }}>
            <div style={{ padding: 24, minHeight: 360, background: '#fff', marginTop: 16 }}>
              <Routes>
                <Route path="/" element={<Tasks />} />
                <Route path="/connections" element={<Connections />} />
                <Route path="/sql-client" element={<SqlClient />} />
                <Route path="/sms-config" element={<SmsConfig />} />
              </Routes>
            </div>
          </Content>
          <Footer style={{ textAlign: 'center' }}>DB Monitor Tool ©2026</Footer>
        </Layout>
      </Layout>
    </Router>
  );
};

export default App;
