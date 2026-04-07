export interface Document {
  id: string
  title: string
  content: string
  file_type: string
  file_size: number
  status: 'pending' | 'processing' | 'completed' | 'failed'
  chunk_count: number
  created_at: string
  updated_at: string
  user_id: string
}

export interface DocumentQueryParams {
  page?: number
  page_size?: number
  status?: string
  search?: string
}

export interface DocumentChunk {
  id: string
  content: string
  chunk_index: number
  token_count: number
}
