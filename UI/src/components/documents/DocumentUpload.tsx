import { useState } from 'react'
import { Modal, Upload, Button, message } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import { useDocumentMutations } from '@/hooks/useDocuments'

const { Dragger } = Upload

interface Props {
  open: boolean
  onClose: () => void
}

const DocumentUpload = ({ open, onClose }: Props) => {
  const [fileList, setFileList] = useState<File[]>([])
  const { upload, isUploading } = useDocumentMutations()

  const handleUpload = async () => {
    if (fileList.length === 0) {
      message.warning('请选择文件')
      return
    }
    for (const file of fileList) {
      upload({ file })
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
          setFileList([...fileList, file])
          return false
        }}
        fileList={fileList as unknown as any[]}
        onRemove={(file) => {
          const index = fileList.indexOf(file as unknown as File)
          const newFileList = fileList.slice()
          newFileList.splice(index, 1)
          setFileList(newFileList)
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
