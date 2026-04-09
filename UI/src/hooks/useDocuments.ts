// 文档相关 Hook
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
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
    },
    onError: () => {}
  })

  const uploadWithProgress = async (file: File, onProgress?: (progress: number) => void): Promise<void> => {
    return new Promise((resolve, reject) => {
      // 模拟进度更新（实际应用中需要使用 XMLHttpRequest 来获取真实进度）
      let progress = 0
      const interval = setInterval(() => {
        progress += Math.random() * 20
        if (progress >= 100) {
          progress = 100
          clearInterval(interval)
          onProgress?.(100)
          
          // 执行实际上传
          documentsApi.uploadDocument(file)
            .then(() => {
              queryClient.invalidateQueries({ queryKey: ['documents'] })
              resolve()
            })
            .catch((error) => {
              reject(error)
            })
        } else {
          onProgress?.(Math.round(progress))
        }
      }, 200)
    })
  }

  const batchUploadMutation = useMutation({
    mutationFn: (files: File[]) => documentsApi.batchUploadDocuments(files),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: () => {}
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: () => {}
  })

  const batchDeleteMutation = useMutation({
    mutationFn: (ids: number[]) => documentsApi.batchDeleteDocuments(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: () => {}
  })

  return {
    upload: uploadMutation.mutate,
    uploadAsync: uploadMutation.mutateAsync,
    batchUpload: batchUploadMutation.mutate,
    delete: deleteMutation.mutate,
    batchDelete: batchDeleteMutation.mutate,
    uploadWithProgress,
    isUploading: uploadMutation.isPending,
    isBatchUploading: batchUploadMutation.isPending,
    isDeleting: deleteMutation.isPending,
    isBatchDeleting: batchDeleteMutation.isPending
  }
}

// 获取单个文档详情
export const useDocument = (id: number | null) => {
  return useQuery({
    queryKey: ['document', id],
    queryFn: () => documentsApi.getDocument(id!),
    enabled: !!id
  })
}

// 获取文档分块列表
export const useDocumentChunks = (documentId: number | null) => {
  return useQuery({
    queryKey: ['document-chunks', documentId],
    queryFn: () => documentsApi.getDocumentChunks(documentId!),
    enabled: !!documentId
  })
}
