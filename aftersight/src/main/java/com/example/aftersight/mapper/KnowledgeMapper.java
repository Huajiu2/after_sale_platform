package com.example.aftersight.mapper;

import com.example.aftersight.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface KnowledgeMapper {

    @Insert("INSERT INTO knowledge_doc (doc_code, doc_name, category, file_type, file_size, chunk_count, " +
            "vectorize_status, uploaded_by, uploaded_at) " +
            "VALUES (#{docCode}, #{docName}, #{category}, #{fileType}, #{fileSize}, #{chunkCount}, " +
            "#{vectorizeStatus}, #{uploadedBy}, #{uploadedAt})")
    void insert(KnowledgeDoc doc);

    @Select("SELECT CONCAT('DOC', LPAD(IFNULL(MAX(CAST(SUBSTRING(doc_code, 4) AS UNSIGNED)), 0) + 1, 3, '0')) FROM knowledge_doc")
    String nextDocCode();

    List<KnowledgeDoc> selectList(@Param("docName") String docName);
}
