import React, { useState, useEffect, useRef } from 'react';
import { Layout, Select, Button, Table, message, Tree, Input, Tooltip } from 'antd';
import Editor, { useMonaco } from '@monaco-editor/react';
import { PlayCircleOutlined, DownloadOutlined, TableOutlined, FieldStringOutlined, SearchOutlined } from '@ant-design/icons';
import { getConnections, executeSql, getTables, getColumns, getSchemas } from '../api';
import { MySQL } from 'dt-sql-parser';

const { Content, Sider } = Layout;

const SqlClient: React.FC = () => {
  const [connections, setConnections] = useState<any[]>([]);
  const [selectedConn, setSelectedConn] = useState<number | null>(null);
  const selectedConnRef = useRef<number | null>(null); // Ref to access latest state in callbacks
  
  const [schemas, setSchemas] = useState<string[]>([]);
  const [selectedSchema, setSelectedSchema] = useState<string | undefined>(undefined);
  const selectedSchemaRef = useRef<string | undefined>(undefined);

  // Use a ref to store the initial SQL, so we don't re-initialize Editor on re-renders
  // We don't need 'sql' state for driving the editor value anymore (uncontrolled component for performance)
  // const [sql, setSql] = useState('SELECT * FROM DUAL'); // REMOVED: Using uncontrolled editor
  const initialSql = useRef('SELECT * FROM DUAL');
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState<any[]>([]);
  
  const parserRef = useRef<MySQL>(new MySQL());

  // We still need to update the editor content when user clicks the tree
  const insertTextIntoEditor = (text: string) => {
      const editor = editorRef.current;
      if (editor) {
          const currentVal = editor.getValue();
          // Simple append for now
          editor.setValue(currentVal + ' ' + text);
          // Focus editor
          editor.focus();
      }
  };

  const [tables, setTables] = useState<any[]>([]); // Keep raw list for autocomplete and filtering
  const [searchText, setSearchText] = useState(''); // Search filter text
  
  const [columnCache, setColumnCache] = useState<Record<string, any[]>>({});
  const columnCacheRef = useRef<Record<string, any[]>>({}); // Ref for provider

  // Split View State
  const [splitPos, setSplitPos] = useState(50); // Percentage
  const [isDragging, setIsDragging] = useState(false);
  const splitContainerRef = useRef<HTMLDivElement>(null);
  const [tableHeight, setTableHeight] = useState(300);

  const monaco = useMonaco();
  const editorRef = useRef<any>(null);

  // Alias Map for auto-completion
  const aliasMapRef = useRef<Map<string, string>>(new Map());
  const tablesSetRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    selectedConnRef.current = selectedConn;
    columnCacheRef.current = columnCache;
    selectedSchemaRef.current = selectedSchema;
  }, [selectedConn, selectedSchema, columnCache]);

  useEffect(() => {
    // Update tables set whenever tables list changes
    const newSet = new Set<string>();
    tables.forEach(t => newSet.add(t.name.toUpperCase()));
    tablesSetRef.current = newSet;
  }, [tables]);



  useEffect(() => {
    getConnections().then(setConnections).catch(e => {
       console.error(e);
       message.error("Failed to load connections.");
    });
  }, []);

  useEffect(() => {
    if (selectedConn) {
      setTreeData([]); // Clear previous
      setColumnCache({});
      setSchemas([]);
      setSelectedSchema(undefined);
      setSearchText('');
      
      // Load Schemas first
      getSchemas(selectedConn).then(data => {
          setSchemas(data);
          loadTables(selectedConn, undefined);
      }).catch(() => {
          // If schema fetch fails (e.g. not supported), just load tables
          loadTables(selectedConn, undefined);
      });
    }
  }, [selectedConn]);
  
  const loadTables = (connId: number, schema?: string) => {
      setTreeData([]);
      getTables(connId, schema).then(data => {
        setTables(data);
        // Initial render with full list (or sliced)
        renderTree(data, '');
      });
  };

  // Filter and render tree
  const renderTree = (tableList: any[], filter: string) => {
      let filtered = tableList;
      if (filter) {
          const lower = filter.toLowerCase();
          filtered = tableList.filter((t: any) => t.name.toLowerCase().includes(lower));
      }
      
      // Limit to 100 for performance
      const limited = filtered.slice(0, 100).map((t: any) => ({
          title: t.name,
          key: t.name,
          icon: <TableOutlined />,
          isLeaf: false // Allow expand to load columns
      }));
      setTreeData(limited);
  };

  useEffect(() => {
      if (tables.length > 0) {
          renderTree(tables, searchText);
      }
  }, [searchText, tables]);

  const handleSchemaChange = (val: string) => {
      setSelectedSchema(val);
      setColumnCache({});
      columnCacheRef.current = {};
      if (selectedConn) {
          loadTables(selectedConn, val);
      }
  };

  const onLoadData = async ({ key, children }: any) => {
    if (children) return; // Already loaded

    try {
        const tableName = key as string;
        let cols = columnCache[tableName];
        
        if (!cols && selectedConn) {
            cols = await getColumns(selectedConn, tableName, selectedSchema);
            setColumnCache(prev => ({ ...prev, [tableName]: cols }));
        }

        setTreeData(origin =>
            updateTreeData(origin, key, cols.map((c: any) => ({
                title: (
                  <Tooltip title={c.comment || ''} placement="right">
                    <span>{`${c.name} (${c.type})`}</span>
                  </Tooltip>
                ),
                key: `${tableName}.${c.name}`,
                icon: <FieldStringOutlined />,
                isLeaf: true,
                comment: c.comment
            })))
        );
    } catch (e) {
        message.error("Failed to load columns");
    }
  };

  const updateTreeData = (list: any[], key: React.Key, children: any[]): any[] => {
    return list.map(node => {
      if (node.key === key) {
        return { ...node, children };
      }
      if (node.children) {
        return { ...node, children: updateTreeData(node.children, key, children) };
      }
      return node;
    });
  };

  const handleExecute = () => {
    if (!selectedConn) return message.error('Select a connection first');
    
    // Check for selected text
    const editor = editorRef.current;
    let sqlToExecute = '';
    
    if (editor) {
        const selection = editor.getSelection();
        if (selection && !selection.isEmpty()) {
            sqlToExecute = editor.getModel().getValueInRange(selection);
        } else {
            sqlToExecute = editor.getValue();
        }
    } else {
        // Fallback (shouldn't happen if editor mounted)
        sqlToExecute = initialSql.current;
    }
    
    if (!sqlToExecute || !sqlToExecute.trim()) return message.warning('SQL is empty');

    setLoading(true);
    executeSql(selectedConn, sqlToExecute)
      .then(res => {
        if (res.success) {
          setResult(res);
        } else {
          message.error(res.message);
        }
      })
      .finally(() => setLoading(false));
  };

  const stripIdentifier = (s: string) => s.replace(/^["`]+|["`]+$/g, '').trim();

  const splitQualifiedName = (name: string): { schema?: string; table: string } => {
      const raw = stripIdentifier(name);
      const idx = raw.lastIndexOf('.');
      if (idx <= 0 || idx === raw.length - 1) return { table: raw };
      return { schema: raw.slice(0, idx), table: raw.slice(idx + 1) };
  };

  const buildAliasMap = (text: string) => {
        const newAliasMap = new Map<string, string>();
        const regex = /(?:FROM|JOIN|,)\s+([a-zA-Z0-9_$#.`"]+)(?:\s+(?:AS\s+)?([a-zA-Z0-9_]+))?/gim;

        let match;
        while ((match = regex.exec(text)) !== null) {
            const tableName = stripIdentifier(match[1] || '');
            const alias = stripIdentifier(match[2] || '');

            const reserved = ['WHERE', 'GROUP', 'ORDER', 'HAVING', 'LIMIT', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER', 'ON', 'UNION', 'SELECT', 'SET', 'VALUES'];
            if (alias && !reserved.includes(alias.toUpperCase())) {
                newAliasMap.set(alias.toUpperCase(), tableName);
            }
        }
        return newAliasMap;
  };

  const parseAliases = (text: string) => {
        aliasMapRef.current = buildAliasMap(text);
  };

  const handleEditorDidMount = (editor: any, monaco: any) => {
    editorRef.current = editor;
    
    // Register completion provider using dt-sql-parser
    monaco.languages.registerCompletionItemProvider('sql', {
      triggerCharacters: ['.', ' '],
      provideCompletionItems: async (model: any, position: any) => {
        const connId = selectedConnRef.current;
        if (!connId) return { suggestions: [] };

        const text = model.getValue();
        const parser = parserRef.current;
        
        const lineContent = model.getLineContent(position.lineNumber);
        const textBefore = lineContent.substring(0, position.column - 1);
        
        let analysisText = text;

        // If user just typed a dot "t.", parser will fail. We need to analyze "t" instead.
        if (textBefore.trim().endsWith('.')) {
             // Remove the dot at the current cursor position from the full text
             const offset = model.getOffsetAt(position);
             if (text[offset - 1] === '.') {
                 analysisText = text.slice(0, offset - 1) + text.slice(offset);
             }
        }

        let suggestions: any = null;
        try {
            suggestions = parser.getSuggestionAtCaretPosition(analysisText, {
                lineNumber: position.lineNumber,
                column: position.column
            });
        } catch (e) {
            // Ignore parser errors on incomplete SQL
        }
        
        const monacoSuggestions: any[] = [];
        
        // Add Keywords from parser
        if (suggestions?.keywords) {
            suggestions.keywords.forEach((kw: string) => {
                monacoSuggestions.push({
                    label: kw,
                    kind: monaco.languages.CompletionItemKind.Keyword,
                    insertText: kw,
                    range: {
                        startLineNumber: position.lineNumber,
                        endLineNumber: position.lineNumber,
                        startColumn: position.column - (kw.length > 3 ? 3 : 1),
                        endColumn: position.column
                    }
                });
            });
        }

        // 2. Parse Entities to find Aliases
        let entities: any[] = [];
        try {
            // Use analysisText (without trailing dot) for entity extraction
            entities = (parser as any).getAllEntities(analysisText) || [];
        } catch (e) {
             // Fallback: use a simple regex if the heavy parser fails on invalid SQL
             const regex = /(?:FROM|JOIN|,)\s+([a-zA-Z0-9_$#.`"]+)(?:\s+(?:AS\s+)?([a-zA-Z0-9_]+))?/gim;
             let m;
             while ((m = regex.exec(analysisText)) !== null) {
                 const tableName = m[1].replace(/["`]/g, '');
                 const alias = m[2];
                 if (alias) {
                     entities.push({ tableName, alias });
                 }
             }
        }
        
        if (textBefore.trim().endsWith('.')) {
             const match = textBefore.match(/([a-zA-Z0-9_$#"]+)\.$/);
             if (match) {
                 const identifier = match[1].replace(/["`]/g, '');
                 
                 let targetTable = identifier;
                 let targetSchema = selectedSchemaRef.current;
                 
                 // Look for alias in parser results
                 // dt-sql-parser entities: [{ tableName: 'schema.table', alias: 't' }] or [{ tableName: 'table', alias: 't' }]
                 const entity = entities.find((e: any) => e.alias && e.alias.toUpperCase() === identifier.toUpperCase());
                 
                 if (entity) {
                     // Found alias! Use the real table name
                     const fullTableName = entity.tableName;
                     const parts = fullTableName.split('.');
                     if (parts.length > 1) {
                         targetSchema = parts[0];
                         targetTable = parts[1];
                     } else {
                         targetTable = fullTableName;
                     }
                 } else {
                     // If no alias found, maybe the identifier IS the table name (e.g. "SELECT * FROM table1.col")
                     // Or maybe regex missed it. Let's try one more fallback regex check on the full text just in case parser failed silently
                     const fallbackRegex = new RegExp(`(?:FROM|JOIN)\\s+([a-zA-Z0-9_$#.\`"]+)\\s+(?:AS\\s+)?${identifier}(?:\\s|$)`, 'i');
                     const m = analysisText.match(fallbackRegex);
                     if (m) {
                         const fullTableName = m[1].replace(/["`]/g, '');
                         const parts = fullTableName.split('.');
                         if (parts.length > 1) {
                             targetSchema = parts[0];
                             targetTable = parts[1];
                         } else {
                             targetTable = fullTableName;
                         }
                     }
                 }

                 if (targetTable) {
                    try {
                         const cacheKey = (targetSchema ? targetSchema + ":" : "") + targetTable;
                         let cols = columnCacheRef.current[cacheKey];
                         
                         if (!cols) {
                             cols = await getColumns(connId, targetTable, targetSchema);
                             columnCacheRef.current = { ...columnCacheRef.current, [cacheKey]: cols };
                         }
                         
                         if (cols) {
                             cols.forEach((c: any) => {
                                 monacoSuggestions.push({
                                     label: c.name,
                                     kind: monaco.languages.CompletionItemKind.Field,
                                     insertText: c.name,
                                     detail: c.type
                                 });
                             });
                         }
                    } catch (e) {}
                 }
             }
        } else {
            tablesRef.current.forEach((t: any) => {
                 monacoSuggestions.push({
                     label: t.name,
                     kind: monaco.languages.CompletionItemKind.Class,
                     insertText: t.name
                 });
            });
        }
        
        return { suggestions: monacoSuggestions };
      }
    });
  };

  const tablesRef = useRef<any[]>([]);
  useEffect(() => { tablesRef.current = tables; }, [tables]);

  const handleExport = () => {
    if (!result || !result.rows || result.rows.length === 0) return message.warning('No data to export');
    
    const header = result.columns.join(',');
    const rows = result.rows.map((row: any) => 
      result.columns.map((col: string) => {
        const val = row[col];
        return val === null || val === undefined ? '' : `"${String(val).replace(/"/g, '""')}"`;
      }).join(',')
    );
    
    const csvContent = [header, ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'export.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const columns = React.useMemo(() => result?.columns?.map((col: string) => ({
    title: col,
    dataIndex: [col],
    key: col,
    width: 150,
    ellipsis: true,
    sorter: (a: any, b: any) => {
        // Safe check for null/undefined
        const valA = a[col] ?? '';
        const valB = b[col] ?? '';
        return valA > valB ? 1 : -1;
    },
  })), [result]);

  // --- Resize Logic ---
  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging || !splitContainerRef.current) return;
      
      const containerRect = splitContainerRef.current.getBoundingClientRect();
      const relativeY = e.clientY - containerRect.top;
      const newPercentage = (relativeY / containerRect.height) * 100;
      
      const clamped = Math.min(Math.max(newPercentage, 10), 90);
      setSplitPos(clamped);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'row-resize';
    } else {
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    };
  }, [isDragging]);

  useEffect(() => {
    const updateHeight = () => {
        if (splitContainerRef.current) {
            const totalH = splitContainerRef.current.offsetHeight;
            const bottomH = totalH * ((100 - splitPos) / 100) - 5;
            setTableHeight(Math.max(bottomH - 50, 100));
        }
    };
    
    updateHeight();
    window.addEventListener('resize', updateHeight);
    return () => window.removeEventListener('resize', updateHeight);
  }, [splitPos]);

  return (
    <Layout style={{ height: '100vh' }}>
      <Sider width={300} theme="light" style={{ padding: 10, borderRight: '1px solid #eee', display: 'flex', flexDirection: 'column' }}>
        <Select 
          style={{ width: '100%', marginBottom: 10 }} 
          placeholder="Select Connection"
          onChange={setSelectedConn}
          options={connections.map(c => ({ label: c.name, value: c.id }))}
        />
        {schemas.length > 0 && (
            <Select 
              style={{ width: '100%', marginBottom: 10 }} 
              placeholder="Select Schema / User"
              value={selectedSchema}
              onChange={handleSchemaChange}
              showSearch
              options={schemas.map(s => ({ label: s, value: s }))}
            />
        )}
        <Input 
          placeholder="Search tables..." 
          prefix={<SearchOutlined />} 
          style={{ marginBottom: 10 }}
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
          allowClear
        />
        <div style={{ flex: 1, overflow: 'auto' }}>
           {treeData.length > 0 ? (
             <Tree
               treeData={treeData}
               loadData={onLoadData}
               showIcon
               onSelect={(keys, info) => {
                   if (info.node.isLeaf) {
                       const nodeTitle = (info.node as any).title;
                       let colName = '';
                       if (React.isValidElement(nodeTitle)) {
                           const children = (nodeTitle.props as any).children;
                           if (children && children.props && children.props.children) {
                               colName = children.props.children.split(' ')[0];
                           }
                       } else {
                           colName = (nodeTitle as string).split(' ')[0];
                       }
                       if (colName) insertTextIntoEditor(colName);
                   } else {
                       if (info.node.key) insertTextIntoEditor(info.node.key as string);
                   }
               }}
             />
           ) : (
             <div style={{ padding: 20, textAlign: 'center', color: '#999' }}>
               {selectedConn ? 'No tables found or loading...' : 'Please select a connection'}
             </div>
           )}
        </div>
      </Sider>
      
      <Content style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
        <div style={{ padding: 10, background: '#f5f5f5', borderBottom: '1px solid #ddd', display: 'flex', justifyContent: 'space-between', flexShrink: 0 }}>
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleExecute} loading={loading}>Execute</Button>
          <Button icon={<DownloadOutlined />} onClick={handleExport}>Export CSV</Button>
        </div>

        <div ref={splitContainerRef} style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, position: 'relative' }}>
            
            <div style={{ height: `${splitPos}%`, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                <Editor 
                  height="100%" 
                  defaultLanguage="sql" 
                  defaultValue={initialSql.current}
                  onMount={handleEditorDidMount}
                  options={{ minimap: { enabled: false }, automaticLayout: true }}
                />
            </div>

            <div 
                onMouseDown={handleMouseDown}
                style={{ 
                    height: '5px', 
                    background: '#f0f0f0', 
                    cursor: 'row-resize', 
                    borderTop: '1px solid #ddd', 
                    borderBottom: '1px solid #ddd',
                    zIndex: 10,
                    flexShrink: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                }}
            >
                <div style={{ width: 40, height: 3, background: '#ccc', borderRadius: 2 }}></div>
            </div>

            <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', background: '#fff' }}>
                {result && (
                    <Table 
                      dataSource={result.rows} 
                      columns={columns} 
                      size="small" 
                      rowKey={(record, index) => index || 0}
                      scroll={{ x: 'max-content', y: tableHeight }}
                      pagination={{ pageSize: 50, pageSizeOptions: [20, 50, 100, 200], size: 'small' }}
                    />
                )}
            </div>
        </div>
      </Content>
    </Layout>
  );
};

export default SqlClient;
