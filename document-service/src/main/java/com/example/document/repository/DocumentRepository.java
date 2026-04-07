package com.example.document.repository;

import com.example.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档Repository
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 根据文件名校验和查询
     */
    Optional<Document> findByFileNameAndChecksum(String fileName, String checksum);

    /**
     * 根据状态查询
     */
    Page<Document> findByStatus(Document.DocumentStatus status, Pageable pageable);

    /**
     * 根据文件类型查询
     */
    Page<Document> findByFileType(String fileType, Pageable pageable);

    /**
     * 查询未向量化的文档
     */
    @Query("SELECT d FROM Document d WHERE d.status IN ('PARSED', 'UPLOADED') AND d.status != 'EMBEDDED'")
    List<Document> findDocumentsNotEmbedded();

    /**
     * 统计各状态文档数量
     */
    @Query("SELECT d.status, COUNT(d) FROM Document d GROUP BY d.status")
    List<Object[]> countByStatus();
}
