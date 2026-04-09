import { useState } from 'react'
import { Modal, Upload, Button, message } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import { useDocumentMutations } from '@/hooks/useDocuments'
import type { UploadFile } from 'antd/es/upload/interface'

const { Dragger } = Upload

interface Props {
  open: boolean
  onClose: () => void
}

const DocumentUpload = ({ open, onClose }: Props) => {
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const { upload, isUploading } = useDocumentMutations()

  const handleUpload = async () => {
    if (fileList.length === 0) {
      message.warning('请选择文件')
      return
    }
    for (const item of fileList) {
      if (item.originFileObj) {
        upload({ file: item.originFileObj })
      }
    }
    setFileList([])
    onClose()
  }

  return (
    <Modal
      title="上传文档"
      open={open}
      onCancel={() => { setFileList([]); onClose() }}
      footer={[
        <Button key="cancel" onClick={() => { setFileList([]); onClose() }}>取消</Button>,
        <Button key="upload" type="primary" loading={isUploading} onClick={handleUpload}>上传</Button>
      ]}
    >
      <Dragger
        beforeUpload={(file) => {
          setFileList((prev) => [...prev, file as UploadFile])
          return false
        }}
        fileList={fileList}
        onRemove={(file) => {
          setFileList((prev) => prev.filter((item) => item.uid !== file.uid))
        }}
      >
        <p className="ant-upload-drag-icon"><InboxOutlined /></p>
        <p className="ant-upload-text">点击或拖拽文件上传</p>
        <p className="ant-upload-hint">支持 PDF、TXT、Word、Markdown</p>
      </Dragger>
    </Modal>
  )
}

export default DocumentUpload
