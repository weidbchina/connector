import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import 'antd/dist/reset.css';
import { loader } from '@monaco-editor/react';

// Configure Monaco Editor to use local resources
loader.config({ paths: { vs: '/accounting-tool/monaco/vs' } });

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
