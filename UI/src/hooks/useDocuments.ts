import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { documentsApi } from '@/api/documents'
import type { DocumentQueryParams } from '@/types/document'

export const useDocuments = (params: DocumentQueryParams) => {
  return useQuery({
    queryKey: ['documents', params],
    queryFn: () => documentsApi.getDocuments(params)
  })
}

export const useDocumentMutations = () => {
  const queryClient = useQueryClient()

  const uploadMutation = useMutation({
    mutationFn: ({ file, title }: { file: File; title?: string }) => 
      documentsApi.uploadDocument(file, title),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      message.success('上传成功')
    },
    onError: () => message.error('上传失败')
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      message.success('删除成功')
    },
    onError: () => message.error('删除失败')
  })

  return {
    upload: uploadMutation.mutate,
    delete: deleteMutation.mutate,
    isUploading: uploadMutation.isPending,
    isDeleting: deleteMutation.isPending
  }
}
